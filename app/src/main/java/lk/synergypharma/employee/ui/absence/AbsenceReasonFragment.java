package lk.synergypharma.employee.ui.absence;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.FragmentAbsenceReasonBinding;
import lk.synergypharma.employee.domain.model.AbsenceReason;
import lk.synergypharma.employee.domain.model.Attachment;
import lk.synergypharma.employee.domain.model.enums.AbsenceReasonType;
import lk.synergypharma.employee.domain.usecase.ValidateAbsenceReason;
import lk.synergypharma.employee.ui.common.BaseFragment;
import lk.synergypharma.employee.util.Constants;
import lk.synergypharma.employee.util.DateTimeUtils;
import lk.synergypharma.employee.util.Result;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * Screen 05 — tell HR why the gate has no record of you.
 *
 * <p>This and the leave request are the only two things the app writes. It is
 * also the most valuable screen in the release: face recognition misses people,
 * and without a way to say so the employee finds out on payday and HR finds out
 * by telephone.
 */
public final class AbsenceReasonFragment extends BaseFragment
        implements DocumentPickerHelper.Listener {

    private FragmentAbsenceReasonBinding binding;
    private AbsenceViewModel viewModel;
    private AttachmentAdapter adapter;
    private DocumentPickerHelper picker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Registered here because an ActivityResultLauncher cannot be created
        // once the fragment has reached STARTED.
        picker = new DocumentPickerHelper(this, this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAbsenceReasonBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AbsenceViewModel.class);

        ViewUtils.applySystemBarPadding(binding.header.headerRoot, true, false);
        ViewUtils.applySystemBarPadding(binding.scroll, false, true);

        binding.header.textTitle.setText(R.string.absence_title);
        binding.header.buttonBack.setOnClickListener(v -> nav().popBackStack());

        if (viewModel.date() == null) {
            viewModel.setDate(readDateArg());
        }
        renderDate();
        renderAlert();

        buildReasonChips();

        adapter = new AttachmentAdapter();
        adapter.setOnRemove(index -> viewModel.removeAttachment(index));
        binding.listAttachments.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.listAttachments.setAdapter(adapter);

        binding.boxUpload.setOnClickListener(v -> picker.chooseSource());
        binding.inputDate.setEndIconOnClickListener(v -> showDatePicker());
        binding.editDate.setOnClickListener(v -> showDatePicker());
        binding.buttonSend.setOnClickListener(v -> submit());

        viewModel.attachments().observe(getViewLifecycleOwner(), this::renderAttachments);
        viewModel.submission().observe(getViewLifecycleOwner(), this::renderSubmission);
    }

    // ----------------------------------------------------------------- date

    @Nullable
    private LocalDate readDateArg() {
        Bundle args = getArguments();
        String iso = args == null ? null : args.getString(Constants.ARG_DATE);
        if (iso == null) {
            return null;
        }
        try {
            return DateTimeUtils.fromIso(iso);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void renderDate() {
        LocalDate date = viewModel.date();
        binding.editDate.setText(date == null ? "" : DateTimeUtils.fullDate(date));
    }

    /**
     * The alert is only shown when we know which day is at stake. Arriving from
     * the "Absence reason" shortcut with no day picked yet, there is nothing
     * truthful to put in it.
     */
    private void renderAlert() {
        LocalDate date = viewModel.date();
        if (date == null) {
            ViewUtils.visible(binding.cardAlert, false);
            return;
        }
        ViewUtils.visible(binding.cardAlert, true);
        binding.textAlertTitle.setText(getString(R.string.absence_alert_title,
                DateTimeUtils.fullDate(date)));
        binding.textAlertSub.setText(getString(R.string.absence_alert_sub,
                ValidateAbsenceReason.SUBMISSION_WINDOW_DAYS));
    }

    private void showDatePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                // A reason for a day that has not happened is not a reason.
                .setValidator(DateValidatorPointBackward.now())
                .build();

        LocalDate selected = viewModel.date();
        long preset = (selected == null ? LocalDate.now() : selected)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        MaterialDatePicker<Long> dialog = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.absence_date)
                .setCalendarConstraints(constraints)
                .setSelection(preset)
                .build();

        dialog.addOnPositiveButtonClickListener(millis -> {
            // The picker works in UTC; read it back the same way or the date
            // shifts by a day for anyone east of Greenwich — which is everyone here.
            viewModel.setDate(java.time.Instant.ofEpochMilli(millis)
                    .atZone(ZoneOffset.UTC).toLocalDate());
            renderDate();
            renderAlert();
            binding.inputDate.setError(null);
        });
        dialog.show(getChildFragmentManager(), "absence_date");
    }

    // ----------------------------------------------------------- reason type

    private void buildReasonChips() {
        binding.groupReason.removeAllViews();
        for (AbsenceReasonType type : AbsenceReasonType.values()) {
            Chip chip = new Chip(requireContext(), null,
                    com.google.android.material.R.attr.chipStyle);
            chip.setId(View.generateViewId());
            chip.setText(type.labelRes);
            chip.setCheckable(true);
            chip.setTag(type);
            chip.setTextAppearance(R.style.TextAppearance_Synergy_Body);
            chip.setChecked(type == viewModel.type());
            binding.groupReason.addView(chip);
        }

        binding.groupReason.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                viewModel.setType(null);
                return;
            }
            View checked = group.findViewById(checkedIds.get(0));
            if (checked != null && checked.getTag() instanceof AbsenceReasonType) {
                viewModel.setType((AbsenceReasonType) checked.getTag());
                updateUploadHint();
            }
        });
        updateUploadHint();
    }

    /** A medical reason without a certificate will be bounced by HR, so say so. */
    private void updateUploadHint() {
        AbsenceReasonType type = viewModel.type();
        boolean required = type != null && type.requiresDocument;
        binding.textUploadTitle.setText(viewModel.currentAttachments().isEmpty()
                ? R.string.absence_add_file
                : R.string.absence_add_another_file);
        binding.boxUpload.setAlpha(required && viewModel.currentAttachments().isEmpty()
                ? 1f : 0.92f);
    }

    // ----------------------------------------------------------- attachments

    private void renderAttachments(@Nullable List<Attachment> items) {
        List<Attachment> list = items == null ? java.util.Collections.emptyList() : items;
        adapter.submit(list);
        ViewUtils.visible(binding.cardAttachments, !list.isEmpty());
        updateUploadHint();
    }

    @Override
    public void onPicked(@NonNull Attachment attachment) {
        viewModel.addAttachment(attachment);
    }

    @Override
    public void onPickFailed(@StringRes int messageRes) {
        toast(messageRes);
    }

    @Override
    public void onPickBusy(boolean busy) {
        if (binding != null) {
            ViewUtils.visible(binding.progress, busy);
            binding.boxUpload.setEnabled(!busy);
        }
    }

    // ---------------------------------------------------------------- submit

    private void submit() {
        binding.inputDate.setError(null);
        binding.inputExplanation.setError(null);

        LocalDate date = viewModel.date();
        if (date == null) {
            binding.inputDate.setError(getString(R.string.absence_date));
            return;
        }

        CharSequence typed = binding.editExplanation.getText();
        String explanation = typed == null ? "" : typed.toString().trim();

        ValidateAbsenceReason.Error error = ValidateAbsenceReason.validate(
                viewModel.type(), explanation, viewModel.currentAttachments());

        switch (error) {
            case NO_TYPE:
                toast(R.string.absence_error_no_reason);
                return;
            case NO_EXPLANATION:
                binding.inputExplanation.setError(
                        getString(R.string.absence_error_no_explanation));
                return;
            case MISSING_DOCUMENT:
                toast(R.string.absence_error_medical_needs_doc);
                return;
            case FILE_TOO_LARGE:
                toast(R.string.absence_error_file_too_large);
                return;
            default:
                break;
        }

        AbsenceReasonType type = viewModel.type();
        if (type == null) {
            return;
        }
        viewModel.submit(date, type, explanation);
    }

    private void renderSubmission(@Nullable Result<AbsenceReason> result) {
        if (result == null) {
            return;
        }
        ViewUtils.visible(binding.progress, result.isLoading());
        binding.buttonSend.setEnabled(!result.isLoading());

        if (result.isError()) {
            toast(result.message == null ? getString(R.string.error_generic) : result.message);
            viewModel.consumeSubmission();
            return;
        }
        if (result.isSuccess()) {
            viewModel.consumeSubmission();
            toast(R.string.absence_submitted);
            nav().popBackStack();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
