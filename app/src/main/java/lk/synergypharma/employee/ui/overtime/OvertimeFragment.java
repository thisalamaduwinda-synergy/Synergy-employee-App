package lk.synergypharma.employee.ui.overtime;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.FragmentOvertimeBinding;
import lk.synergypharma.employee.domain.model.Overtime;
import lk.synergypharma.employee.ui.common.BaseFragment;
import lk.synergypharma.employee.util.Constants;
import lk.synergypharma.employee.util.CurrencyUtils;
import lk.synergypharma.employee.util.DateTimeUtils;
import lk.synergypharma.employee.util.Result;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * Screen 06 — overtime, derived from the gate log.
 *
 * <p>Read-only, because there is nothing here for an employee to type: the
 * minutes come from when they walked out of the gate. What they can do is check
 * the working — tapping a row opens that day's timeline.
 */
public final class OvertimeFragment extends BaseFragment {

    private FragmentOvertimeBinding binding;
    private OvertimeViewModel viewModel;
    private OvertimeAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOvertimeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OvertimeViewModel.class);

        ViewUtils.applySystemBarPadding(binding.header.headerRoot, true, false);
        ViewUtils.applySystemBarPadding(binding.scroll, false, true);

        binding.header.textTitle.setText(R.string.overtime_title);
        binding.header.buttonBack.setOnClickListener(v -> nav().popBackStack());

        binding.statApproved.label.setText(R.string.overtime_approved);
        binding.statApproved.value.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.state_present));
        binding.statPending.label.setText(R.string.overtime_pending);
        binding.statPending.value.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.synergy_blue_700));
        binding.statRate.label.setText(R.string.overtime_rate);

        adapter = new OvertimeAdapter();
        adapter.setOnEntryClick(this::openGateLog);
        binding.listEntries.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listEntries.setAdapter(adapter);

        viewModel.overtime().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@Nullable Result<Overtime> result) {
        if (result == null || !result.isSuccess() || result.data == null) {
            if (result != null && result.isError()) {
                toast(result.message == null ? getString(R.string.error_generic) : result.message);
            }
            return;
        }

        Overtime overtime = result.data;
        binding.textMonth.setText(DateTimeUtils.month(overtime.month));
        binding.textTotal.setText(DateTimeUtils.duration(overtime.totalMinutes));
        binding.textValue.setText(getString(R.string.overtime_estimated_value,
                CurrencyUtils.format(overtime.estimatedValue)));

        // Provisional until payroll locks the month — say so rather than let
        // anyone plan around a number that can still move.
        ViewUtils.statusChip(binding.chipProvisional, R.string.overtime_provisional,
                R.color.slate, R.color.slate_bg);

        binding.statApproved.value.setText(DateTimeUtils.duration(overtime.approvedMinutes));
        binding.statPending.value.setText(DateTimeUtils.duration(overtime.pendingMinutes));
        binding.statRate.value.setText(
                String.format(Locale.getDefault(), "%.1f×", overtime.rateMultiplier));

        List<Overtime.Entry> entries = overtime.entries;
        adapter.submit(entries);
        ViewUtils.visible(binding.listEntries, !entries.isEmpty());
        ViewUtils.visible(binding.textEmpty, entries.isEmpty());
    }

    private void openGateLog(@NonNull LocalDate date) {
        Bundle args = new Bundle();
        args.putString(Constants.ARG_DATE, DateTimeUtils.toIso(date));
        nav().navigate(R.id.action_global_gate_log, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
