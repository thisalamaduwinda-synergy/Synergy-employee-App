package lk.synergypharma.employee.ui.leave;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;
import java.util.Locale;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.FragmentLeaveBinding;
import lk.synergypharma.employee.databinding.IncludeLeaveBucketBinding;
import lk.synergypharma.employee.domain.model.LeaveBalance;
import lk.synergypharma.employee.domain.model.LeaveRequest;
import lk.synergypharma.employee.domain.model.LeaveSnapshot;
import lk.synergypharma.employee.domain.model.enums.LeaveType;
import lk.synergypharma.employee.ui.common.BaseFragment;
import lk.synergypharma.employee.util.Result;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * Screen 07 — how much leave is left, then what has been asked for.
 *
 * <p>Every number here comes down from the HR database. Nothing is recalculated
 * on the phone: if the app and payroll ever disagree, payroll has to win, and
 * the only way to guarantee that is not to do the arithmetic twice.
 */
public final class LeaveFragment extends BaseFragment {

    private FragmentLeaveBinding binding;
    private LeaveViewModel viewModel;
    private LeaveRequestAdapter adapter;
    private boolean firstResume = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLeaveBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LeaveViewModel.class);

        ViewUtils.applySystemBarPadding(binding.header, true, false);

        adapter = new LeaveRequestAdapter();
        binding.listRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listRequests.setAdapter(adapter);

        binding.fabApply.setOnClickListener(
                v -> nav().navigate(R.id.action_global_apply_leave));
        binding.refresh.setOnRefreshListener(() -> viewModel.refresh());
        binding.refresh.setColorSchemeResources(R.color.synergy_blue_700);

        viewModel.snapshot().observe(getViewLifecycleOwner(), this::render);
    }

    @Override
    public void onResume() {
        super.onResume();
        // A request submitted on the apply screen has to appear the moment we
        // come back — but the ViewModel already loaded once on creation.
        if (!firstResume) {
            viewModel.refresh();
        }
        firstResume = false;
    }

    private void render(@Nullable Result<LeaveSnapshot> result) {
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

        LeaveSnapshot snapshot = result.data;
        LeaveBalance balance = snapshot.balance;

        binding.textBalanceYear.setText(getString(R.string.leave_balance_year,
                String.valueOf(balance.year)));
        binding.textDaysLeft.setText(getString(R.string.leave_days_left,
                trimZero(balance.totalRemaining())));

        bucket(binding.bucketAnnual, balance, LeaveType.ANNUAL, R.color.synergy_blue_600);
        bucket(binding.bucketCasual, balance, LeaveType.CASUAL, R.color.synergy_blue_500);
        bucket(binding.bucketMedical, balance, LeaveType.MEDICAL, R.color.synergy_blue_800);

        List<LeaveRequest> requests = snapshot.requests;
        adapter.submit(requests);
        ViewUtils.visible(binding.listRequests, !requests.isEmpty());
        ViewUtils.visible(binding.textEmpty, requests.isEmpty());
    }

    private void bucket(@NonNull IncludeLeaveBucketBinding row,
                        @NonNull LeaveBalance balance,
                        @NonNull LeaveType type,
                        int colorRes) {
        LeaveBalance.Bucket data = balance.bucketOf(type);
        row.label.setText(type.labelRes);

        if (data == null) {
            row.value.setText(R.string.value_placeholder);
            row.bar.setProgress(0);
            return;
        }
        row.value.setText(getString(R.string.leave_used_of,
                trimZero(data.used), trimZero(data.entitled)));
        row.bar.setProgress(data.usedPercent());
        row.bar.setIndicatorColor(ContextCompat.getColor(requireContext(), colorRes));
    }

    /** "14" not "14.0", but still "0.5" when someone took half a day. */
    @NonNull
    private static String trimZero(float value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format(Locale.getDefault(), "%.1f", value);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
