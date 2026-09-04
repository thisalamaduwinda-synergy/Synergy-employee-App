package lk.synergypharma.employee.ui.attendance;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.FragmentGateLogBinding;
import lk.synergypharma.employee.domain.model.AttendanceDay;
import lk.synergypharma.employee.ui.common.BaseFragment;
import lk.synergypharma.employee.util.Constants;
import lk.synergypharma.employee.util.DateTimeUtils;
import lk.synergypharma.employee.util.Result;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * Screen 03 — every IN and OUT the gate recorded for one day.
 *
 * <p>Read-only. The one action is "report an incorrect record", which opens a
 * review with HR and changes nothing on the record itself. That is what keeps
 * the face-recognition system the single source of truth and the audit trail
 * intact.
 */
public final class GateLogFragment extends BaseFragment {

    private FragmentGateLogBinding binding;
    private GateLogViewModel viewModel;
    private GateLogAdapter adapter;
    private LocalDate date;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGateLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(GateLogViewModel.class);
        date = readDateArg();

        ViewUtils.applySystemBarPadding(binding.header, true, false);
        ViewUtils.applySystemBarPadding(binding.scroll, false, true);

        binding.textTitle.setText(getString(R.string.gate_log_title,
                DateTimeUtils.shortDate(date)));
        binding.buttonBack.setOnClickListener(v -> nav().popBackStack());

        adapter = new GateLogAdapter();
        binding.listEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listEvents.setAdapter(adapter);

        binding.rowShift.key.setText(R.string.gate_shift);
        binding.rowInside.key.setText(R.string.gate_total_inside);
        binding.rowBreak.key.setText(R.string.gate_break_deducted);
        binding.rowLate.key.setText(R.string.gate_late_by);
        binding.rowOt.key.setText(R.string.gate_approved_ot);
        binding.rowOt.value.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(),
                        R.color.synergy_blue_700));

        binding.buttonReport.setOnClickListener(v -> askForCorrection());

        viewModel.day().observe(getViewLifecycleOwner(), this::render);
        viewModel.correction().observe(getViewLifecycleOwner(), this::renderCorrection);
        viewModel.load(date);
    }

    @NonNull
    private LocalDate readDateArg() {
        Bundle args = getArguments();
        String iso = args == null ? null : args.getString(Constants.ARG_DATE);
        if (iso == null) {
            return LocalDate.now();
        }
        try {
            return DateTimeUtils.fromIso(iso);
        } catch (RuntimeException e) {
            return LocalDate.now();
        }
    }

    private void render(@Nullable Result<AttendanceDay> result) {
        if (result == null || !result.isSuccess() || result.data == null) {
            if (result != null && result.isError()) {
                toast(result.message == null ? getString(R.string.error_generic) : result.message);
            }
            return;
        }

        AttendanceDay day = result.data;
        adapter.submit(day.events);

        boolean hasEvents = !day.events.isEmpty();
        ViewUtils.visible(binding.listEvents, hasEvents);
        ViewUtils.visible(binding.textEmpty, !hasEvents);

        binding.chipInside.setText(getString(R.string.gate_chip_inside,
                DateTimeUtils.duration(day.workedMinutes())));
        ViewUtils.visible(binding.chipOt, day.overtimeMinutes > 0);
        binding.chipOt.setText(getString(R.string.gate_chip_ot,
                DateTimeUtils.duration(day.overtimeMinutes)));

        binding.rowShift.value.setText(DateTimeUtils.time(day.shiftStart)
                + " – " + DateTimeUtils.time(day.shiftEnd));
        binding.rowInside.value.setText(DateTimeUtils.duration(day.insideMinutes));
        binding.rowBreak.value.setText(DateTimeUtils.duration(day.breakMinutes));
        binding.rowOt.value.setText(DateTimeUtils.duration(day.overtimeMinutes));

        // Lateness is only worth a row on a day the employee was actually late.
        ViewUtils.visible(binding.rowLate.getRoot(), day.lateMinutes > 0);
        binding.rowLate.value.setText(DateTimeUtils.duration(day.lateMinutes));

        renderProvenance(day);
    }

    /**
     * Explains a number that would otherwise look wrong. Both cases below are
     * ones an employee would reasonably dispute, so saying nothing would leave
     * them arguing with a figure they cannot see the reason for.
     */
    private void renderProvenance(@NonNull AttendanceDay day) {
        if (day.manuallyAdjusted) {
            binding.textProvenance.setText(getString(R.string.gate_adjusted_by,
                    day.adjustedBy == null ? "HR" : day.adjustedBy,
                    day.adjustmentReason == null ? "" : day.adjustmentReason));
            ViewUtils.visible(binding.textProvenance, true);
        } else if (day.missingCheckout) {
            binding.textProvenance.setText(R.string.gate_missing_checkout);
            ViewUtils.visible(binding.textProvenance, true);
        } else {
            ViewUtils.visible(binding.textProvenance, false);
        }
    }

    // -------------------------------------------------------- dispute a day

    private void askForCorrection() {
        EditText input = new EditText(requireContext());
        input.setHint(R.string.gate_correction_hint);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(3);
        input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        int pad = ViewUtils.dp(requireContext(), 20f);
        input.setPadding(pad, pad / 2, pad, pad / 2);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.gate_correction_title)
                .setView(input)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_submit, (d, w) -> {
                    CharSequence message = input.getText();
                    if (message == null || message.toString().trim().isEmpty()) {
                        return;
                    }
                    viewModel.submitCorrection(date, message.toString().trim());
                })
                .show();
    }

    private void renderCorrection(@Nullable Result<Boolean> result) {
        if (result == null || !result.isSuccess()) {
            if (result != null && result.isError()) {
                toast(result.message == null ? getString(R.string.error_generic) : result.message);
                viewModel.consumeCorrection();
            }
            return;
        }
        toast(R.string.gate_correction_sent);
        viewModel.consumeCorrection();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
