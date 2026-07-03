package com.example.cuisine_finder.activities;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
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
import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CreateRestaurantActivity extends AppCompatActivity {

    private ImageView ivSignboard;
    private View layoutCameraHint;
    private EditText etName, etFoodType, etAddress, etDescription;
    private TextView tvOpenTime, tvCloseTime;
    private TextView btnPriceCheap, btnPriceMedium, btnPriceExpensive;
    private TextView btnSubmit;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private double selectedLat = 10.7769; // default Saigon Center
    private double selectedLon = 106.7009;
    private String openTime = "07:00";
    private String closeTime = "22:00";
    private String selectedPrice = "MEDIUM";
    private boolean isSubmitting = false;

    private UserRepository userRepository;
    private String currentUserId;
    private String restaurantId = null;
    private FoodPlace editingRestaurant = null;
    //
    //  Sử dụng ActivityResultLauncher để mở thư viện ảnh của thiết bị. Khi người dùng chọn một ảnh, URI của ảnh sẽ được lưu lại, hiển thị lên ImageView và ẩn phần gợi ý chụp ảnh.
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivSignboard.setVisibility(View.VISIBLE);
                    layoutCameraHint.setVisibility(View.GONE);
                    Glide.with(this).load(uri).centerCrop().into(ivSignboard);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_restaurant);

        userRepository = new UserRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        ivSignboard = findViewById(R.id.ivSignboard);
        layoutCameraHint = findViewById(R.id.layoutCameraHint);
        etName = findViewById(R.id.etName);
        etFoodType = findViewById(R.id.etFoodType);
        etAddress = findViewById(R.id.etAddress);
        etDescription = findViewById(R.id.etDescription);
        tvOpenTime = findViewById(R.id.tvOpenTime);
        tvCloseTime = findViewById(R.id.tvCloseTime);
        btnPriceCheap = findViewById(R.id.btnPriceCheap);
        btnPriceMedium = findViewById(R.id.btnPriceMedium);
        btnPriceExpensive = findViewById(R.id.btnPriceExpensive);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);

        findViewById(R.id.frameCameraPanel).setOnClickListener(v -> galleryLauncher.launch("image/*"));

        tvOpenTime.setOnClickListener(v -> showTimePicker(true));
        tvCloseTime.setOnClickListener(v -> showTimePicker(false));

        btnPriceCheap.setOnClickListener(v -> togglePrice("CHEAP"));
        btnPriceMedium.setOnClickListener(v -> togglePrice("MEDIUM"));
        btnPriceExpensive.setOnClickListener(v -> togglePrice("EXPENSIVE"));

        btnSubmit.setOnClickListener(v -> submitForm());

        updatePriceButtons();
        detectLocationSilently();

        restaurantId = getIntent().getStringExtra("restaurantId");
        if (restaurantId != null) {
            TextView tvTitle = findViewById(R.id.tvCreateRestaurantTitle);
            TextView tvSubtitle = findViewById(R.id.tvCreateRestaurantSubtitle);
            if (tvTitle != null) tvTitle.setText("Chỉnh sửa quán ăn");
            if (tvSubtitle != null) tvSubtitle.setText("Cập nhật thông tin cửa hàng của bạn");
            btnSubmit.setText("Cập nhật thông tin");
            loadRestaurantDataForEditing();
        }
    }

    private void loadRestaurantDataForEditing() {
        setLoadingState(true);
        FirebaseFirestore.getInstance().collection("food_places").document(restaurantId)
                .get()
                .addOnSuccessListener(doc -> {
                    setLoadingState(false);
                    editingRestaurant = doc.toObject(FoodPlace.class);
                    if (editingRestaurant != null) {
                        editingRestaurant.setId(doc.getId());
                        etName.setText(editingRestaurant.getName());
                        etFoodType.setText(editingRestaurant.getFoodType());
                        etAddress.setText(editingRestaurant.getAddress());
                        etDescription.setText(editingRestaurant.getDescription());
                        
                        openTime = editingRestaurant.getOpenTime();
                        closeTime = editingRestaurant.getCloseTime();
                        tvOpenTime.setText(openTime);
                        tvCloseTime.setText(closeTime);
                        
                        togglePrice(editingRestaurant.getPriceRange());
                        
                        selectedLat = editingRestaurant.getLatitude();
                        selectedLon = editingRestaurant.getLongitude();
                        
                        if (editingRestaurant.getImageUrls() != null && !editingRestaurant.getImageUrls().isEmpty()) {
                            ivSignboard.setVisibility(View.VISIBLE);
                            layoutCameraHint.setVisibility(View.GONE);
                            Glide.with(this).load(editingRestaurant.getImageUrls().get(0)).centerCrop().into(ivSignboard);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    setLoadingState(false);
                    Toast.makeText(this, "Không thể tải thông tin quán: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void detectLocationSilently() {//   Sử dụng LocationManager để lấy vị trí cuối cùng của thiết bị. Nếu có quyền truy cập vị trí, nó sẽ lấy vị trí từ GPS hoặc mạng và lưu lại vào selectedLat và selectedLon. Nếu không có quyền hoặc không có vị trí, nó sẽ giữ giá trị mặc định.
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location loc = null;
                try {
                    loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                } catch (SecurityException ignored) {}
                if (loc != null) {
                    selectedLat = loc.getLatitude();
                    selectedLon = loc.getLongitude();
                }
            }
        } catch (Exception ignored) {}
    }

    private void showTimePicker(boolean isOpen) {// Hiển thị một hộp thoại chọn thời gian. Nếu isOpen là true, nó sẽ hiển thị thời gian mở cửa hiện tại, nếu false, nó sẽ hiển thị thời gian đóng cửa hiện tại. Khi người dùng chọn thời gian, nó sẽ cập nhật openTime hoặc closeTime và hiển thị lên TextView tương ứng.
        String current = isOpen ? openTime : closeTime;
        int hour = 7, minute = 0;
        try {
            String[] parts = current.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception ignored) {}

        new TimePickerDialog(this, (picker, h, m) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            if (isOpen) {
                openTime = time;
                tvOpenTime.setText(time);
            } else {
                closeTime = time;
                tvCloseTime.setText(time);
            }
        }, hour, minute, true).show();
    }

    private void togglePrice(String price) {//  Cập nhật giá trị selectedPrice khi người dùng nhấn vào một trong ba nút giá. Sau đó, nó sẽ gọi updatePriceButtons() để cập nhật giao diện của các nút.
        selectedPrice = price;
        updatePriceButtons();
    }

    private void updatePriceButtons() {//   Cập nhật giao diện của các nút giá dựa trên giá trị selectedPrice. Nếu nút nào được chọn, nó sẽ có nền màu xanh và chữ màu trắng, ngược lại sẽ có nền sáng và chữ màu tối.
        applyPriceButtonState(btnPriceCheap, "CHEAP");
        applyPriceButtonState(btnPriceMedium, "MEDIUM");
        applyPriceButtonState(btnPriceExpensive, "EXPENSIVE");
    }

    private void applyPriceButtonState(TextView btn, String price) {//  Áp dụng trạng thái cho một nút giá cụ thể. Nếu giá trị của nút trùng với selectedPrice, nó sẽ được đánh dấu là đang hoạt động, ngược lại sẽ không hoạt động.
        boolean active = price.equals(selectedPrice);
        btn.setBackgroundResource(active ? R.drawable.bg_filter_chip_active : R.drawable.bg_button_light);
        btn.setTextColor(active ? getColor(R.color.white) : getColor(R.color.text_dark));
    }

    private void submitForm() {//   Kiểm tra xem người dùng đã nhập đầy đủ thông tin bắt buộc chưa. Nếu có ảnh được chọn, nó sẽ tải ảnh lên Firebase Storage trước khi lưu thông tin quán ăn vào Firestore. Nếu không có ảnh, nó sẽ lưu trực tiếp thông tin quán ăn. Sau khi lưu thành công, nó sẽ liên kết quán ăn với người dùng hiện tại và điều hướng đến MerchantDashboardActivity.
        if (isSubmitting) return;

        String name = etName.getText().toString().trim();
        String foodType = etFoodType.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (name.isEmpty() || foodType.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        isSubmitting = true;
        setLoadingState(true);

        if (selectedImageUri != null) {
            uploadImageThenSave(name, foodType, address, desc);
        } else {
            saveRestaurant(name, foodType, address, desc, null);
        }
    }

    private void setLoadingState(boolean loading) {//   Cập nhật trạng thái tải lên. Khi đang tải lên, nó sẽ hiển thị ProgressBar và vô hiệu hóa các trường nhập liệu và nút gửi. Khi không tải lên, nó sẽ ẩn ProgressBar và kích hoạt lại các trường nhập liệu và nút gửi.
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
        etName.setEnabled(!loading);
        etFoodType.setEnabled(!loading);
        etAddress.setEnabled(!loading);
        etDescription.setEnabled(!loading);
    }

    private void uploadImageThenSave(String name, String foodType, String address, String desc) {//  Tải ảnh lên Firebase Storage và sau đó lưu thông tin quán ăn vào Firestore. Nó tạo một đường dẫn duy nhất cho ảnh dựa trên thời gian hiện tại và ID người dùng. Sau khi tải ảnh thành công, nó lấy URL tải xuống của ảnh và gọi saveRestaurant() để lưu thông tin quán ăn cùng với URL ảnh. Nếu có lỗi xảy ra trong quá trình tải ảnh, nó sẽ hiển thị thông báo lỗi và đặt lại trạng thái gửi.
        String path = "community_posts/restaurants/" + System.currentTimeMillis() + "_" + currentUserId + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference(path);

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(url -> saveRestaurant(name, foodType, address, desc, url.toString()))
                .addOnFailureListener(e -> {
                    isSubmitting = false;
                    setLoadingState(false);
                    Toast.makeText(this, "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveRestaurant(String name, String foodType, String address, String desc, String imageUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FoodPlace place = editingRestaurant != null ? editingRestaurant : new FoodPlace();
        place.setName(name);
        place.setFoodType(foodType);
        place.setAddress(address);
        place.setDescription(desc);
        place.setLatitude(selectedLat);
        place.setLongitude(selectedLon);
        place.setOpenTime(openTime);
        place.setCloseTime(closeTime);
        place.setPriceRange(selectedPrice);
        place.setUpdatedAt(System.currentTimeMillis());

        if (imageUrl != null) {
            List<String> list = new ArrayList<>();
            list.add(imageUrl);
            place.setImageUrls(list);
        }

        if (restaurantId != null) {
            db.collection("food_places")
                    .document(restaurantId)
                    .set(place)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cập nhật quán ăn thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        isSubmitting = false;
                        setLoadingState(false);
                        Toast.makeText(this, "Lỗi cập nhật quán ăn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            place.setOpenLate(false);
            place.setCreatedBy(currentUserId);
            place.setStatus("APPROVED");
            place.setCreatedAt(System.currentTimeMillis());
            place.setAverageRating(0.0);
            place.setReviewCount(0);
            place.setFavoriteCount(0);
            place.setExploredCount(0);

            db.collection("food_places")
                    .add(place)
                    .addOnSuccessListener(ref -> {
                        String newId = ref.getId();
                        linkRestaurantToUser(newId);
                    })
                    .addOnFailureListener(e -> {
                        isSubmitting = false;
                        setLoadingState(false);
                        Toast.makeText(this, "Lỗi lưu quán ăn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void linkRestaurantToUser(String restaurantId) {//  Liên kết quán ăn mới tạo với người dùng hiện tại. Nó lấy thông tin người dùng từ Firestore, thêm ID quán ăn vào danh sách các quán ăn mà người dùng quản lý, cập nhật thông tin người dùng và hiển thị thông báo thành công. Nếu có lỗi xảy ra trong quá trình liên kết, nó sẽ hiển thị thông báo lỗi và đặt lại trạng thái gửi.
        if (currentUserId == null) return;

        userRepository.getUser(currentUserId).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            if (user != null) {
                List<String> managed = user.getManagedRestaurantIds();
                if (managed == null) {
                    managed = new ArrayList<>();
                }
                managed.add(restaurantId);
                user.setManagedRestaurantIds(managed);
                user.setUpdatedAt(System.currentTimeMillis());

                userRepository.updateUser(user).addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Tạo quán ăn thành công!", Toast.LENGTH_SHORT).show();
                    // Navigate to MerchantDashboardActivity
                    Intent intent = new Intent(this, MerchantDashboardActivity.class);
                    startActivity(intent);
                    finish();
                }).addOnFailureListener(e -> {
                    isSubmitting = false;
                    setLoadingState(false);
                    Toast.makeText(this, "Lỗi liên kết tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
