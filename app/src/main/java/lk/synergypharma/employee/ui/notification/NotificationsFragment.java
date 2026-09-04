package lk.synergypharma.employee.ui.notification;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.FragmentNotificationsBinding;
import lk.synergypharma.employee.domain.model.AppNotification;
import lk.synergypharma.employee.domain.model.enums.NotificationType;
import lk.synergypharma.employee.ui.common.BaseFragment;
import lk.synergypharma.employee.util.Constants;
import lk.synergypharma.employee.util.Result;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * Screen 13 — everything time-sensitive in one list.
 *
 * <p>Tapping a row has to land somewhere useful, not on a generic screen: a
 * missing gate record opens the reason form already set to that date, an OT
 * notice opens that day's gate log. A notification you cannot act on is just
 * noise, and noise is what makes people turn notifications off.
 */
public final class NotificationsFragment extends BaseFragment {

    private FragmentNotificationsBinding binding;
    private NotificationsViewModel viewModel;
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        ViewUtils.applySystemBarPadding(binding.header.headerRoot, true, false);
        ViewUtils.applySystemBarPadding(binding.scroll, false, true);

        binding.header.textTitle.setText(R.string.notifications_title);
        binding.header.buttonBack.setOnClickListener(v -> nav().popBackStack());
        binding.header.textAction.setVisibility(View.VISIBLE);
        binding.header.textAction.setText(R.string.notifications_mark_all_read);
        binding.header.textAction.setOnClickListener(v -> viewModel.markAllRead());

        adapter = new NotificationAdapter();
        adapter.setOnClick(this::open);
        binding.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.list.setAdapter(adapter);

        viewModel.notifications().observe(getViewLifecycleOwner(), this::render);
        viewModel.markAll().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                viewModel.consumeMarkAll();
                viewModel.refresh();
            }
        });
    }

    private void render(@Nullable Result<List<AppNotification>> result) {
        if (result == null || !result.isSuccess() || result.data == null) {
            if (result != null && result.isError()) {
                toast(result.message == null ? getString(R.string.error_generic) : result.message);
            }
            return;
        }

        List<AppNotification> items = result.data;
        adapter.submit(items, new String[]{
                getString(R.string.notifications_today),
                getString(R.string.notifications_this_week),
                getString(R.string.notifications_earlier)});

        ViewUtils.visible(binding.list, !items.isEmpty());
        ViewUtils.visible(binding.textEmpty, items.isEmpty());
    }

    /** Routes each notification to the screen that can actually resolve it. */
    private void open(@NonNull AppNotification notification) {
        Bundle args = new Bundle();
        if (notification.targetDate != null) {
            args.putString(Constants.ARG_DATE, notification.targetDate);
        }

        if (notification.type == NotificationType.MISSING_GATE_RECORD) {
            nav().navigate(R.id.action_global_absence_reason, args);
            return;
        }
        if (notification.type == NotificationType.OVERTIME) {
            nav().navigate(R.id.action_global_overtime);
            return;
        }
        if (notification.type == NotificationType.LEAVE_DECISION) {
            nav().navigate(R.id.leaveFragment);
            return;
        }
        // Payslips and announcements land in v1.1; say so rather than do nothing.
        toast(R.string.more_coming_soon);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
