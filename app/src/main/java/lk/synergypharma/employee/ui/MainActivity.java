package lk.synergypharma.employee.ui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import lk.synergypharma.employee.R;
import lk.synergypharma.employee.ServiceLocator;
import lk.synergypharma.employee.databinding.ActivityMainBinding;
import lk.synergypharma.employee.util.ViewUtils;

/**
 * The only activity.
 *
 * <p>Everything else is a fragment inside one navigation graph, which is what
 * keeps the bottom bar from being rebuilt on every screen and makes the
 * supervisor destinations a matter of showing a menu item rather than launching
 * a different app.
 */
public final class MainActivity extends AppCompatActivity {

    /** Destinations that keep the bottom bar on screen. */
    private static final Set<Integer> TAB_DESTINATIONS = new HashSet<>(Arrays.asList(
            R.id.homeFragment,
            R.id.attendanceFragment,
            R.id.leaveFragment,
            R.id.moreFragment));

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Swap the splash window background out before the first frame.
        setTheme(R.style.Theme_Synergy);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host);
        if (host == null) {
            throw new IllegalStateException("NavHostFragment missing from activity_main");
        }
        navController = host.getNavController();

        startAtTheRightScreen();

        // targetSdk 35+ is edge to edge, so the tab bar has to hold itself clear
        // of the gesture bar. Each screen's blue header handles its own top inset.
        ViewUtils.applySystemBarPadding(binding.bottomNav, false, true);

        NavigationUI.setupWithNavController(binding.bottomNav, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean showTabs = TAB_DESTINATIONS.contains(destination.getId());
            binding.bottomNav.setVisibility(showTabs ? View.VISIBLE : View.GONE);
            binding.bottomNavDivider.setVisibility(showTabs ? View.VISIBLE : View.GONE);
        });
    }

    /**
     * A returning employee should never see the login screen flash past, so the
     * start destination is decided here rather than declared in the graph:
     *
     * <ul>
     *   <li>session + biometric unlock on → Login, which immediately asks for
     *       the fingerprint;</li>
     *   <li>session, no biometric → straight to Home;</li>
     *   <li>no session → Login.</li>
     * </ul>
     */
    private void startAtTheRightScreen() {
        NavGraph graph = navController.getNavInflater().inflate(R.navigation.nav_graph);
        ServiceLocator services = ServiceLocator.get();
        boolean openDirectly = services.auth().isLoggedIn()
                && !services.auth().isBiometricEnabled();
        graph.setStartDestination(openDirectly ? R.id.homeFragment : R.id.loginFragment);
        navController.setGraph(graph);
    }

    @NonNull
    public NavController navController() {
        return navController;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
