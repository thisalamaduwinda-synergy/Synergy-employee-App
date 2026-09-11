package lk.synergypharma.employee.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.FragmentHomeBinding;
import lk.synergypharma.employee.databinding.IncludeQuickActionBinding;
import lk.synergypharma.employee.domain.model.Announcement;
import lk.synergypharma.employee.domain.model.AppNotification;
import lk.synergypharma.employee.domain.model.AttendanceDay;
import lk.synergypharma.employee.domain.model.Employee;
import lk.synergypharma.employee.domain.model.GateEvent;
import lk.synergypharma.employee.domain.model.MonthSummary;
import lk.synergypharma.employee.domain.usecase.ValidateAbsenceReason;
import lk.synergypharma.employee.ui.common.BaseFragment;
import lk.synergypharma.employee.util.Constants;
import lk.synergypharma.employee.util.DateTimeUtils;
import lk.synergypharma.employee.util.Result;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * Screen 02 — what the gate has on you today, and anything it is missing.
 *
 * <p>There is no "Check in" button and there must never be one. The gate already
 * identified the employee by face; a button here would be a second source of
 * truth, and the first time the two disagree the attendance record stops being
 * evidence of anything.
 */
public final class HomeFragment extends BaseFragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private AnnouncementAdapter adapter;
    private boolean firstResume = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        ViewUtils.applySystemBarPadding(binding.header, true, false);

        adapter = new AnnouncementAdapter();
        binding.listAnnouncements.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listAnnouncements.setAdapter(adapter);

        setUpQuickActions();
        binding.buttonNotifications.setOnClickListener(
                v -> nav().navigate(R.id.action_global_notifications));
        binding.refresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.refresh.setColorSchemeResources(R.color.synergy_blue_700);

        viewModel.employee().observe(getViewLifecycleOwner(), this::renderEmployee);
        viewModel.month().observe(getViewLifecycleOwner(), this::renderMonth);
        viewModel.announcements().observe(getViewLifecycleOwner(), result -> {
            if (result == null || !result.isSuccess() || result.data == null) {
                return;
            }
            List<Announcement> feed = result.data;
            adapter.submit(feed.size() > 3 ? feed.subList(0, 3) : feed);
            ViewUtils.visible(binding.textAnnouncementsEmpty, feed.isEmpty());
        });
        viewModel.notifications().observe(getViewLifecycleOwner(), result -> {
            boolean anyUnread = false;
            if (result != null && result.isSuccess() && result.data != null) {
                for (AppNotification n : result.data) {
                    if (!n.read) {
                        anyUnread = true;
                        break;
                    }
                }
            }
            ViewUtils.visible(binding.badgeUnread, anyUnread);
        });
    }

    /**
     * How often the gate card re-reads the server while Home is on screen. The
     * gate writes a scan the moment a face is accepted; this is what makes the
     * card follow it without the employee pulling to refresh. One small request
     * per employee per interval - negligible for the server.
     */
    static final long POLL_MS = 45_000L;

    private final Handler poller = new Handler(Looper.getMainLooper());
    private final Runnable pollTick = new Runnable() {
        @Override
        public void run() {
            if (binding == null || viewModel == null) {
                return;
            }
            viewModel.pollAttendance();
            poller.postDelayed(this, POLL_MS);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        // Coming back from the absence form must clear the alert immediately.
        if (!firstResume) {
            viewModel.refresh();
        }
        firstResume = false;
        poller.removeCallbacks(pollTick);
        poller.postDelayed(pollTick, POLL_MS);
    }

    @Override
    public void onPause() {
        // No polling in the background: it would drain the battery for a card
        // nobody is looking at, and onResume restarts it anyway.
        poller.removeCallbacks(pollTick);
        super.onPause();
    }

    // --------------------------------------------------------------- render

    private void renderEmployee(@Nullable Employee employee) {
        binding.textGreeting.setText(greetingFor(LocalTime.now()));
        binding.textName.setText(employee == null ? "" : employee.shortName());
    }

    @StringRes
    private static int greetingFor(@NonNull LocalTime now) {
        if (now.isBefore(LocalTime.NOON)) {
            return R.string.home_greeting_morning;
        }
        if (now.isBefore(LocalTime.of(17, 0))) {
            return R.string.home_greeting_afternoon;
        }
        return R.string.home_greeting_evening;
    }

    private void renderMonth(@Nullable Result<MonthSummary> result) {
        if (result == null) {
            return;
        }
        boolean silent = viewModel.isSilentRefresh();
        if (result.isLoading()) {
            // A background poll keeps the last good card; only a deliberate
            // pull-to-refresh shows the spinner.
            if (!silent) {
                binding.refresh.setRefreshing(true);
            }
            return;
        }
        binding.refresh.setRefreshing(false);
        if (result.isError()) {
            if (!silent) {
                toast(result.message == null ? getString(R.string.error_generic) : result.message);
            }
            return;
        }
        if (!result.isSuccess() || result.data == null) {
            return;
        }

        MonthSummary month = result.data;
        LocalDate today = LocalDate.now();
        binding.textToday.setText(getString(R.string.home_today,
                DateTimeUtils.weekdayShortDate(today)));

        renderToday(month.dayOf(today));
        renderMissingRecord(month);
    }

    private void renderToday(@Nullable AttendanceDay day) {
        boolean hasRecord = day != null && day.hasGateRecord();

        if (hasRecord) {
            ViewUtils.statusChip(binding.chipFace, R.string.home_face_verified,
                    R.color.state_present, R.color.state_present_bg);
            binding.textGateIn.setText(DateTimeUtils.time(day.firstIn()));
            binding.textGateOut.setText(day.isStillInside()
                    ? getString(R.string.value_placeholder)
                    : DateTimeUtils.time(day.lastOut()));

            // While the employee is still on site the counter has to keep
            // running, so the open span is measured up to now.
            int inside = day.isStillInside()
                    ? lk.synergypharma.employee.domain.usecase.CalculateWorkedTime
                    .insideMinutes(day.events, LocalDateTime.now())
                    : day.insideMinutes;
            binding.textInside.setText(DateTimeUtils.duration(inside));

            GateEvent latest = day.events.get(day.events.size() - 1);
            Employee employee = viewModel.employee().getValue();
            binding.textLocation.setText(employee == null
                    ? latest.gateName
                    : latest.gateName + " · " + employee.location);
        } else {
            ViewUtils.statusChip(binding.chipFace, R.string.home_face_pending,
                    R.color.slate, R.color.slate_bg);
            String dash = getString(R.string.value_placeholder);
            binding.textGateIn.setText(dash);
            binding.textGateOut.setText(dash);
            binding.textInside.setText(DateTimeUtils.duration(0));

            Employee employee = viewModel.employee().getValue();
            binding.textLocation.setText(employee == null ? "" : employee.location);
        }

        binding.buttonGateLog.setOnClickListener(v -> openGateLog(LocalDate.now()));
    }

    /**
     * The single highest-value thing on this screen. Face recognition misses
     * people — a mask, a cap, a camera down — and without this prompt the
     * employee finds out on payday.
     */
    private void renderMissingRecord(@NonNull MonthSummary month) {
        List<AttendanceDay> pending = month.daysNeedingExplanation();
        if (pending.isEmpty()) {
            ViewUtils.visible(binding.cardMissing, false);
            return;
        }

        AttendanceDay day = pending.get(0);
        LocalDate deadline = day.date.plusDays(ValidateAbsenceReason.SUBMISSION_WINDOW_DAYS);

        ViewUtils.visible(binding.cardMissing, true);
        binding.textMissingTitle.setText(getString(R.string.home_missing_title,
                DateTimeUtils.shortDate(day.date)));
        binding.textMissingSub.setText(getString(R.string.home_missing_sub,
                DateTimeUtils.shortDate(deadline)));
        binding.buttonSubmitReason.setOnClickListener(v -> openAbsenceReason(day.date));
        binding.cardMissing.setOnClickListener(v -> openAbsenceReason(day.date));
    }

    // ------------------------------------------------------- quick actions

    private void setUpQuickActions() {
        quickAction(binding.actionLeave, R.drawable.ic_calendar, R.string.home_qa_apply_leave,
                v -> nav().navigate(R.id.action_global_apply_leave));
        quickAction(binding.actionAbsence, R.drawable.ic_medical, R.string.home_qa_absence_reason,
                v -> openAbsenceReason(null));
        quickAction(binding.actionOvertime, R.drawable.ic_clock, R.string.home_qa_my_ot,
                v -> nav().navigate(R.id.action_global_overtime));
        quickAction(binding.actionAttendance, R.drawable.ic_chart, R.string.nav_attendance,
                v -> nav().navigate(R.id.action_global_attendance));
    }

    private void quickAction(@NonNull IncludeQuickActionBinding tile,
                             @DrawableRes int iconRes,
                             @StringRes int labelRes,
                             @NonNull View.OnClickListener onClick) {
        tile.icon.setImageResource(iconRes);
        tile.label.setText(labelRes);
        tile.getRoot().setOnClickListener(onClick);
    }

    // ---------------------------------------------------------- navigation

    private void openGateLog(@NonNull LocalDate date) {
        Bundle args = new Bundle();
        args.putString(Constants.ARG_DATE, DateTimeUtils.toIso(date));
        nav().navigate(R.id.action_global_gate_log, args);
    }

    private void openAbsenceReason(@Nullable LocalDate date) {
        Bundle args = new Bundle();
        if (date != null) {
            args.putString(Constants.ARG_DATE, DateTimeUtils.toIso(date));
        }
        nav().navigate(R.id.action_global_absence_reason, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
