package com.example.cuisine_finder.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.CartItem;
import com.example.cuisine_finder.models.Order;
import com.example.cuisine_finder.models.OrderItem;
import com.example.cuisine_finder.models.PaymentMethod;
import com.example.cuisine_finder.repositories.OrderRepository;
import com.example.cuisine_finder.utils.InsetUtils;
import com.google.firebase.auth.FirebaseAuth;
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
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        restaurantId = getIntent().getStringExtra(EXTRA_RESTAURANT_ID);
        restaurantName = getIntent().getStringExtra(EXTRA_RESTAURANT_NAME);
        parseCartItems(getIntent().getStringExtra(EXTRA_CART_ITEMS_JSON));

        initViews();
        bindSummary();
        btnConfirmOrder.setOnClickListener(v -> submitOrder());
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
        tvCheckoutRestaurantName.setText(restaurantName != null ? restaurantName : "Quan an");
        tvCheckoutSummary.setText(getCartItemCount() + " mon trong gio hang");
        tvCheckoutTotal.setText(formatPrice(getCartTotal()));
    }

    private void submitOrder() {
        if (submitting) return;

        String customerId = FirebaseAuth.getInstance().getUid();
        if (customerId == null) {
            Toast.makeText(this, "Vui long dang nhap de dat hang", Toast.LENGTH_SHORT).show();
            return;
        }

        String phone = etCustomerPhone.getText().toString().trim();
        String address = etDeliveryAddress.getText().toString().trim();
        String note = etDeliveryNote.getText().toString().trim();

        if (phone.isEmpty()) {
            etCustomerPhone.setError("Bat buoc");
            return;
        }
        if (address.isEmpty()) {
            etDeliveryAddress.setError("Bat buoc");
            return;
        }
        if (restaurantId == null || restaurantId.isEmpty() || cartItems.isEmpty()) {
            Toast.makeText(this, "Thong tin gio hang khong hop le", Toast.LENGTH_SHORT).show();
            return;
        }

        Order order = buildOrder(customerId, phone, address, note);
        submitting = true;
        btnConfirmOrder.setEnabled(false);
        btnConfirmOrder.setText("Dang tao don...");
        orderRepository.createOrder(order)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Dat hang thanh cong", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ActiveOrderActivity.class);
                    intent.putExtra(ActiveOrderActivity.EXTRA_ORDER_ID, documentReference.getId());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    submitting = false;
                    btnConfirmOrder.setEnabled(true);
                    btnConfirmOrder.setText("Xac nhan dat hang");
                    Toast.makeText(this, "Khong tao duoc don: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                if (item.getFoodItemId() != null && !item.getFoodItemId().isEmpty() && item.getQuantity() > 0) {
                    cartItems.add(item);
                }
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Khong doc duoc gio hang", Toast.LENGTH_SHORT).show();
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
        return df.format((long) price) + "d";
    }
}
