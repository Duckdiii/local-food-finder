package com.example.cuisine_finder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.UserRepository;
import com.example.cuisine_finder.services.AuthService;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private TextView tvFullName, tvEmail, btnSignOut;
    private AuthService authService;
    private UserRepository userRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        authService = new AuthService();
        userRepository = new UserRepository();

        tvFullName = view.findViewById(R.id.tvFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        btnSignOut = view.findViewById(R.id.btnSignOut);

        loadUserProfile();

        btnSignOut.setOnClickListener(v -> {
            authService.signOut();
            navigateToSignIn();
        });

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser firebaseUser = authService.getCurrentUser();
        if (firebaseUser != null) {
            userRepository.getUser(firebaseUser.getUid()).addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    User user = task.getResult().toObject(User.class);
                    if (user != null) {
                        tvFullName.setText(user.getFullName());
                        tvEmail.setText(user.getEmail());
                    }
                } else {
                    Toast.makeText(getContext(), "Không thể tải thông tin cá nhân", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void navigateToSignIn() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SignInFragment())
                    .commit();
        }
    }
}
