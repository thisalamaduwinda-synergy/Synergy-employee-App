package lk.synergypharma.employee.ui.common;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import lk.synergypharma.employee.ServiceLocator;

/**
 * The handful of things every screen needs: the nav controller, the dependency
 * graph, and one consistent way to tell the employee something went wrong.
 */
public abstract class BaseFragment extends Fragment {

    @NonNull
    protected NavController nav() {
        return NavHostFragment.findNavController(this);
    }

    @NonNull
    protected ServiceLocator services() {
        return ServiceLocator.get();
    }

    protected void toast(@NonNull CharSequence message) {
        View root = getView();
        if (root != null) {
            Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
        }
    }

    protected void toast(@StringRes int messageRes) {
        toast(getString(messageRes));
    }
}
