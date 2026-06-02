package com.example.cuisine_finder.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.cuisine_finder.HomeFragment;
import com.example.cuisine_finder.ExploreFragment;
import com.example.cuisine_finder.CommunityFragment;
import com.example.cuisine_finder.SavedFragment;
import com.example.cuisine_finder.ProfileFragment;
import com.example.cuisine_finder.SignInFragment;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.services.AuthService;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authService = new AuthService();

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        
        // Load default fragment
        if (savedInstanceState == null) {
            Fragment initialFragment = authService.isLoggedIn() ? new HomeFragment() : new SignInFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, initialFragment)
                    .commit();
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