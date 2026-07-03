package com.example.cuisine_finder.activities;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import com.example.cuisine_finder.HomeFragment;
import com.example.cuisine_finder.ExploreFragment;
import com.example.cuisine_finder.CommunityFragment;
import com.example.cuisine_finder.SavedFragment;
import com.example.cuisine_finder.ProfileFragment;
import com.example.cuisine_finder.SignInFragment;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.models.UserRole;
import com.example.cuisine_finder.repositories.UserRepository;
import com.example.cuisine_finder.services.AuthService;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private AuthService authService;

    public void updateBottomNavVisibility() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setVisibility(authService.isLoggedIn() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Edge-to-Edge for Notch/Punch hole support
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_main);

        // Handle Insets: root gets top padding for status bar only
        // BottomNavigationView gets bottom padding for navigation bar separately
        View rootView = findViewById(R.id.mainRoot);
        BottomNavigationView bottomNavInsets = findViewById(R.id.bottomNavigation);
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            // Only apply top padding to root (for status bar / notch)
            v.setPadding(0, statusBarHeight, 0, 0);
            // Apply bottom padding to BottomNavigationView so it sits above system nav bar
            bottomNavInsets.setPadding(0, 0, 0, navBarHeight);
            return insets;
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        authService = new AuthService();

        // Seed Hóc Môn data if needed
        com.example.cuisine_finder.utils.HocMonDataSeeder.seedDataIfNeeded(this);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        updateBottomNavVisibility();
        
        // Load default fragment
        if (savedInstanceState == null) {
            if (authService.isLoggedIn()) {
                String currentUserId = authService.getCurrentUser().getUid();
                new UserRepository().getUser(currentUserId).addOnSuccessListener(doc -> {
                    User user = doc.toObject(User.class);
                    if (user != null) {
                        if (UserRole.isMerchant(user.getRole())) {
                            if (user.getManagedRestaurantIds() == null || user.getManagedRestaurantIds().isEmpty()) {
                                Intent intent = new Intent(MainActivity.this, CreateRestaurantActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Intent intent = new Intent(MainActivity.this, MerchantDashboardActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else if (UserRole.isShipper(user.getRole())) {
                            Intent intent = new Intent(MainActivity.this, ShipperOrdersActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragmentContainer, new HomeFragment())
                                    .commit();
                        }
                    } else {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragmentContainer, new HomeFragment())
                                .commit();
                    }
                }).addOnFailureListener(e -> {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new HomeFragment())
                            .commit();
                });
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new SignInFragment())
                        .commit();
            }
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_explore) {
                selectedFragment = new ExploreFragment();
            } else if (id == R.id.nav_community) {
                selectedFragment = new CommunityFragment();
            } else if (id == R.id.nav_saved) {
                selectedFragment = new SavedFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, selectedFragment)
                        .commit();
            }
            return true;
        });
    }
}