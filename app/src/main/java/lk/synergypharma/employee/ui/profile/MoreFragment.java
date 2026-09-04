package lk.synergypharma.employee.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import lk.synergypharma.employee.BuildConfig;
import lk.synergypharma.employee.R;
import lk.synergypharma.employee.databinding.FragmentMoreBinding;
import lk.synergypharma.employee.databinding.IncludeMoreRowBinding;
import lk.synergypharma.employee.domain.model.Employee;
import lk.synergypharma.employee.ui.common.BaseFragment;
import lk.synergypharma.employee.util.Constants;
import lk.synergypharma.employee.util.LocaleHelper;
import lk.synergypharma.employee.util.Result;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * Screen 14 — profile, the rest of the app, and settings.
 *
 * <p>The Approvals row is the whole of the "manager app" question: it is one row
 * in one list, revealed by the role the server returned at login. Nobody needs a
 * second APK, a second login or a second Play Store listing.
 */
public final class MoreFragment extends BaseFragment {

    private FragmentMoreBinding binding;
    private MoreViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMoreBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(MoreViewModel.class);

        ViewUtils.applySystemBarPadding(binding.header, true, false);

        binding.textVersion.setText(getString(R.string.login_version,
                BuildConfig.VERSION_NAME, getString(R.string.company_name)));

        buildRows();

        binding.rowLogout.setOnClickListener(v -> confirmLogout());

        viewModel.employee().observe(getViewLifecycleOwner(), this::renderEmployee);
        viewModel.logout().observe(getViewLifecycleOwner(), this::renderLogout);
    }

    // ----------------------------------------------------------------- rows

    private void buildRows() {
        row(binding.rowOvertime, R.drawable.ic_clock, R.string.more_overtime,
                v -> nav().navigate(R.id.action_global_overtime));
        row(binding.rowAbsence, R.drawable.ic_medical, R.string.more_absence_reasons,
                v -> nav().navigate(R.id.action_global_absence_reason));

        // Shipping in v1.1 — shown so the roadmap is visible, but honest about it.
        row(binding.rowPayslips, R.drawable.ic_wallet, R.string.more_payslips,
                v -> toast(R.string.more_coming_soon));
        row(binding.rowDocuments, R.drawable.ic_file, R.string.more_documents,
                v -> toast(R.string.more_coming_soon));
        row(binding.rowApprovals, R.drawable.ic_check, R.string.more_approvals,
                v -> toast(R.string.more_coming_soon));

        row(binding.rowLanguage, R.drawable.ic_language, R.string.more_language,
                v -> showLanguageDialog());
        row(binding.rowHelp, R.drawable.ic_phone, R.string.more_help,
                v -> contactHr());

        // Hidden until the server says otherwise.
        ViewUtils.visible(binding.rowApprovals.getRoot(), false);
    }

    private void row(@NonNull IncludeMoreRowBinding row,
                     @DrawableRes int iconRes,
                     @StringRes int labelRes,
                     @NonNull View.OnClickListener onClick) {
        row.icon.setImageResource(iconRes);
        row.label.setText(labelRes);
        row.getRoot().setOnClickListener(onClick);
    }

    // -------------------------------------------------------------- profile

    private void renderEmployee(@Nullable Employee employee) {
        if (employee == null) {
            return;
        }
        binding.textAvatar.setText(employee.initials());
        binding.textName.setText(employee.fullName);
        binding.textMeta.setText(getString(R.string.more_profile_meta,
                employee.designation, employee.epfNumber));

        ViewUtils.statusChip(binding.chipLocation, employee.location,
                R.color.synergy_blue_700, R.color.synergy_blue_50);

        // Role-gated, not app-gated. Same APK, same login, one extra row.
        ViewUtils.visible(binding.rowApprovals.getRoot(), employee.role.canApprove());
    }

    // ------------------------------------------------------------- language

    private void showLanguageDialog() {
        String current = viewModel.languageTag();
        int checked = LocaleHelper.SI.equals(current) ? 1 : 0;

        CharSequence[] options = {
                getString(R.string.language_english),
                getString(R.string.language_sinhala)
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.language_dialog_title)
                .setSingleChoiceItems(options, checked, (dialog, which) -> {
                    dialog.dismiss();
                    // Recreates the activity; every strings.xml lookup swaps over,
                    // which is why no layout in this project has hard-coded text.
                    viewModel.setLanguage(which == 1 ? LocaleHelper.SI : LocaleHelper.EN);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    /** Opens the dialler pre-filled — never places the call itself. */
    private void contactHr() {
        Intent dial = new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + Constants.HR_CONTACT_NUMBER));
        if (dial.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(dial);
        } else {
            toast(R.string.error_generic);
        }
    }

    // --------------------------------------------------------------- logout

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.more_logout_confirm_title)
                .setMessage(R.string.more_logout_confirm_body)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.more_logout, (d, w) -> viewModel.logOut())
                .show();
    }

    private void renderLogout(@Nullable Result<Boolean> result) {
        if (result == null || !result.isSuccess()) {
            return;
        }
        viewModel.consumeLogout();
        // Unwinds the whole back stack — no screen behind this should survive.
        nav().navigate(R.id.action_global_login);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
