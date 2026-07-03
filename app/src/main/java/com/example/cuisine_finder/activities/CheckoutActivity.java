package com.example.cuisine_finder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.CartItem;
import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.models.OrderItem;
import com.example.cuisine_finder.models.PaymentMethod;
import com.example.cuisine_finder.repositories.OrderRepository;
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

    private EditText etCustomerPhone, etDeliveryAddress, etDeliveryNote;
    private RadioGroup rgPaymentMethod;
    private RadioButton rbCod, rbBankTransfer, rbEWallet;
    private TextView tvCheckoutRestaurantName, tvCheckoutSummary, tvCheckoutTotal, btnConfirmOrder;

    private final OrderRepository orderRepository = new OrderRepository();
    private final List<CartItem> cartItems = new ArrayList<>();
    private String restaurantId;
    private String restaurantName;
    private boolean submitting;

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
        btnConfirmOrder.setOnClickListener(v -> submitOrder());

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
        rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        rbCod = findViewById(R.id.rbCod);
        rbBankTransfer = findViewById(R.id.rbBankTransfer);
        rbEWallet = findViewById(R.id.rbEWallet);
        tvCheckoutRestaurantName = findViewById(R.id.tvCheckoutRestaurantName);
        tvCheckoutSummary = findViewById(R.id.tvCheckoutSummary);
        tvCheckoutTotal = findViewById(R.id.tvCheckoutTotal);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);
    }

    private void bindSummary() {
        tvCheckoutRestaurantName.setText(restaurantName != null ? restaurantName : "Quán ăn");
        tvCheckoutSummary.setText(getCartItemCount() + " món trong giỏ hàng");
        tvCheckoutTotal.setText(formatPrice(getCartTotal()));
    }

    private void loadSavedCustomerInfo() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        new com.example.cuisine_finder.repositories.UserRepository().getUser(currentUserId)
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        if (user.getPhone() != null && !user.getPhone().isEmpty() && etCustomerPhone.getText().toString().isEmpty()) {
                            etCustomerPhone.setText(user.getPhone());
                        }
                        if (user.getAddress() != null && !user.getAddress().isEmpty() && etDeliveryAddress.getText().toString().isEmpty()) {
                            etDeliveryAddress.setText(user.getAddress());
                        }
                    }
                });
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
            etCustomerPhone.setError("Bắt buộc");
            return;
        }
        if (!phone.matches("\\d{10,11}")) {
            etCustomerPhone.setError("So dien thoai phai co 10-11 chu so");
            return;
        }
        if (address.isEmpty()) {
            etDeliveryAddress.setError("Bắt buộc");
            return;
        }
        if (address.length() < 10) {
            etDeliveryAddress.setError("Dia chi qua ngan");
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
        btnConfirmOrder.setText("Đang tạo đơn...");
        orderRepository.createOrder(order)
                .addOnSuccessListener(documentReference -> {
                    // Save last used phone and address to user profile
                    FirebaseFirestore.getInstance().collection("users").document(customerId)
                            .update("phone", phone, "address", address);

                    Toast.makeText(this, "Đặt hàng thành công", Toast.LENGTH_SHORT).show();
                    // Clear cart after successful order
                    com.example.cuisine_finder.utils.CartManager.getInstance(this).clearCart();
                    Intent intent = new Intent(this, ActiveOrderActivity.class);
                    intent.putExtra(ActiveOrderActivity.EXTRA_ORDER_ID, documentReference.getId());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    submitting = false;
                    btnConfirmOrder.setEnabled(true);
                    btnConfirmOrder.setText("Xác nhận đặt hàng");
                    Toast.makeText(this, "Không tạo được đơn: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private Order buildOrder(String customerId, String phone, String address, String note) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            orderItems.add(new OrderItem(cartItem));
        }

        Order order = new Order();
        order.setCustomerId(customerId);
        order.setCustomerPhone(phone);
        order.setDeliveryAddress(address);
        order.setDeliveryNote(note);
        order.setRestaurantId(restaurantId);
        order.setRestaurantName(restaurantName);
        order.setItems(orderItems);
        order.setSubtotal(getCartTotal());
        order.setDeliveryFee(0);
        order.setTotalAmount(getCartTotal());
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
