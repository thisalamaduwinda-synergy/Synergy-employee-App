package lk.synergypharma.employee.ui.leave;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.FragmentApplyLeaveBinding;
import lk.synergypharma.employee.domain.model.Colleague;
import lk.synergypharma.employee.domain.model.LeaveRequest;
import lk.synergypharma.employee.domain.model.enums.LeaveType;
import lk.synergypharma.employee.domain.usecase.ValidateLeaveRequest;
import lk.synergypharma.employee.ui.common.BaseFragment;
import lk.synergypharma.employee.util.DateTimeUtils;
import lk.synergypharma.employee.util.Result;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * Screen 08 — one page, no wizard.
 *
 * <p>Dates, day count, covering officer and approver all sit on the same screen
 * because they are decided together: the day count changes which approver is
 * needed, and the covering officer depends on how long you are away.
 */
public final class ApplyLeaveFragment extends BaseFragment {

    private FragmentApplyLeaveBinding binding;
    private ApplyLeaveViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentApplyLeaveBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ApplyLeaveViewModel.class);

        ViewUtils.applySystemBarPadding(binding.header.headerRoot, true, false);
        ViewUtils.applySystemBarPadding(binding.scroll, false, true);

        binding.header.textTitle.setText(R.string.apply_leave_title);
        binding.header.buttonBack.setOnClickListener(v -> nav().popBackStack());

        buildTypeChips();
        setUpDates();
        setUpPortion();

        binding.buttonSubmit.setOnClickListener(v -> submit());

        viewModel.totalDays().observe(getViewLifecycleOwner(), this::renderTotal);
        viewModel.colleagues().observe(getViewLifecycleOwner(), this::renderPeople);
        viewModel.snapshot().observe(getViewLifecycleOwner(), result -> {
            // The holiday calendar arrives with the balance, so the day count is
            // provisional until it lands. Recount the moment it does.
            if (result != null && result.isSuccess()) {
                viewModel.recount();
            }
        });
        viewModel.submission().observe(getViewLifecycleOwner(), this::renderSubmission);

        viewModel.recount();
    }

    // ---------------------------------------------------------------- type

    private void buildTypeChips() {
        binding.groupType.removeAllViews();
        // Only the four an employee may apply for. Maternity and short leave
        // exist on the backend and render in history, but HR raises those.
        for (LeaveType type : LeaveType.selectable()) {
            Chip chip = new Chip(requireContext(), null,
                    com.google.android.material.R.attr.chipStyle);
            chip.setId(View.generateViewId());
            chip.setText(type.labelRes);
            chip.setCheckable(true);
            chip.setTag(type);
            chip.setChecked(type == viewModel.type());
            binding.groupType.addView(chip);
        }
        binding.groupType.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            View checked = group.findViewById(checkedIds.get(0));
            if (checked != null && checked.getTag() instanceof LeaveType) {
                viewModel.setType((LeaveType) checked.getTag());
            }
        });
    }

    // --------------------------------------------------------------- dates

    private void setUpDates() {
        binding.editFrom.setOnClickListener(v -> pickDate(true));
        binding.editTo.setOnClickListener(v -> pickDate(false));
        renderDates();
    }

    private void renderDates() {
        binding.editFrom.setText(DateTimeUtils.shortDate(viewModel.from())
                + " " + viewModel.from().getYear());
        binding.editTo.setText(DateTimeUtils.shortDate(viewModel.to())
                + " " + viewModel.to().getYear());
    }

    private void pickDate(boolean isFrom) {
        LocalDate current = isFrom ? viewModel.from() : viewModel.to();

        // Leave is applied for in advance; back-dating it belongs with HR, not
        // in a self-service form.
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();

        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isFrom ? R.string.apply_from : R.string.apply_to)
                .setCalendarConstraints(constraints)
                .setSelection(current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .build();

        picker.addOnPositiveButtonClickListener(millis -> {
            LocalDate picked = Instant.ofEpochMilli(millis)
                    .atZone(ZoneOffset.UTC).toLocalDate();
            if (isFrom) {
                viewModel.setFrom(picked);
            } else {
                viewModel.setTo(picked);
            }
            renderDates();
        });
        picker.show(getChildFragmentManager(), isFrom ? "leave_from" : "leave_to");
    }

    private void setUpPortion() {
        binding.groupPortion.check(R.id.button_full_day);
        binding.groupPortion.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.button_half_am) {
                viewModel.setPortion(LeaveRequest.DayPortion.HALF_AM);
            } else if (checkedId == R.id.button_half_pm) {
                viewModel.setPortion(LeaveRequest.DayPortion.HALF_PM);
            } else {
                viewModel.setPortion(LeaveRequest.DayPortion.FULL_DAY);
            }
        });
    }

    private void renderTotal(@Nullable Float days) {
        float value = days == null ? 0f : days;
        binding.textTotalDays.setText(
                LeaveRequestAdapter.formatDays(requireContext(), value));

        // A range made entirely of weekends and Poya days costs nothing, and
        // there is nothing to submit.
        binding.buttonSubmit.setEnabled(value > 0f);
    }

    // -------------------------------------------------------------- people

    private void renderPeople(@Nullable Result<List<Colleague>> result) {
        if (result == null || !result.isSuccess() || result.data == null) {
            return;
        }
        List<Colleague> all = result.data;

        List<String> covering = new ArrayList<>();
        List<String> approvers = new ArrayList<>();
        for (Colleague c : all) {
            covering.add(c.fullName);
            if (c.canApprove) {
                approvers.add(c.fullName + " — " + c.designation);
            }
        }

        binding.editCovering.setSimpleItems(covering.toArray(new String[0]));
        binding.editApprover.setSimpleItems(approvers.toArray(new String[0]));

        binding.editCovering.setOnItemClickListener((parent, v, position, id) ->
                viewModel.setCoveringOfficer(covering.get(position)));
        binding.editApprover.setOnItemClickListener((parent, v, position, id) -> {
            String label = approvers.get(position);
            int dash = label.indexOf(" — ");
            viewModel.setApprover(dash > 0 ? label.substring(0, dash) : label);
        });

        // Sensible defaults so the common case is two taps, not four.
        if (!approvers.isEmpty() && binding.editApprover.getText().length() == 0) {
            binding.editApprover.setText(approvers.get(0), false);
            String first = approvers.get(0);
            int dash = first.indexOf(" — ");
            viewModel.setApprover(dash > 0 ? first.substring(0, dash) : first);
        }
    }

    // -------------------------------------------------------------- submit

    private void submit() {
        binding.inputReason.setError(null);
        binding.inputCovering.setError(null);
        binding.inputTo.setError(null);

        CharSequence typed = binding.editReason.getText();
        String reason = typed == null ? "" : typed.toString().trim();

        ValidateLeaveRequest.Outcome outcome = viewModel.validate(reason);
        switch (outcome.error) {
            case END_BEFORE_START:
                binding.inputTo.setError(getString(R.string.apply_error_dates));
                return;
            case NO_REASON:
                binding.inputReason.setError(getString(R.string.apply_error_no_reason));
                return;
            case NO_COVERING_OFFICER:
                binding.inputCovering.setError(getString(R.string.apply_error_no_covering));
                return;
            case INSUFFICIENT_BALANCE:
                toast(getString(R.string.apply_error_balance,
                        String.valueOf(outcome.remaining),
                        getString(viewModel.type().labelRes)));
                return;
            case OVERLAPS_EXISTING:
                toast(R.string.apply_error_overlap);
                return;
            default:
                break;
        }
        viewModel.submit(reason);
    }

    private void renderSubmission(@Nullable Result<LeaveRequest> result) {
        if (result == null) {
            return;
        }
        ViewUtils.visible(binding.progress, result.isLoading());
        binding.buttonSubmit.setEnabled(!result.isLoading());

        if (result.isError()) {
            toast(result.message == null ? getString(R.string.error_generic) : result.message);
            viewModel.consumeSubmission();
            return;
        }
        if (result.isSuccess() && result.data != null) {
            String approver = result.data.approver;
            viewModel.consumeSubmission();
            toast(getString(R.string.apply_submitted,
                    approver == null ? getString(R.string.apply_approver) : approver));
            nav().popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
