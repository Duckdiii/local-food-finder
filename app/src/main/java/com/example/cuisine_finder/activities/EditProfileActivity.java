package com.example.cuisine_finder.activities;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private EditText etFullName, etEmail, etPhone, etPassword;
    private ProgressBar progressBar;
    private TextView btnSave, btnCancel;

    private UserRepository userRepository;
    private String currentUserId;
    private User currentUser;
    private Uri selectedImageUri;

    // Image Picker Launcher
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivAvatar.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        userRepository = new UserRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        ivAvatar = findViewById(R.id.ivAvatar);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.cardAvatar).setOnClickListener(v -> galleryLauncher.launch("image/*"));

        loadCurrentData();

        btnSave.setOnClickListener(v -> saveChanges());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadCurrentData() {
        if (currentUserId == null) return;
        progressBar.setVisibility(View.VISIBLE);
        userRepository.getUser(currentUserId).addOnSuccessListener(doc -> {
            progressBar.setVisibility(View.GONE);
            currentUser = doc.toObject(User.class);
            if (currentUser != null) {
                etFullName.setText(currentUser.getFullName());
                etEmail.setText(currentUser.getEmail());
                etPhone.setText(currentUser.getPhone());

                if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
                    Glide.with(this)
                            .load(currentUser.getAvatarUrl())
                            .placeholder(R.drawable.bg_image_placeholder)
                            .into(ivAvatar);
                }
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Không thể tải dữ liệu", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveChanges() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable views and show progress
        setLoadingState(true);

        if (selectedImageUri != null) {
            uploadAvatarAndSave(name, email, phone, password);
        } else {
            updateAuthAndFirestore(name, email, phone, password, currentUser.getAvatarUrl());
        }
    }

    private void setLoadingState(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
        btnCancel.setEnabled(!loading);
        etFullName.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPhone.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }

    private void uploadAvatarAndSave(String name, String email, String phone, String password) {
        String path = "avatars/" + currentUserId + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference(path);

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(url -> {
                    updateAuthAndFirestore(name, email, phone, password, url.toString());
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Toast.makeText(this, "Lỗi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateAuthAndFirestore(String name, String email, String phone, String password, String avatarUrl) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            setLoadingState(false);
            Toast.makeText(this, "Tài khoản chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Task<Void>> authTasks = new ArrayList<>();

        // If email changed, update in FirebaseAuth
        boolean emailChanged = !email.equalsIgnoreCase(currentUser.getEmail());
        if (emailChanged) {
            authTasks.add(firebaseUser.updateEmail(email));
        }

        // If password is not empty, update in FirebaseAuth
        boolean passwordChanged = !password.isEmpty();
        if (passwordChanged) {
            authTasks.add(firebaseUser.updatePassword(password));
        }

        if (!authTasks.isEmpty()) {
            Tasks.whenAllComplete(authTasks).addOnCompleteListener(task -> {
                boolean allSuccessful = true;
                StringBuilder errorMessage = new StringBuilder();

                for (Task<Void> subTask : authTasks) {
                    if (!subTask.isSuccessful()) {
                        allSuccessful = false;
                        if (subTask.getException() != null) {
                            errorMessage.append(subTask.getException().getMessage()).append("\n");
                        }
                    }
                }

                if (allSuccessful) {
                    saveUserToFirestore(name, email, phone, avatarUrl);
                } else {
                    setLoadingState(false);
                    Toast.makeText(this, "Lỗi cập nhật bảo mật:\n" + errorMessage.toString() + "Vui lòng đăng nhập lại để cập nhật Email/Mật khẩu.", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            saveUserToFirestore(name, email, phone, avatarUrl);
        }
    }

    private void saveUserToFirestore(String name, String email, String phone, String avatarUrl) {
        currentUser.setFullName(name);
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        currentUser.setAvatarUrl(avatarUrl);
        currentUser.setUpdatedAt(System.currentTimeMillis());

        userRepository.updateUser(currentUser).addOnSuccessListener(aVoid -> {
            setLoadingState(false);
            Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            setLoadingState(false);
            Toast.makeText(this, "Lỗi lưu Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
