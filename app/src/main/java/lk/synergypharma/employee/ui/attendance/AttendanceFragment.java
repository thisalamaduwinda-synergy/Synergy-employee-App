package lk.synergypharma.employee.ui.attendance;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.time.YearMonth;
import java.util.List;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.FragmentAttendanceBinding;
import lk.synergypharma.employee.databinding.IncludeLegendBinding;
import lk.synergypharma.employee.databinding.IncludeStatBinding;
import lk.synergypharma.employee.domain.model.AttendanceDay;
import lk.synergypharma.employee.domain.model.MonthSummary;
import lk.synergypharma.employee.domain.model.enums.DayState;
import lk.synergypharma.employee.ui.common.BaseFragment;
import lk.synergypharma.employee.util.Constants;
import lk.synergypharma.employee.util.DateTimeUtils;
import lk.synergypharma.employee.util.Result;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * Screen 04 — the month at a glance.
 *
 * <p>Tapping any day drills into what the gate actually recorded (screen 03),
 * except for a day with no record at all, which goes straight to the reason form
 * — that is the day the employee needs to act on.
 */
public final class AttendanceFragment extends BaseFragment {

    private FragmentAttendanceBinding binding;
    private AttendanceViewModel viewModel;
    private CalendarAdapter calendarAdapter;
    private AttendanceDayAdapter dayAdapter;
    private boolean firstResume = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAttendanceBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AttendanceViewModel.class);

        ViewUtils.applySystemBarPadding(binding.header, true, false);

        buildWeekdayHeader();
        setUpLabels();

        calendarAdapter = new CalendarAdapter();
        calendarAdapter.setOnDayClick(this::openDay);
        binding.gridCalendar.setLayoutManager(
                new GridLayoutManager(requireContext(), CalendarAdapter.COLUMNS));
        binding.gridCalendar.setAdapter(calendarAdapter);

        dayAdapter = new AttendanceDayAdapter();
        dayAdapter.setOnDayClick(this::openDay);
        binding.listDays.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listDays.setAdapter(dayAdapter);

        binding.buttonPrevMonth.setOnClickListener(v -> viewModel.previousMonth());
        binding.buttonNextMonth.setOnClickListener(v -> viewModel.nextMonth());
        binding.refresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.refresh.setColorSchemeResources(R.color.synergy_blue_700);

        viewModel.month().observe(getViewLifecycleOwner(), this::renderMonthLabel);
        viewModel.summary().observe(getViewLifecycleOwner(), this::render);
    }

    // ---------------------------------------------------------------- setup

    /**
     * Built in code from a single translatable string so the Sinhala initials
     * (ස, අ, බ …) drop straight in without touching the layout.
     */
    private void buildWeekdayHeader() {
        String[] initials = getString(R.string.weekday_initials).split(",");
        binding.rowWeekdays.removeAllViews();
        for (String initial : initials) {
            TextView label = new TextView(requireContext());
            label.setText(initial.trim());
            label.setGravity(Gravity.CENTER);
            label.setTextSize(10.5f);
            label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
            label.setTextColor(ContextCompat.getColor(requireContext(), R.color.muted));
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            binding.rowWeekdays.addView(label);
        }
    }

    private void setUpLabels() {
        label(binding.statWorked, R.string.attendance_worked, R.color.state_present);
        label(binding.statOt, R.string.attendance_ot_hours, R.color.synergy_blue_700);
        label(binding.statLeave, R.string.attendance_leave, R.color.state_leave);
        label(binding.statAbsent, R.string.attendance_absent, R.color.state_absent);

        legend(binding.legendWorked, R.string.day_state_worked, R.color.state_present);
        legend(binding.legendLeave, R.string.day_state_leave, R.color.state_leave);
        legend(binding.legendAbsent, R.string.day_state_absent, R.color.state_absent);
    }

    private void label(@NonNull IncludeStatBinding stat, int labelRes, int colorRes) {
        stat.label.setText(labelRes);
        stat.value.setTextColor(ContextCompat.getColor(requireContext(), colorRes));
    }

    private void legend(@NonNull IncludeLegendBinding legend, int labelRes, int colorRes) {
        legend.label.setText(labelRes);
        ViewUtils.tintBackground(legend.swatch, colorRes);
    }

    // --------------------------------------------------------------- render

    private void renderMonthLabel(@Nullable YearMonth month) {
        if (month == null) {
            return;
        }
        binding.textMonth.setText(DateTimeUtils.month(month));
        boolean forward = viewModel.canGoForward();
        binding.buttonNextMonth.setEnabled(forward);
        binding.buttonNextMonth.setAlpha(forward ? 1f : 0.35f);
    }

    private void render(@Nullable Result<MonthSummary> result) {
        if (result == null) {
            return;
        }
        binding.refresh.setRefreshing(result.isLoading());
        if (result.isError()) {
            toast(result.message == null ? getString(R.string.error_generic) : result.message);
            return;
        }
        if (!result.isSuccess() || result.data == null) {
            return;
        }

        MonthSummary summary = result.data;
        binding.statWorked.value.setText(String.valueOf(summary.workedDays));
        binding.statOt.value.setText(DateTimeUtils.hoursDecimal(summary.overtimeMinutes));
        binding.statLeave.value.setText(String.valueOf(summary.leaveDays));
        binding.statAbsent.value.setText(String.valueOf(summary.absentDays));

        calendarAdapter.submit(summary);

        List<AttendanceDay> notable = summary.notableDays();
        dayAdapter.submit(notable);
        ViewUtils.visible(binding.listDays, !notable.isEmpty());
        ViewUtils.visible(binding.textEmpty, notable.isEmpty());
    }

    // ----------------------------------------------------------- navigation

    /**
     * A day with no gate record is the one the employee has to do something
     * about, so it skips the timeline and opens the reason form directly.
     */
    private void openDay(@NonNull AttendanceDay day) {
        Bundle args = new Bundle();
        args.putString(Constants.ARG_DATE, DateTimeUtils.toIso(day.date));

        if (day.state == DayState.ABSENT && !day.reasonSubmitted) {
            nav().navigate(R.id.action_global_absence_reason, args);
            return;
        }
        if (!day.hasGateRecord()) {
            return;
        }
        nav().navigate(R.id.action_global_gate_log, args);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Returning from the absence form must repaint the calendar, but the
        // ViewModel already loaded on creation — refetching there too would
        // fire two requests on every first open.
        if (!firstResume) {
            viewModel.refresh();
        }
        firstResume = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
