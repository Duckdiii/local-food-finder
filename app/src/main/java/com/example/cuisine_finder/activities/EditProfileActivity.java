package com.example.cuisine_finder.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etFullName;
    private TextView btnSave, btnCancel;
    private UserRepository userRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        userRepository = new UserRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        etFullName = findViewById(R.id.etFullName);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadCurrentData();

        btnSave.setOnClickListener(v -> saveChanges());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void loadCurrentData() {
        if (currentUserId == null) return;
        userRepository.getUser(currentUserId).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                etFullName.setText(user.getFullName());
            }
        });
    }

    private void saveChanges() {
        String newName = etFullName.getText().toString().trim();
        if (newName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show();
            return;
        }

        userRepository.getUser(currentUserId).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                user.setFullName(newName);
                userRepository.updateUser(user).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }
}
