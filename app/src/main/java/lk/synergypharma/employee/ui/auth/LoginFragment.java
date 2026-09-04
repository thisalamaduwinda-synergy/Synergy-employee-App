package lk.synergypharma.employee.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import lk.synergypharma.employee.BuildConfig;
import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.FragmentLoginBinding;
import lk.synergypharma.employee.ui.common.BaseFragment;
import lk.synergypharma.employee.util.Result;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * Screen 01 — sign in with the EPF / employee number.
 *
 * <p>Also the lock screen: when a session already exists but biometric unlock is
 * switched on, this is where the employee is asked for a fingerprint before the
 * app reopens.
 */
public final class LoginFragment extends BaseFragment {

    private FragmentLoginBinding binding;
    private LoginViewModel viewModel;
    /** Stops the prompt firing again after a rotation. */
    private boolean promptShown;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Edge to edge on targetSdk 35+: keep the card clear of the status bar
        // and the version line clear of the gesture bar.
        ViewUtils.applySystemBarPadding(view, true, true);

        binding.textVersion.setText(getString(R.string.login_version,
                BuildConfig.VERSION_NAME, getString(R.string.company_name)));

        // Nobody should retype their EPF number every morning.
        String last = viewModel.lastEmployeeId();
        if (!TextUtils.isEmpty(last)) {
            binding.editEmployeeId.setText(last);
            binding.editPassword.requestFocus();
        }

        clearErrorAsTheyType(binding.editEmployeeId);
        clearErrorAsTheyType(binding.editPassword);

        binding.buttonLogin.setOnClickListener(v -> attemptLogin());
        binding.rowBiometric.setOnClickListener(v -> startBiometric(true));
        binding.textRegister.setOnClickListener(
                v -> nav().navigate(R.id.action_login_to_register));

        viewModel.state().observe(getViewLifecycleOwner(), this::render);

        maybeAutoUnlock(savedInstanceState);
    }

    // ------------------------------------------------------------- password

    private void attemptLogin() {
        String id = text(binding.editEmployeeId);
        String password = text(binding.editPassword);

        binding.inputEmployeeId.setError(null);
        binding.inputPassword.setError(null);

        if (id.isEmpty()) {
            binding.inputEmployeeId.setError(getString(R.string.login_error_id_required));
            return;
        }
        if (password.isEmpty()) {
            binding.inputPassword.setError(getString(R.string.login_error_password_required));
            return;
        }
        viewModel.login(id, password);
    }

    private void render(@Nullable Result<lk.synergypharma.employee.domain.model.Session> result) {
        if (result == null) {
            setBusy(false);
            return;
        }
        switch (result.status) {
            case LOADING:
                setBusy(true);
                break;
            case SUCCESS:
                setBusy(false);
                viewModel.consume();
                offerBiometricThenContinue();
                break;
            case ERROR:
                setBusy(false);
                binding.inputPassword.setError(result.message == null
                        ? getString(R.string.login_error_invalid) : result.message);
                viewModel.consume();
                break;
        }
    }

    private void setBusy(boolean busy) {
        binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.buttonLogin.setEnabled(!busy);
        binding.rowBiometric.setEnabled(!busy);
    }

    // ------------------------------------------------------------ biometric

    /**
     * A locked session goes straight to the prompt — making someone tap a button
     * to be asked for the fingerprint they came to give is pure friction.
     */
    private void maybeAutoUnlock(@Nullable Bundle savedInstanceState) {
        boolean firstCreate = savedInstanceState == null && !promptShown;
        if (firstCreate && viewModel.isLockedSession()
                && BiometricHelper.canAuthenticate(requireContext())) {
            startBiometric(false);
        }
    }

    private void startBiometric(boolean userInitiated) {
        if (!BiometricHelper.canAuthenticate(requireContext())) {
            if (userInitiated) {
                toast(R.string.login_biometric_unavailable);
            }
            return;
        }
        if (!viewModel.isLockedSession()) {
            if (userInitiated) {
                toast(R.string.login_biometric_needs_password);
            }
            return;
        }

        promptShown = true;
        BiometricHelper.prompt(this,
                getString(R.string.login_biometric_title),
                getString(R.string.login_biometric_subtitle),
                getString(R.string.action_cancel),
                new BiometricHelper.Listener() {
                    @Override
                    public void onUnlocked() {
                        goToHome();
                    }

                    @Override
                    public void onFailed(@Nullable String message) {
                        if (message != null) {
                            toast(message);
                        }
                    }
                });
    }

    /**
     * Asked once, right after the first successful password login — the only
     * moment the offer makes sense, because that is when the token exists.
     */
    private void offerBiometricThenContinue() {
        boolean available = BiometricHelper.canAuthenticate(requireContext());
        if (!available || viewModel.isBiometricEnabled()) {
            goToHome();
            return;
        }

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.login_biometric)
                .setMessage(R.string.login_biometric_subtitle)
                .setPositiveButton(R.string.action_ok, (d, w) -> {
                    viewModel.setBiometricEnabled(true);
                    goToHome();
                })
                .setNegativeButton(R.string.action_cancel, (d, w) -> goToHome())
                .setOnCancelListener(d -> goToHome())
                .show();
    }

    private void goToHome() {
        if (binding == null) {
            return;
        }
        nav().navigate(R.id.action_login_to_home);
    }

    // ---------------------------------------------------------------- utils

    @NonNull
    private static String text(@NonNull android.widget.EditText field) {
        CharSequence value = field.getText();
        return value == null ? "" : value.toString().trim();
    }

    private void clearErrorAsTheyType(@NonNull android.widget.EditText field) {
        field.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.inputEmployeeId.setError(null);
                binding.inputPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
