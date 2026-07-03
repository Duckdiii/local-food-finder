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
import com.example.cuisine_finder.models.FoodItem;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;

public class AddDishActivity extends AppCompatActivity {

    private ImageView ivDishImage;
    private View layoutCameraHint;
    private EditText etName, etPrice, etDescription;
    private TextView btnSubmit, btnDelete, tvTitle;
    private ProgressBar progressBar;
    private android.widget.Spinner spinnerCategory;
    private View layoutAvailableStatus;
    private com.google.android.material.switchmaterial.SwitchMaterial switchAvailable;

    private String placeId;
    private String dishId;
    private FoodItem editingDish;
    private Uri selectedImageUri;
    private boolean isSubmitting = false;

    private final String[] categories = {"Món chính", "Khai vị", "Món ăn vặt", "Tráng miệng", "Nước uống", "Lẩu"};

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivDishImage.setVisibility(View.VISIBLE);
                    layoutCameraHint.setVisibility(View.GONE);
                    Glide.with(this).load(uri).centerCrop().into(ivDishImage);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_dish);

        placeId = getIntent().getStringExtra("placeId");
        dishId = getIntent().getStringExtra("dishId");
        if (placeId == null || placeId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin quán ăn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ivDishImage = findViewById(R.id.ivDishImage);
        layoutCameraHint = findViewById(R.id.layoutCameraHint);
        etName = findViewById(R.id.etName);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        etPrice = findViewById(R.id.etPrice);
        etDescription = findViewById(R.id.etDescription);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnDelete = findViewById(R.id.btnDelete);
        tvTitle = findViewById(R.id.tvTitle);
        progressBar = findViewById(R.id.progressBar);
        layoutAvailableStatus = findViewById(R.id.layoutAvailableStatus);
        switchAvailable = findViewById(R.id.switchAvailable);

        // Populate Spinner
        android.widget.ArrayAdapter<String> spinnerAdapter = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(spinnerAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.frameCameraPanel).setOnClickListener(v -> galleryLauncher.launch("image/*"));

        btnSubmit.setOnClickListener(v -> submitForm());
        btnDelete.setOnClickListener(v -> confirmDeleteDish());

        if (dishId != null && !dishId.isEmpty()) {
            setupEditMode();
        }
    }

    private void setupEditMode() {
        tvTitle.setText("Chỉnh sửa món ăn");
        btnSubmit.setText("Lưu thay đổi");
        btnDelete.setVisibility(View.VISIBLE);
        layoutAvailableStatus.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance().collection("food_items").document(dishId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    editingDish = documentSnapshot.toObject(FoodItem.class);
                    if (editingDish != null) {
                        editingDish.setId(documentSnapshot.getId());
                        etName.setText(editingDish.getName());
                        etPrice.setText(String.valueOf((int) editingDish.getPrice()));
                        etDescription.setText(editingDish.getDescription());

                        // Select category in spinner
                        for (int i = 0; i < categories.length; i++) {
                            if (categories[i].equalsIgnoreCase(editingDish.getCategoryName())) {
                                spinnerCategory.setSelection(i);
                                break;
                            }
                        }

                        // Set switch state
                        switchAvailable.setChecked(editingDish.isAvailable());

                        // Display image if available
                        if (editingDish.getImageUrls() != null && !editingDish.getImageUrls().isEmpty()) {
                            String imageUrl = editingDish.getImageUrls().get(0);
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                ivDishImage.setVisibility(View.VISIBLE);
                                layoutCameraHint.setVisibility(View.GONE);
                                Glide.with(this).load(imageUrl).centerCrop().into(ivDishImage);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin món ăn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmDeleteDish() {
        if (dishId == null) return;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xóa món ăn")
                .setMessage("Bạn có chắc chắn muốn xóa món ăn này khỏi thực đơn?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteDish())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteDish() {
        isSubmitting = true;
        setLoadingState(true);
        FirebaseFirestore.getInstance().collection("food_items").document(dishId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Xóa món ăn thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    isSubmitting = false;
                    setLoadingState(false);
                    Toast.makeText(this, "Lỗi khi xóa món ăn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void submitForm() {
        if (isSubmitting) return;

        String name = etName.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String priceStr = etPrice.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (name.isEmpty() || category.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Giá tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        isSubmitting = true;
        setLoadingState(true);

        if (selectedImageUri != null) {
            uploadImageThenSave(name, category, price, desc);
        } else {
            saveDish(name, category, price, desc, null);
        }
    }

    private void setLoadingState(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
        btnDelete.setEnabled(!loading);
        etName.setEnabled(!loading);
        spinnerCategory.setEnabled(!loading);
        etPrice.setEnabled(!loading);
        etDescription.setEnabled(!loading);
    }

    private void uploadImageThenSave(String name, String category, double price, String desc) {
        String path = "community_posts/dishes/" + System.currentTimeMillis() + "_" + placeId + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference(path);

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(url -> saveDish(name, category, price, desc, url.toString()))
                .addOnFailureListener(e -> {
                    if (selectedImageUri != null) {
                        String localUrl = com.example.cuisine_finder.utils.ImageStorageUtils.saveImageToInternalStorage(this, selectedImageUri, "dishes");
                        saveDish(name, category, price, desc, localUrl);
                        Toast.makeText(this, "Thêm món ăn thành công (sử dụng ảnh local do lỗi kết nối)!", Toast.LENGTH_SHORT).show();
                    } else {
                        isSubmitting = false;
                        setLoadingState(false);
                        Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveDish(String name, String category, double price, String desc, String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FoodItem item = editingDish != null ? editingDish : new FoodItem();
        item.setPlaceId(placeId);
        item.setName(name);
        item.setCategoryName(category);
        item.setFoodType(category);
        item.setPrice(price);
        item.setDescription(desc);
        item.setUpdatedAt(System.currentTimeMillis());

        if (editingDish != null) {
            item.setAvailable(switchAvailable.isChecked());
        } else {
            item.setAvailable(true);
            item.setAverageRating(0.0);
            item.setCreatedAt(System.currentTimeMillis());
        }

        if (imageUrl != null) {
            List<String> list = new ArrayList<>();
            list.add(imageUrl);
            item.setImageUrls(list);
        } else if (editingDish != null) {
            item.setImageUrls(editingDish.getImageUrls());
        }

        if (dishId != null) {
            // Edit Mode
            db.collection("food_items").document(dishId)
                    .set(item)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật món ăn thành công!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        isSubmitting = false;
                        setLoadingState(false);
                        Toast.makeText(this, "Lỗi cập nhật món ăn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add Mode
            db.collection("food_items")
                    .add(item)
                    .addOnSuccessListener(ref -> {
                        String newDishId = ref.getId();
                        db.collection("food_items").document(newDishId).update("id", newDishId)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Thêm món ăn thành công!", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    isSubmitting = false;
                                    setLoadingState(false);
                                    Toast.makeText(this, "Lỗi cập nhật ID món ăn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        isSubmitting = false;
                        setLoadingState(false);
                        Toast.makeText(this, "Lỗi lưu món ăn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
