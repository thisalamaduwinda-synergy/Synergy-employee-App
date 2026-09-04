package lk.synergypharma.employee.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import lk.synergypharma.employee.BuildConfig;
import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.FragmentRegisterBinding;
import lk.synergypharma.employee.domain.model.RegistrationChallenge;
import lk.synergypharma.employee.domain.model.Session;
import lk.synergypharma.employee.domain.usecase.ValidateRegistration;
import lk.synergypharma.employee.ui.common.BaseFragment;
import lk.synergypharma.employee.util.Result;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * First-time registration.
 *
 * <p>This is an <b>activation</b>, not a sign-up. Every employee is already in
 * the recognition system — HR enrolled their face before their first day — and
 * the only thing missing is a password. So the flow proves the record belongs
 * to the person claiming it (employee number + the NIC HR holds), confirms the
 * phone number on that record with an OTP, and then sets the password.
 *
 * <p>Nothing here creates an employee. An unknown employee number is not a new
 * joiner; it is somebody guessing.
 */
public final class RegisterFragment extends BaseFragment {

    private FragmentRegisterBinding binding;
    private RegisterViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);

        ViewUtils.applySystemBarPadding(binding.header.headerRoot, true, false);
        ViewUtils.applySystemBarPadding(binding.scroll, false, true);

        binding.header.textTitle.setText(R.string.register_title);
        binding.header.buttonBack.setOnClickListener(v -> goBack());
        binding.textHaveAccount.setOnClickListener(v -> nav().popBackStack());

        binding.buttonPrimary.setOnClickListener(v -> onPrimaryAction());
        binding.buttonResend.setOnClickListener(v -> viewModel.resendOtp());

        // Back must step through the wizard, not abandon it from step 3.
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        goBack();
                    }
                });

        viewModel.step().observe(getViewLifecycleOwner(), this::renderStep);
        viewModel.challenge().observe(getViewLifecycleOwner(), this::renderChallenge);
        viewModel.verification().observe(getViewLifecycleOwner(), this::renderVerification);
        viewModel.completion().observe(getViewLifecycleOwner(), this::renderCompletion);
    }

    private void goBack() {
        if (!viewModel.goBackAStep()) {
            nav().popBackStack();
        }
    }

    // --------------------------------------------------------------- render

    private void renderStep(@Nullable Integer step) {
        if (step == null) {
            return;
        }
        binding.textStep.setText(getString(R.string.register_step, step,
                RegisterViewModel.STEP_PASSWORD));
        binding.progressSteps.setProgress(step);

        ViewUtils.visible(binding.groupIdentity, step == RegisterViewModel.STEP_IDENTITY);
        ViewUtils.visible(binding.groupOtp, step == RegisterViewModel.STEP_OTP);
        ViewUtils.visible(binding.groupPassword, step == RegisterViewModel.STEP_PASSWORD);
        ViewUtils.visible(binding.textHaveAccount, step == RegisterViewModel.STEP_IDENTITY);

        switch (step) {
            case RegisterViewModel.STEP_OTP:
                RegistrationChallenge challenge = viewModel.currentChallenge();
                binding.textStepTitle.setText(R.string.register_step2_title);
                binding.textStepSubtitle.setText(getString(R.string.register_step2_subtitle,
                        challenge == null ? "" : challenge.maskedPhone));
                binding.buttonPrimary.setText(R.string.register_verify);
                renderDemoHint(challenge);
                break;
            case RegisterViewModel.STEP_PASSWORD:
                binding.textStepTitle.setText(R.string.register_step3_title);
                binding.textStepSubtitle.setText(R.string.register_step3_subtitle);
                binding.buttonPrimary.setText(R.string.register_finish);
                break;
            default:
                binding.textStepTitle.setText(R.string.register_step1_title);
                binding.textStepSubtitle.setText(R.string.register_step1_subtitle);
                binding.buttonPrimary.setText(R.string.register_continue);
                break;
        }
    }

    /**
     * There is no SMS gateway behind the mock, so the code it "sent" is shown on
     * screen. Guarded on USE_MOCK_DATA rather than DEBUG so it cannot survive
     * into a build wired to a real backend.
     */
    private void renderDemoHint(@Nullable RegistrationChallenge challenge) {
        boolean show = BuildConfig.USE_MOCK_DATA
                && challenge != null && challenge.demoOtp != null;
        ViewUtils.visible(binding.textDemoOtp, show);
        if (show) {
            binding.textDemoOtp.setText(
                    getString(R.string.register_demo_otp, challenge.demoOtp));
        }
    }

    private void setBusy(boolean busy) {
        ViewUtils.visible(binding.progress, busy);
        binding.buttonPrimary.setEnabled(!busy);
        binding.buttonResend.setEnabled(!busy);
    }

    // -------------------------------------------------------------- actions

    private void onPrimaryAction() {
        switch (viewModel.currentStep()) {
            case RegisterViewModel.STEP_OTP:
                submitOtp();
                break;
            case RegisterViewModel.STEP_PASSWORD:
                submitPassword();
                break;
            default:
                submitIdentity();
                break;
        }
    }

    private void submitIdentity() {
        binding.inputEmployeeCode.setError(null);
        binding.inputNic.setError(null);

        String code = text(binding.editEmployeeCode);
        String nic = text(binding.editNic);

        switch (ValidateRegistration.identity(code, nic)) {
            case NO_EMPLOYEE_CODE:
                binding.inputEmployeeCode.setError(getString(R.string.login_error_id_required));
                return;
            case NO_NIC:
                binding.inputNic.setError(getString(R.string.register_error_nic_required));
                return;
            case NIC_FORMAT:
                binding.inputNic.setError(getString(R.string.register_error_nic_format));
                return;
            default:
                break;
        }
        viewModel.startRegistration(code, ValidateRegistration.normaliseNic(nic));
    }

    private void submitOtp() {
        binding.inputOtp.setError(null);
        String otp = text(binding.editOtp);
        if (otp.isEmpty()) {
            binding.inputOtp.setError(getString(R.string.register_error_otp_required));
            return;
        }
        viewModel.verifyOtp(otp);
    }

    private void submitPassword() {
        binding.inputPassword.setError(null);
        binding.inputConfirm.setError(null);

        String password = text(binding.editPassword);
        String confirm = text(binding.editConfirm);

        switch (ValidateRegistration.password(
                viewModel.employeeCode(), viewModel.nic(), password, confirm)) {
            case TOO_SHORT:
                binding.inputPassword.setError(getString(R.string.register_error_short,
                        ValidateRegistration.MIN_PASSWORD_LENGTH));
                return;
            case NEEDS_LETTER_AND_DIGIT:
                binding.inputPassword.setError(getString(R.string.register_error_mix));
                return;
            case CONTAINS_IDENTITY:
                binding.inputPassword.setError(getString(R.string.register_error_identity));
                return;
            case MISMATCH:
                binding.inputConfirm.setError(getString(R.string.register_error_mismatch));
                return;
            default:
                break;
        }
        viewModel.completeRegistration(password);
    }

    // -------------------------------------------------------------- results

    private void renderChallenge(@Nullable Result<RegistrationChallenge> result) {
        if (result == null) {
            setBusy(false);
            return;
        }
        setBusy(result.isLoading());

        if (result.isError()) {
            // The server answers "code unknown" and "NIC wrong" identically, so
            // this message is shown as-is rather than pinned to one field.
            toast(result.message == null ? getString(R.string.error_generic) : result.message);
            viewModel.consumeChallengeError();
            return;
        }
        if (result.isSuccess() && result.data != null) {
            boolean resending = viewModel.currentStep() == RegisterViewModel.STEP_OTP;
            viewModel.onChallengeReceived(result.data);
            renderDemoHint(result.data);
            if (resending) {
                toast(R.string.register_resent);
            }
        }
    }

    private void renderVerification(@Nullable Result<String> result) {
        if (result == null) {
            setBusy(false);
            return;
        }
        setBusy(result.isLoading());

        if (result.isError()) {
            binding.inputOtp.setError(result.message == null
                    ? getString(R.string.error_generic) : result.message);
            viewModel.consumeVerifyError();
            return;
        }
        if (result.isSuccess() && result.data != null) {
            viewModel.onVerified(result.data);
        }
    }

    private void renderCompletion(@Nullable Result<Session> result) {
        if (result == null) {
            setBusy(false);
            return;
        }
        setBusy(result.isLoading());

        if (result.isError()) {
            toast(result.message == null ? getString(R.string.error_generic) : result.message);
            viewModel.consumeCompletion();
            return;
        }
        if (result.isSuccess()) {
            viewModel.consumeCompletion();
            toast(R.string.register_done);
            // Straight to Home, and the login screen is cleared off the stack —
            // somebody who has just proved who they are should not have to type
            // it all again.
            nav().navigate(R.id.action_register_to_home);
        }
    }

    // ---------------------------------------------------------------- utils

    @NonNull
    private static String text(@NonNull EditText field) {
        CharSequence value = field.getText();
        return value == null ? "" : value.toString().trim();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
