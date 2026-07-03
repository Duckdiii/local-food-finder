package com.example.cuisine_finder;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.cuisine_finder.models.PlaceSubmission;
import com.example.cuisine_finder.repositories.PlaceRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddRestaurantFragment extends Fragment {

    private ImageView ivSignboard;
    private TextView tvCameraHint;
    private EditText etName, etFoodType, etDescription, etAddress;
    private TextView tvCoordinates, tvOpenTime, tvCloseTime;
    private CheckBox checkOpenLate;
    private TextView btnPriceCheap, btnPriceMedium, btnPriceExpensive;
    private TextView btnSubmit;

    private Uri selectedImageUri;
    private double selectedLat = 0, selectedLon = 0;
    private boolean hasLocation = false;
    private String openTime = "07:00";
    private String closeTime = "23:30";
    private String selectedPrice = null;
    private boolean isSubmitting = false;

    // Permission launchers
    private final ActivityResultLauncher<String> cameraPermLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) launchCameraCapture();
            else toast("Cần quyền camera để chụp ảnh");
        }); // Xử lý yêu cầu quyền truy cập Camera từ người dùng

    private final ActivityResultLauncher<String> locationPermLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) detectLocation();
            else toast("Cần quyền vị trí để tự động điền tọa độ");
        }); // Xử lý yêu cầu quyền truy cập Vị trí để lấy tọa độ tự động

    // Camera: TakePicture writes to a FileProvider URI
    private Uri cameraFileUri;
    private final ActivityResultLauncher<Uri> cameraLauncher =
        registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success && cameraFileUri != null) {
                selectedImageUri = cameraFileUri;
                showSignboardPreview(selectedImageUri);
            }
        });//   Xử lý kết quả sau khi chụp ảnh từ camera và lưu vào URI của FileProvider

    private final ActivityResultLauncher<String> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                showSignboardPreview(uri);
            }
        });//   Xử lý kết quả sau khi chọn ảnh từ thư viện và lưu vào URI được chọn

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_restaurant, container, false);
    }// Inflate the layout for this fragment

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) { // Khởi tạo các thành phần giao diện và thiết lập sự kiện sau khi View đã được tạo
        super.onViewCreated(view, savedInstanceState);
        bindViews(view);
        wireListeners(view);
        requestLocationSilently();
    }

    // ─── View binding ─────────────────────────────────────────────────────────

    private void bindViews(View view) { // Ánh xạ các View từ layout XML vào các biến thành viên trong Java
        ivSignboard       = view.findViewById(R.id.ivSignboard);
        tvCameraHint      = view.findViewById(R.id.tvCameraHint);
        etName            = view.findViewById(R.id.etName);
        etFoodType        = view.findViewById(R.id.etFoodType);
        etDescription     = view.findViewById(R.id.etDescription);
        etAddress         = view.findViewById(R.id.etAddress);
        tvCoordinates     = view.findViewById(R.id.tvCoordinates);
        tvOpenTime        = view.findViewById(R.id.tvOpenTime);
        tvCloseTime       = view.findViewById(R.id.tvCloseTime);
        checkOpenLate     = view.findViewById(R.id.checkOpenLate);
        btnPriceCheap     = view.findViewById(R.id.btnPriceCheap);
        btnPriceMedium    = view.findViewById(R.id.btnPriceMedium);
        btnPriceExpensive = view.findViewById(R.id.btnPriceExpensive);
        btnSubmit         = view.findViewById(R.id.btnSubmit);
    }

    private void wireListeners(View view) { // Thiết lập các sự kiện lắng nghe tương tác của người dùng (Click, Change,...)
        //  Back button → pop back stack
        view.findViewById(R.id.btnBack).setOnClickListener(v ->
            requireActivity().getSupportFragmentManager().popBackStack());

        //  Camera panel click → launch gallery picker
        view.findViewById(R.id.frameCameraPanel).setOnClickListener(v ->
            galleryLauncher.launch("image/*"));

        //  Camera button click → check permission and launch camera
        view.findViewById(R.id.btnCapture).setOnClickListener(v -> {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                cameraPermLauncher.launch(Manifest.permission.CAMERA);
            } else {
                launchCameraCapture();
            }
        });

        //  Gallery button click → launch gallery picker
        view.findViewById(R.id.btnGallery).setOnClickListener(v ->
            galleryLauncher.launch("image/*"));

        //  Detect location button click → request GPS coordinates
        view.findViewById(R.id.btnDetectLocation).setOnClickListener(v ->
            requestLocationExplicit());

        //  Time selection clicks → show time picker dialogs
        tvOpenTime.setOnClickListener(v -> showTimePicker(true));
        tvCloseTime.setOnClickListener(v -> showTimePicker(false));

        //  Price range chips → toggle selection state
        btnPriceCheap.setOnClickListener(v -> togglePrice("CHEAP"));
        btnPriceMedium.setOnClickListener(v -> togglePrice("MEDIUM"));
        btnPriceExpensive.setOnClickListener(v -> togglePrice("EXPENSIVE"));

        //  Submit button click → validate and upload data to Firebase
        btnSubmit.setOnClickListener(v -> submitForm());
    }

    // ─── Camera ───────────────────────────────────────────────────────────────

    private void launchCameraCapture() {//  Kiểm tra quyền truy cập camera và khởi động camera để chụp ảnh, lưu vào FileProvider URI
        try {
            File imageFile = createTempImageFile(); // Chuẩn bị tệp tin tạm để lưu trữ ảnh sắp chụp
            cameraFileUri = FileProvider.getUriForFile( // Tạo URI an toàn từ FileProvider để chia sẻ với ứng dụng camera
                requireContext(),
                requireContext().getPackageName() + ".provider",
                imageFile);
            cameraLauncher.launch(cameraFileUri);// Mở ứng dụng camera để chụp ảnh và lưu vào đường dẫn đã tạo
        } catch (IOException e) {
            toast("Không thể khởi động camera");
        }
    }

    private File createTempImageFile() throws IOException { //  Tạo một tệp tin tạm thời để lưu trữ ảnh chụp từ camera, với tên duy nhất dựa trên timestamp
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("SIGN_" + ts, ".jpg", dir);
    }

    private void showSignboardPreview(Uri uri) {//  Hiển thị ảnh biển hiệu đã chọn hoặc chụp lên ImageView, ẩn hướng dẫn camera và sử dụng Glide để tải ảnh từ URI
        ivSignboard.setVisibility(View.VISIBLE);
        tvCameraHint.setVisibility(View.GONE);
        Glide.with(this).load(uri).centerCrop().into(ivSignboard);
    }

    // ─── Location ─────────────────────────────────────────────────────────────

    private void requestLocationSilently() { // Kiểm tra quyền truy cập vị trí mà không yêu cầu người dùng, nếu đã được cấp thì tự động lấy tọa độ GPS
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            detectLocation();
        }
    }

    private void requestLocationExplicit() { // Yêu cầu vị trí khi người dùng nhấn nút, kiểm tra quyền và yêu cầu cấp quyền nếu cần thiết
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            detectLocation();
        }
    }

    private void detectLocation() {//   Lấy tọa độ GPS hiện tại của thiết bị bằng LocationManager, ưu tiên GPS, sau đó là Network và Passive. Nếu không có vị trí, yêu cầu cập nhật một lần từ Network Provider. Cập nhật giao diện với tọa độ hoặc thông báo lỗi nếu không thể lấy vị trí.
        try {
            LocationManager lm = (LocationManager)
                requireContext().getSystemService(Context.LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

            if (loc != null) {
                applyLocation(loc.getLatitude(), loc.getLongitude());
            } else {
                tvCoordinates.setText("Đang tìm vị trí...");
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, location -> {
                        if (location != null && getActivity() != null) {
                            requireActivity().runOnUiThread(() ->
                                applyLocation(location.getLatitude(), location.getLongitude()));
                        }
                    }, requireActivity().getMainLooper());
                } else {
                    tvCoordinates.setText("Không thể lấy vị trí — hãy bật GPS");
                }
            }
        } catch (SecurityException e) {
            tvCoordinates.setText("Không có quyền truy cập vị trí");
        }
    }

    private void applyLocation(double lat, double lon) {//  Cập nhật tọa độ GPS đã lấy được vào các biến thành viên, đánh dấu rằng đã có vị trí và hiển thị tọa độ trên TextView theo định dạng độ thập phân với 6 chữ số sau dấu phẩy.
        selectedLat = lat;
        selectedLon = lon;
        hasLocation = true;
        tvCoordinates.setText(String.format(Locale.US, "%.6f°N, %.6f°E", lat, lon));
    }

    // ─── Time picker ──────────────────────────────────────────────────────────

    private void showTimePicker(boolean isOpen) {
        String current = isOpen ? openTime : closeTime;
        int hour = 7, minute = 0;
        try {
            String[] parts = current.split(":");
            hour   = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        } catch (Exception ignored) {}

        new TimePickerDialog(requireContext(), (picker, h, m) -> {
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

    // ─── Price range ──────────────────────────────────────────────────────────

    private void togglePrice(String price) {
        selectedPrice = price.equals(selectedPrice) ? null : price;
        updatePriceButtons();
    }

    private void updatePriceButtons() {
        applyPriceButtonState(btnPriceCheap,     "CHEAP");
        applyPriceButtonState(btnPriceMedium,    "MEDIUM");
        applyPriceButtonState(btnPriceExpensive, "EXPENSIVE");
    }

    private void applyPriceButtonState(TextView btn, String price) {
        boolean active = price.equals(selectedPrice);
        btn.setBackgroundResource(active ? R.drawable.bg_filter_chip_active : R.drawable.bg_button_light);
        btn.setTextColor(active
            ? requireContext().getColor(R.color.white)
            : requireContext().getColor(R.color.text_dark));
    }

    // ─── Submit ───────────────────────────────────────────────────────────────

    private void submitForm() {
        if (isSubmitting) return;

        String name = etName.getText().toString().trim();// Lấy tên quán từ EditText và kiểm tra xem có rỗng hay không, nếu rỗng thì hiển thị thông báo và yêu cầu người dùng nhập tên quán
        if (name.isEmpty()) {
            toast("Vui lòng nhập tên quán");
            etName.requestFocus();
            return;
        }

        isSubmitting = true;
        btnSubmit.setEnabled(false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();//   Lấy người dùng hiện tại từ Firebase Authentication để xác định ai đang gửi đề xuất quán ăn. Nếu người dùng chưa đăng nhập, currentUser sẽ là null.
        if (selectedImageUri != null) {
            uploadImageThenSubmit(name, currentUser);
        } else {
            saveSubmission(name, currentUser, null);
        }
    }

    private void uploadImageThenSubmit(String name, @Nullable FirebaseUser user) {//    Tải ảnh biển hiệu lên Firebase Storage và sau đó lưu thông tin đề xuất quán ăn vào Firestore. Nếu tải ảnh thất bại, lưu ảnh vào bộ nhớ trong của thiết bị và sử dụng đường dẫn local để lưu thông tin đề xuất.
        String uid  = user != null ? user.getUid() : "anon";
        String path = "community_posts/place_submissions/" + System.currentTimeMillis() + "_" + uid + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference(path);

        ref.putFile(selectedImageUri)
            .continueWithTask(task -> {
                if (!task.isSuccessful() && task.getException() != null) {
                    throw task.getException();
                }
                return ref.getDownloadUrl();
            })
            .addOnSuccessListener(url -> saveSubmission(name, user, url.toString()))
            .addOnFailureListener(e -> {
                Context context = getContext();
                if (context != null && selectedImageUri != null) {
                    String localUrl = com.example.cuisine_finder.utils.ImageStorageUtils.saveImageToInternalStorage(context, selectedImageUri, "place_submissions");
                    saveSubmission(name, user, localUrl);
                    Toast.makeText(context, "Gửi yêu cầu thành công (sử dụng ảnh local do lỗi kết nối)!", Toast.LENGTH_SHORT).show();
                } else {
                    isSubmitting = false;
                    btnSubmit.setEnabled(true);
                    toast("Lỗi tải ảnh: " + e.getMessage());
                }
            });
    }

    private void saveSubmission(String name, @Nullable FirebaseUser user,
                                @Nullable String imageUrl) {
        PlaceSubmission sub = new PlaceSubmission();
        sub.setName(name);
        sub.setFoodType(etFoodType.getText().toString().trim());
        sub.setDescription(etDescription.getText().toString().trim());
        sub.setAddress(etAddress.getText().toString().trim());
        if (hasLocation) {
            sub.setLatitude(selectedLat);
            sub.setLongitude(selectedLon);
        }
        sub.setOpenTime(openTime);
        sub.setCloseTime(closeTime);
        sub.setOpenLate(checkOpenLate.isChecked());
        sub.setPriceRange(selectedPrice);
        sub.setSignboardImageUrl(imageUrl);
        sub.setStatus("PENDING");
        sub.setCreatedAt(System.currentTimeMillis());
        if (user != null) {
            sub.setSubmittedBy(user.getUid());
            String display = user.getDisplayName();
            sub.setSubmittedByName(display != null && !display.isEmpty()
                ? display : user.getEmail());
        }

        FirebaseFirestore.getInstance()
            .collection("place_submissions")
            .add(sub)
            .addOnSuccessListener(ref -> {
                PlaceRepository.invalidateCache();
                toast("Đã gửi đề xuất! Admin sẽ duyệt sớm.");
                requireActivity().getSupportFragmentManager().popBackStack();
            })
            .addOnFailureListener(e -> {
                isSubmitting = false;
                btnSubmit.setEnabled(true);
                toast("Gửi thất bại, kiểm tra kết nối mạng.");
            });
    }

    private void toast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}
