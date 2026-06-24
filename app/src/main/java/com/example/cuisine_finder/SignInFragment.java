package com.example.cuisine_finder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.content.Intent;
import com.example.cuisine_finder.activities.CreateRestaurantActivity;
import com.example.cuisine_finder.activities.MerchantDashboardActivity;
import com.example.cuisine_finder.activities.ShipperOrdersActivity;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.models.UserRole;
import com.example.cuisine_finder.repositories.UserRepository;
import com.example.cuisine_finder.services.AuthService;

public class SignInFragment extends Fragment {

    private EditText etEmail, etPassword;
    private TextView btnSignIn, tvSignUp;
    private AuthService authService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_in, container, false);

        authService = new AuthService();

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        btnSignIn = view.findViewById(R.id.btnSignIn);
        tvSignUp = view.findViewById(R.id.tvSignUp);

        btnSignIn.setOnClickListener(v -> handleSignIn());

        tvSignUp.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new SignUpFragment())
                        .commit();
            }
        });

        return view;
    }

    private void handleSignIn() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        authService.signIn(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                if (authService.getCurrentUser() != null) {
                    navigateToHome(authService.getCurrentUser().getUid());
                } else {
                    navigateToHome(null);
                }
            } else {
                Toast.makeText(getContext(), "Đăng nhập thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHome(String userId) {
        if (getActivity() == null) return;
        if (userId == null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new HomeFragment())
                    .commit();
            return;
        }

        new UserRepository().getUser(userId).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null && getActivity() != null) {
                if (UserRole.isMerchant(user.getRole())) {
                    if (user.getManagedRestaurantIds() == null || user.getManagedRestaurantIds().isEmpty()) {
                        Intent intent = new Intent(getActivity(), CreateRestaurantActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    } else {
                        Intent intent = new Intent(getActivity(), MerchantDashboardActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    }
                } else if (UserRole.isShipper(user.getRole())) {
                    Intent intent = new Intent(getActivity(), ShipperOrdersActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                } else {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new HomeFragment())
                            .commit();
                }
            }
        }).addOnFailureListener(e -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new HomeFragment())
                        .commit();
            }
        });
    }
}
