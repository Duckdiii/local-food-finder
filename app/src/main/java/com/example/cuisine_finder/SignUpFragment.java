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
import com.google.firebase.auth.FirebaseUser;

public class SignUpFragment extends Fragment {

    private EditText etFirstName, etLastName, etEmail, etPassword;
    private android.widget.RadioGroup rgRole;
    private TextView btnSignUp, tvSignIn;
    private AuthService authService;
    private UserRepository userRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);

        authService = new AuthService();
        userRepository = new UserRepository();

        etFirstName = view.findViewById(R.id.etFirstName);
        etLastName = view.findViewById(R.id.etLastName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        rgRole = view.findViewById(R.id.rgRole);
        btnSignUp = view.findViewById(R.id.btnSignUp);
        tvSignIn = view.findViewById(R.id.tvSignIn);

        btnSignUp.setOnClickListener(v -> handleSignUp());

        tvSignIn.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, new SignInFragment())
                        .commit();
            }
        });

        return view;
    }

    private void handleSignUp() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email format
        if (!email.matches("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")) {
            Toast.makeText(getContext(), "Email không đúng định dạng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password strength (length >= 6 and contains both letters and digits)
        if (password.length() < 6 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            Toast.makeText(getContext(), "Mật khẩu phải từ 6 ký tự và bao gồm cả chữ và số", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedRole = UserRole.CUSTOMER;
        if (rgRole != null) {
            int checkedId = rgRole.getCheckedRadioButtonId();
            if (checkedId == R.id.rbMerchant) {
                selectedRole = UserRole.MERCHANT;
            } else if (checkedId == R.id.rbShipper) {
                selectedRole = UserRole.SHIPPER;
            }
        }

        final String finalRole = selectedRole;
        authService.signUp(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = authService.getCurrentUser();
                if (firebaseUser != null) {
                    saveUserToFirestore(firebaseUser.getUid(), firstName + " " + lastName, email, password, finalRole);
                }
            } else {
                Toast.makeText(getContext(), "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserToFirestore(String userId, String fullName, String email, String password, String role) {
        User user = new User();
        user.setId(userId);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setActive(true);
        user.setCreatedAt(System.currentTimeMillis());

        userRepository.saveUser(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                navigateToHome(user.getId());
            } else {
                Toast.makeText(getContext(), "Lỗi lưu thông tin: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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
