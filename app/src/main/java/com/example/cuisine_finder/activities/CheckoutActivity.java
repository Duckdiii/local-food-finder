package com.example.cuisine_finder.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.CartItem;
import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.models.OrderItem;
import com.example.cuisine_finder.models.PaymentMethod;
import com.example.cuisine_finder.models.PromoCode;
import com.example.cuisine_finder.repositories.OrderRepository;
import com.example.cuisine_finder.repositories.PromoCodeRepository;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.utils.InsetUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CheckoutActivity extends AppCompatActivity {
    public static final String EXTRA_RESTAURANT_ID = "restaurantId";
    public static final String EXTRA_RESTAURANT_NAME = "restaurantName";
    public static final String EXTRA_CART_ITEMS_JSON = "cartItemsJson";

    private static final double DELIVERY_BASE_FEE = 10000;
    private static final double DELIVERY_FREE_KM = 2.0;
    private static final double DELIVERY_PER_KM_FEE = 3000;
    private static final double DELIVERY_FALLBACK_FEE = 15000;

    private EditText etCustomerPhone, etDeliveryAddress, etDeliveryNote, etPromoCode;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbCod, rbBankTransfer, rbEWallet;
    private TextView tvCheckoutRestaurantName, tvCheckoutSummary, tvCheckoutTotal, btnConfirmOrder;
    private TextView btnApplyPromo, tvPromoResult, tvSubtotalValue, tvDeliveryFeeValue, tvDiscountValue, tvMinOrderWarning;
    private View layoutDiscountRow;
    private TextView tvPhoneError, tvAddressError;
    private android.widget.ProgressBar pbConfirmOrder;
    private com.google.android.material.chip.Chip chipSavedAddress, chipHomeAddress, chipOfficeAddress;
    private User currentUser;

    private final OrderRepository orderRepository = new OrderRepository();
    private final PromoCodeRepository promoCodeRepository = new PromoCodeRepository();
    private final List<CartItem> cartItems = new ArrayList<>();
    private String restaurantId;
    private String restaurantName;
    private boolean submitting;

    private FoodPlace restaurant;
    private double deliveryFee = -1; // -1 = not resolved yet
    private double discountAmount = 0;
    private String appliedPromoCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        InsetUtils.applySystemBars(findViewById(R.id.rootCheckout), true, true);
        findViewById(R.id.btnBack).setOnClickListener(v -> handleBack());

        restaurantId = getIntent().getStringExtra(EXTRA_RESTAURANT_ID);
        restaurantName = getIntent().getStringExtra(EXTRA_RESTAURANT_NAME);
        parseCartItems(getIntent().getStringExtra(EXTRA_CART_ITEMS_JSON));

        initViews();
        bindSummary();
        loadSavedCustomerInfo();
        loadRestaurantAndComputeFee();
        btnConfirmOrder.setOnClickListener(v -> submitOrder());
        btnApplyPromo.setOnClickListener(v -> applyPromoCode());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
    }

    private void handleBack() {
        if (submitting) {
            Toast.makeText(this, "Dang xu ly don hang, vui long doi giay lat", Toast.LENGTH_SHORT).show();
            return;
        }
        finish();
    }

    private void initViews() {
        etCustomerPhone = findViewById(R.id.etCustomerPhone);
        etDeliveryAddress = findViewById(R.id.etDeliveryAddress);
        etDeliveryNote = findViewById(R.id.etDeliveryNote);
        etPromoCode = findViewById(R.id.etPromoCode);
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        rbCod = findViewById(R.id.rbCod);
        rbBankTransfer = findViewById(R.id.rbBankTransfer);
        rbEWallet = findViewById(R.id.rbEWallet);
        tvCheckoutRestaurantName = findViewById(R.id.tvCheckoutRestaurantName);
        tvCheckoutSummary = findViewById(R.id.tvCheckoutSummary);
        tvCheckoutTotal = findViewById(R.id.tvCheckoutTotal);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);
        btnApplyPromo = findViewById(R.id.btnApplyPromo);
        tvPromoResult = findViewById(R.id.tvPromoResult);
        tvSubtotalValue = findViewById(R.id.tvSubtotalValue);
        tvDeliveryFeeValue = findViewById(R.id.tvDeliveryFeeValue);
        tvDiscountValue = findViewById(R.id.tvDiscountValue);
        tvMinOrderWarning = findViewById(R.id.tvMinOrderWarning);
        layoutDiscountRow = findViewById(R.id.layoutDiscountRow);
        
        tvPhoneError = findViewById(R.id.tvPhoneError);
        tvAddressError = findViewById(R.id.tvAddressError);
        pbConfirmOrder = findViewById(R.id.pbConfirmOrder);
        chipSavedAddress = findViewById(R.id.chipSavedAddress);
        chipHomeAddress = findViewById(R.id.chipHomeAddress);
        chipOfficeAddress = findViewById(R.id.chipOfficeAddress);

        setupAddressChips();
        setupTextWatchers();
    }

    private void setupAddressChips() {
        chipSavedAddress.setOnClickListener(v -> {
            if (currentUser != null && currentUser.getAddress() != null && !currentUser.getAddress().isEmpty()) {
                etDeliveryAddress.setText(currentUser.getAddress());
            } else {
                Toast.makeText(this, "Bạn chưa lưu địa chỉ. Hãy nhập địa chỉ bên dưới, hệ thống sẽ tự động lưu khi đặt hàng thành công.", Toast.LENGTH_LONG).show();
            }
        });
        chipHomeAddress.setOnClickListener(v -> {
            etDeliveryAddress.setText("12 Nguyễn Ảnh Thủ, Hóc Môn");
        });
        chipOfficeAddress.setOnClickListener(v -> {
            etDeliveryAddress.setText("Tòa nhà HD Tower, Tô Ký, Quận 12");
        });
    }

    private void setupTextWatchers() {
        etCustomerPhone.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvPhoneError.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        etDeliveryAddress.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvAddressError.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void bindSummary() {
        tvCheckoutRestaurantName.setText(restaurantName != null ? restaurantName : "Quán ăn");
        tvCheckoutSummary.setText(getCartItemCount() + " món trong giỏ hàng");
        updateTotals();
    }

    private void loadSavedCustomerInfo() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        new com.example.cuisine_finder.repositories.UserRepository().getUser(currentUserId)
                .addOnSuccessListener(documentSnapshot -> {
                    currentUser = documentSnapshot.toObject(User.class);
                    if (currentUser != null) {
                        if (currentUser.getPhone() != null && !currentUser.getPhone().isEmpty() && etCustomerPhone.getText().toString().isEmpty()) {
                            etCustomerPhone.setText(currentUser.getPhone());
                        }
                        if (currentUser.getAddress() != null && !currentUser.getAddress().isEmpty() && etDeliveryAddress.getText().toString().isEmpty()) {
                            etDeliveryAddress.setText(currentUser.getAddress());
                        }
                    }
                });
    }

    private void loadRestaurantAndComputeFee() {
        if (restaurantId == null || restaurantId.isEmpty()) {
            applyDeliveryFee(DELIVERY_FALLBACK_FEE);
            return;
        }
        FirebaseFirestore.getInstance().collection("food_places").document(restaurantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        restaurant = documentSnapshot.toObject(FoodPlace.class);
                        if (restaurant != null) {
                            restaurant.setId(documentSnapshot.getId());
                        }
                    }
                    resolveLocationAndComputeFee();
                    updateTotals();
                })
                .addOnFailureListener(e -> {
                    applyDeliveryFee(DELIVERY_FALLBACK_FEE);
                });
    }

    private void resolveLocationAndComputeFee() {
        if (restaurant == null || (restaurant.getLatitude() == 0.0 && restaurant.getLongitude() == 0.0)) {
            applyDeliveryFee(DELIVERY_FALLBACK_FEE);
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            applyDeliveryFee(DELIVERY_FALLBACK_FEE);
            return;
        }
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) {
            applyDeliveryFee(DELIVERY_FALLBACK_FEE);
            return;
        }
        try {
            Location last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last == null) last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (last != null) {
                onDeviceLocationResolved(last);
                return;
            }
            LocationListener listener = this::onDeviceLocationResolved;
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, getMainLooper());
            } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, getMainLooper());
            } else {
                applyDeliveryFee(DELIVERY_FALLBACK_FEE);
            }
        } catch (SecurityException e) {
            applyDeliveryFee(DELIVERY_FALLBACK_FEE);
        }
    }

    private void onDeviceLocationResolved(Location location) {
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                restaurant.getLatitude(), restaurant.getLongitude(), results);
        double distanceKm = results[0] / 1000.0;
        applyDeliveryFee(computeDeliveryFeeForDistance(distanceKm));
    }

    private double computeDeliveryFeeForDistance(double distanceKm) {
        if (distanceKm <= DELIVERY_FREE_KM) return DELIVERY_BASE_FEE;
        double extraKm = Math.ceil(distanceKm - DELIVERY_FREE_KM);
        return DELIVERY_BASE_FEE + extraKm * DELIVERY_PER_KM_FEE;
    }

    private void applyDeliveryFee(double fee) {
        deliveryFee = fee;
        updateTotals();
    }

    private void applyPromoCode() {
        String code = etPromoCode.getText().toString().trim();
        if (code.isEmpty()) {
            discountAmount = 0;
            appliedPromoCode = null;
            tvPromoResult.setVisibility(View.GONE);
            updateTotals();
            return;
        }

        btnApplyPromo.setEnabled(false);
        promoCodeRepository.findByCode(code)
                .addOnSuccessListener(snapshot -> {
                    btnApplyPromo.setEnabled(true);
                    if (snapshot == null || snapshot.isEmpty()) {
                        showPromoError("Mã giảm giá không hợp lệ");
                        return;
                    }
                    PromoCode promo = snapshot.getDocuments().get(0).toObject(PromoCode.class);
                    if (promo == null || promo.isExpired()) {
                        showPromoError("Mã giảm giá đã hết hạn");
                        return;
                    }
                    double subtotal = getCartTotal();
                    if (promo.getMinOrderAmount() > 0 && subtotal < promo.getMinOrderAmount()) {
                        showPromoError("Đơn hàng cần tối thiểu " + formatPrice(promo.getMinOrderAmount()) + " để dùng mã này");
                        return;
                    }
                    discountAmount = promo.computeDiscount(subtotal);
                    appliedPromoCode = code.trim().toUpperCase(Locale.ROOT);
                    tvPromoResult.setVisibility(View.VISIBLE);
                    tvPromoResult.setTextColor(getResources().getColor(R.color.green_open));
                    tvPromoResult.setText("Áp dụng thành công! Giảm " + formatPrice(discountAmount));
                    updateTotals();
                })
                .addOnFailureListener(e -> {
                    btnApplyPromo.setEnabled(true);
                    showPromoError("Không kiểm tra được mã, vui lòng thử lại");
                });
    }

    private void showPromoError(String message) {
        discountAmount = 0;
        appliedPromoCode = null;
        tvPromoResult.setVisibility(View.VISIBLE);
        tvPromoResult.setTextColor(getResources().getColor(R.color.red_close));
        tvPromoResult.setText(message);
        updateTotals();
    }

    private void updateTotals() {
        double subtotal = getCartTotal();
        tvSubtotalValue.setText(formatPrice(subtotal));
        tvDeliveryFeeValue.setText(deliveryFee < 0 ? "Đang tính..." : formatPrice(deliveryFee));

        if (discountAmount > 0) {
            layoutDiscountRow.setVisibility(View.VISIBLE);
            tvDiscountValue.setText("-" + formatPrice(discountAmount));
        } else {
            layoutDiscountRow.setVisibility(View.GONE);
        }

        double effectiveFee = deliveryFee < 0 ? 0 : deliveryFee;
        double total = Math.max(0, subtotal + effectiveFee - discountAmount);
        tvCheckoutTotal.setText(formatPrice(total));

        refreshMinOrderState(subtotal);
    }

    private void refreshMinOrderState(double subtotal) {
        double minOrder = restaurant != null ? restaurant.getMinOrderAmount() : 0;
        if (minOrder > 0 && subtotal < minOrder) {
            tvMinOrderWarning.setVisibility(View.VISIBLE);
            tvMinOrderWarning.setText("Đơn tối thiểu " + formatPrice(minOrder) + ". Vui lòng thêm món vào giỏ hàng.");
            btnConfirmOrder.setEnabled(false);
            btnConfirmOrder.setAlpha(0.5f);
        } else {
            tvMinOrderWarning.setVisibility(View.GONE);
            btnConfirmOrder.setEnabled(true);
            btnConfirmOrder.setAlpha(1f);
        }
    }

    private void submitOrder() {
        if (submitting) return;

        String customerId = FirebaseAuth.getInstance().getUid();
        if (customerId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String phone = etCustomerPhone.getText().toString().trim();
        String address = etDeliveryAddress.getText().toString().trim();
        String note = etDeliveryNote.getText().toString().trim();

        if (phone.isEmpty()) {
            tvPhoneError.setText("Số điện thoại không được để trống");
            tvPhoneError.setVisibility(View.VISIBLE);
            return;
        }
        if (!phone.matches("\\d{10,11}")) {
            tvPhoneError.setText("Số điện thoại phải có 10-11 chữ số");
            tvPhoneError.setVisibility(View.VISIBLE);
            return;
        }
        if (address.isEmpty()) {
            tvAddressError.setText("Địa chỉ giao hàng không được để trống");
            tvAddressError.setVisibility(View.VISIBLE);
            return;
        }
        if (address.length() < 10) {
            tvAddressError.setText("Địa chỉ quá ngắn (tối thiểu 10 ký tự)");
            tvAddressError.setVisibility(View.VISIBLE);
            return;
        }
        if (rgPaymentMethod.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Vui long chon phuong thuc thanh toan", Toast.LENGTH_SHORT).show();
            return;
        }
        if (restaurantId == null || restaurantId.isEmpty() || cartItems.isEmpty()) {
            Toast.makeText(this, "Thông tin giỏ hàng không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (restaurant != null && !restaurant.isOpenForOrders()) {
            Toast.makeText(this, "Quán hiện đã đóng cửa, không thể đặt hàng lúc này", Toast.LENGTH_SHORT).show();
            return;
        }
        double minOrder = restaurant != null ? restaurant.getMinOrderAmount() : 0;
        if (minOrder > 0 && getCartTotal() < minOrder) {
            Toast.makeText(this, "Đơn tối thiểu " + formatPrice(minOrder), Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xac nhan dat hang")
                .setMessage("Ban co chac chan muon dat don hang nay?")
                .setPositiveButton("Dat ngay", (dialog, which) -> performSubmit(customerId, phone, address, note))
                .setNegativeButton("Xem lai", null)
                .show();
    }

    private void performSubmit(String customerId, String phone, String address, String note) {
        Order order = buildOrder(customerId, phone, address, note);
        submitting = true;
        btnConfirmOrder.setEnabled(false);
        btnConfirmOrder.setText("");
        pbConfirmOrder.setVisibility(View.VISIBLE);
        orderRepository.createOrder(order)
                .addOnSuccessListener(documentReference -> {
                    // Save last used phone and address to user profile
                    FirebaseFirestore.getInstance().collection("users").document(customerId)
                            .update("phone", phone, "address", address);

                    // Clear cart after successful order
                    com.example.cuisine_finder.utils.CartManager.getInstance(this).clearCart();
                    
                    submitting = false;
                    pbConfirmOrder.setVisibility(View.GONE);
                    btnConfirmOrder.setEnabled(true);
                    btnConfirmOrder.setText("Đặt hàng");

                    showSuccessDialog(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    submitting = false;
                    btnConfirmOrder.setEnabled(true);
                    btnConfirmOrder.setText("Đặt hàng");
                    pbConfirmOrder.setVisibility(View.GONE);
                    Toast.makeText(this, "Không tạo được đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showSuccessDialog(String orderId) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_order_success, null);
        
        TextView tvSuccessOrderId = view.findViewById(R.id.tvSuccessOrderId);
        tvSuccessOrderId.setText("Mã đơn hàng: #" + orderId);
        
        view.findViewById(R.id.btnTrackOrder).setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(this, ActiveOrderActivity.class);
            intent.putExtra(ActiveOrderActivity.EXTRA_ORDER_ID, orderId);
            startActivity(intent);
            finish();
        });
        
        dialog.setContentView(view);
        dialog.setCancelable(false);
        dialog.show();
    }

    private Order buildOrder(String customerId, String phone, String address, String note) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            orderItems.add(new OrderItem(cartItem));
        }

        double subtotal = getCartTotal();
        double fee = deliveryFee >= 0 ? deliveryFee : DELIVERY_FALLBACK_FEE;
        double discount = Math.min(discountAmount, subtotal);
        double total = Math.max(0, subtotal + fee - discount);

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setCustomerPhone(phone);
        order.setDeliveryAddress(address);
        order.setDeliveryNote(note);
        order.setRestaurantId(restaurantId);
        order.setRestaurantName(restaurantName);
        order.setItems(orderItems);
        order.setSubtotal(subtotal);
        order.setDeliveryFee(fee);
        order.setPromoCode(appliedPromoCode);
        order.setDiscountAmount(discount);
        order.setTotalAmount(total);
        order.setPaymentMethod(getSelectedPaymentMethod());
        return order;
    }

    private String getSelectedPaymentMethod() {
        int checkedId = rgPaymentMethod.getCheckedRadioButtonId();
        if (checkedId == rbBankTransfer.getId()) return PaymentMethod.BANK_TRANSFER;
        if (checkedId == rbEWallet.getId()) return PaymentMethod.E_WALLET;
        return PaymentMethod.COD;
    }

    private void parseCartItems(String json) {
        if (json == null || json.isEmpty()) return;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                CartItem item = new CartItem();
                item.setFoodItemId(object.optString("foodItemId"));
                item.setName(object.optString("name"));
                item.setPrice(object.optDouble("price"));
                item.setQuantity(object.optInt("quantity"));
                item.setImageUrl(object.optString("imageUrl", null));
                item.setNote(object.optString("note", null));
                if (item.getFoodItemId() != null && !item.getFoodItemId().isEmpty()
                        && item.getQuantity() > 0 && item.getPrice() > 0) {
                    cartItems.add(item);
                }
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Không đọc được giỏ hàng", Toast.LENGTH_SHORT).show();
        }
    }

    private int getCartItemCount() {
        int count = 0;
        for (CartItem item : cartItems) {
            count += item.getQuantity();
        }
        return count;
    }

    private double getCartTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getSubtotal();
        }
        return total;
    }

    private String formatPrice(double price) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format((long) price) + "đ";
    }
}
