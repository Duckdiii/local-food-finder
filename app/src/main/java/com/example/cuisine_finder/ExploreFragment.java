package com.example.cuisine_finder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cuisine_finder.activities.FoodPlaceDetailActivity;
import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.repositories.PlaceRepository;
import com.google.android.material.card.MaterialCardView;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExploreFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final double NEAR_ME_RADIUS_KM = 1.0;

    // Map + search
    private MapView mapView;
    private EditText etSearch;

    // Bottom sheet views
    private MaterialCardView exploreSheet;
    private LinearLayout layoutResultsList, layoutDetail;
    private TextView tvResultCount, btnCloseSheet;
    private RecyclerView rvResults;
    private TextView btnBackToList;
    private ImageView ivSheetImage;
    private TextView tvSheetPlaceName, tvSheetAddress, tvSheetInfo;
    private TextView tvSheetStatus, tvSheetFoodType, tvSheetPrice, tvSheetTime;
    private TextView btnViewDetail, btnDirect;
    private TextView btnZoomIn, btnZoomOut;
    private TextView btnLocation;

    // Filter chips
    private TextView chipOpenNow, chipNearMe, chipCheap, chipMedium, chipExpensive, chipOpenLate;
    private boolean filterOpenNow = false;
    private boolean filterNearMe = false;
    private boolean filterOpenLate = false;
    private String filterPrice = null; // null | "CHEAP" | "MEDIUM" | "EXPENSIVE"
    private GeoPoint myCurrentLocation = null;
    private boolean hasAutoLoadedNearby = false;
    private boolean pendingApplyNearMeFilter = false;
    public static boolean pendingNearMeFilter = false;

    // Data
    private PlaceRepository placeRepository;
    private final List<FoodPlace> foundPlaces = new ArrayList<>();
    private FoodPlace selectedPlace;
    private ResultsAdapter resultsAdapter;

    // Map overlays
    private RadiusMarkerClusterer markerClusterer;
    private Marker myLocationMarker;
    private Polygon locationCircleOverlay;
    private final Map<String, Marker> markersByPlaceId = new HashMap<>();
    private final Map<String, Integer> markerBaseColors = new HashMap<>();
    private final Map<String, String> markerEmojis = new HashMap<>();
    private String selectedMarkerId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        placeRepository = new PlaceRepository();
        // Cấu hình osmdroid: sử dụng SharedPreferences để quản lý bộ nhớ đệm bản đồ
        Configuration.getInstance().load(requireContext(),
                requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
        // Thiết lập User Agent để tránh bị chặn bởi server cung cấp bản đồ
        // lấy tên gói úng dụng -> com.example.cuisine_finder và gắn vào mỗi yêu cầu tải ảnh bản đồ
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        initViews(view);
        setupMap();
        setupSearch();
        setupZoomControls();
        setupFilterChips();

        return view;
    }

    // ─── View wiring ──────────────────────────────────────────────────────────

    private void initViews(View view) {
        mapView = view.findViewById(R.id.mapView);
        etSearch = view.findViewById(R.id.etSearch);

        exploreSheet  = view.findViewById(R.id.explore_sheet);
        layoutResultsList = view.findViewById(R.id.layoutResultsList);
        layoutDetail  = view.findViewById(R.id.layoutDetail);
        tvResultCount = view.findViewById(R.id.tvResultCount);
        btnCloseSheet = view.findViewById(R.id.btnCloseSheet);
        rvResults     = view.findViewById(R.id.rvResults);
        btnBackToList = view.findViewById(R.id.btnBackToList);
        ivSheetImage  = view.findViewById(R.id.ivSheetImage);
        tvSheetPlaceName = view.findViewById(R.id.tvSheetPlaceName);
        tvSheetAddress   = view.findViewById(R.id.tvSheetAddress);
        tvSheetInfo      = view.findViewById(R.id.tvSheetInfo);
        tvSheetStatus    = view.findViewById(R.id.tvSheetStatus);
        tvSheetFoodType  = view.findViewById(R.id.tvSheetFoodType);
        tvSheetPrice     = view.findViewById(R.id.tvSheetPrice);
        tvSheetTime      = view.findViewById(R.id.tvSheetTime);
        btnViewDetail = view.findViewById(R.id.btnViewDetail);
        btnDirect     = view.findViewById(R.id.btnDirect);
        btnZoomIn     = view.findViewById(R.id.btnZoomIn);
        btnZoomOut    = view.findViewById(R.id.btnZoomOut);
        btnLocation   = view.findViewById(R.id.explore_location);

        chipOpenNow   = view.findViewById(R.id.chipOpenNow);
        chipNearMe    = view.findViewById(R.id.chipNearMe);
        chipCheap     = view.findViewById(R.id.chipCheap);
        chipMedium    = view.findViewById(R.id.chipMedium);
        chipExpensive = view.findViewById(R.id.chipExpensive);
        chipOpenLate  = view.findViewById(R.id.chipOpenLate);

        btnLocation.setOnClickListener(v -> requestCurrentLocation(false));
        btnCloseSheet.setOnClickListener(v -> hideSheet());

        btnBackToList.setOnClickListener(v -> {
            layoutDetail.setVisibility(View.GONE);
            layoutResultsList.setVisibility(View.VISIBLE);
        });

        btnViewDetail.setOnClickListener(v -> {
            if (selectedPlace != null && selectedPlace.getId() != null) {
                Intent intent = new Intent(getActivity(), FoodPlaceDetailActivity.class);
                intent.putExtra(FoodPlaceDetailActivity.EXTRA_PLACE_ID, selectedPlace.getId());
                startActivity(intent);
            }
        });

        btnDirect.setOnClickListener(v -> {
            if (selectedPlace == null) return;
            if (hasValidCoordinates(selectedPlace)) {
                openDirections(selectedPlace);
            } else {
                Toast.makeText(getContext(), "Chưa có tọa độ cho quán này", Toast.LENGTH_SHORT).show();
            }
        });

        resultsAdapter = new ResultsAdapter(foundPlaces, place -> {
            Marker marker = markersByPlaceId.get(place.getId());
            if (marker != null) {
                selectMarker(place, marker);
            } else {
                selectedPlace = place;
                if (hasValidCoordinates(place)) zoomToPlace(place);
                showDetail(place, true);
            }
        });
        rvResults.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvResults.setAdapter(resultsAdapter);
    }

    // ─── Map setup ────────────────────────────────────────────────────────────

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK); // Sử dụng nguồn bản đồ chuẩn Mapnik
        mapView.setMultiTouchControls(true); // Cho phép người dùng phóng to/thu nhỏ bằng cử chỉ chụm/giãn
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER); // Ẩn các nút zoom mặc định của osmdroid, chúng ta sẽ dùng nút zoom tùy chỉnh
        mapView.getController().setZoom(15.0); //   Thiết lập mức zoom mặc định khi mở bản đồ
        mapView.getController().setCenter(new GeoPoint(10.762622, 106.660172)); //  Thiết lập vị trí trung tâm mặc định (TP. Hồ Chí Minh)

        markerClusterer = new RadiusMarkerClusterer(requireContext()); //   Tạo một đối tượng clusterer để gom các ghim gần nhau thành một nhóm
        markerClusterer.setRadius(100);
        mapView.getOverlays().add(markerClusterer);
    }

    private void setupSearch() { // Thiết lập hành vi tìm kiếm khi người dùng nhấn nút "Tìm kiếm" trên bàn phím
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {//   Ẩn bàn phím và thực hiện tìm kiếm
                if (!etSearch.getText().toString().trim().isEmpty()) { //   Chỉ thực hiện tìm kiếm nếu người dùng đã nhập từ khóa
                    refreshMap();
                    hideKeyboard();
                }
                return true;
            }
            return false;
        });
    }

    private void setupZoomControls() {
        btnZoomIn.setOnClickListener(v -> mapView.getController().zoomIn());
        btnZoomOut.setOnClickListener(v -> mapView.getController().zoomOut());
    }

    // ─── Filter chips ─────────────────────────────────────────────────────────

    private void setupFilterChips() {
        chipOpenNow.setOnClickListener(v -> {
            filterOpenNow = !filterOpenNow;
            setChipActive(chipOpenNow, filterOpenNow);
            refreshMap();
        });

        chipNearMe.setOnClickListener(v -> {
            filterNearMe = !filterNearMe;
            if (filterNearMe && myCurrentLocation == null) {
                // Request location first; chip visually stays inactive until we get it
                filterNearMe = false;
                requestCurrentLocation(true);
            } else {
                setChipActive(chipNearMe, filterNearMe);
                refreshMap();
            }
        });

        chipCheap.setOnClickListener(v -> {
            filterPrice = "CHEAP".equals(filterPrice) ? null : "CHEAP";
            setChipActive(chipCheap,     "CHEAP".equals(filterPrice));
            setChipActive(chipMedium,    false);
            setChipActive(chipExpensive, false);
            refreshMap();
        });

        chipMedium.setOnClickListener(v -> {
            filterPrice = "MEDIUM".equals(filterPrice) ? null : "MEDIUM";
            setChipActive(chipMedium,    "MEDIUM".equals(filterPrice));
            setChipActive(chipCheap,     false);
            setChipActive(chipExpensive, false);
            refreshMap();
        });

        chipExpensive.setOnClickListener(v -> {
            filterPrice = "EXPENSIVE".equals(filterPrice) ? null : "EXPENSIVE";
            setChipActive(chipExpensive, "EXPENSIVE".equals(filterPrice));
            setChipActive(chipCheap,     false);
            setChipActive(chipMedium,    false);
            refreshMap();
        });

        chipOpenLate.setOnClickListener(v -> {
            filterOpenLate = !filterOpenLate;
            setChipActive(chipOpenLate, filterOpenLate);
            refreshMap();
        });
    }

    private void setChipActive(TextView chip, boolean active) {
        if (active) {
            chip.setBackgroundResource(R.drawable.bg_filter_chip_active);
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_light);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_gray));
        }
    }

    // ─── Data loading & filtering ─────────────────────────────────────────────

    private void refreshMap() { //  Hàm này thực hiện việc lọc dữ liệu dựa trên các bộ lọc và từ khóa tìm kiếm, sau đó cập nhật bản đồ và danh sách kết quả
        String q = etSearch.getText().toString().trim().toLowerCase();
        boolean hasFilters = filterOpenNow || filterNearMe || filterOpenLate || filterPrice != null || !q.isEmpty();
        if (!hasFilters) { //   Nếu không có bộ lọc nào được áp dụng và từ khóa tìm kiếm trống, chúng ta sẽ xóa tất cả các ghim trên bản đồ và danh sách kết quả, đồng thời ẩn khung thông tin phía dưới
            clearMarkers();
            foundPlaces.clear();
            resultsAdapter.notifyDataSetChanged();
            hideSheet();
            return;
        }

        placeRepository.getCachedApprovedPlaces(new PlaceRepository.OnPlacesLoadedCallback() { //   Lấy danh sách tất cả các quán ăn đã được phê duyệt từ bộ nhớ đệm (cache) và áp dụng các bộ lọc và từ khóa tìm kiếm
            @Override
            public void onLoaded(List<FoodPlace> allPlaces) { //  Khi dữ liệu được tải xong, chúng ta sẽ lọc danh sách quán ăn dựa trên các bộ lọc và từ khóa tìm kiếm, sau đó cập nhật bản đồ và danh sách kết quả
                if (!isAdded()) return;
                List<FoodPlace> results = new ArrayList<>();
                for (FoodPlace place : allPlaces) {
                    if (matchesTextFilter(place, q) && matchesChipFilters(place)) {
                        results.add(place);
                    }
                }
                requireActivity().runOnUiThread(() -> handleResults(results));
            }

            @Override
            public void onError() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(), "Lỗi kết nối, vui lòng thử lại",
                        Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private boolean matchesTextFilter(FoodPlace place, String q) {
        if (q.isEmpty()) return true;
        String name     = place.getName()     != null ? place.getName().toLowerCase()     : "";
        String foodType = place.getFoodType() != null ? place.getFoodType().toLowerCase() : "";
        String address  = place.getAddress()  != null ? place.getAddress().toLowerCase()  : "";
        return name.contains(q) || foodType.contains(q) || address.contains(q);
    }

    private boolean matchesChipFilters(FoodPlace place) {
        if (filterOpenNow && !isCurrentlyOpen(place)) return false;
        if (filterOpenLate && !place.isOpenLate()) return false;
        if (filterPrice != null && !filterPrice.equals(place.getPriceRange())) return false;
        if (filterNearMe) {
            if (myCurrentLocation == null) return false;
            double dist = distanceKm(
                    myCurrentLocation.getLatitude(), myCurrentLocation.getLongitude(),
                    place.getLatitude(), place.getLongitude());
            if (dist > NEAR_ME_RADIUS_KM) return false;
        }
        return true;
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {//   Hàm này tính khoảng cách giữa hai điểm trên bề mặt Trái Đất dựa trên tọa độ vĩ độ và kinh độ của chúng, sử dụng công thức Haversine
        double dLat = Math.toRadians(lat2 - lat1); //   Chuyển đổi độ vĩ độ và kinh độ từ độ sang radian

        double dLon = Math.toRadians(lon2 - lon1); //
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);//
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ─── Results display ──────────────────────────────────────────────────────

    private void handleResults(List<FoodPlace> results) {
        //hàm này có nhiệm vụ quyết định xem bản đồ sẽ trông như thế nào và người dùng sẽ thấy gì ở khung thông tin phía dưới.
        clearMarkers();
        updateLocationCircle();
        foundPlaces.clear();

        if (results.isEmpty()) {
            Toast.makeText(getContext(), "Không tìm thấy quán nào phù hợp", Toast.LENGTH_SHORT).show();
            hideSheet();
            return;
        }

        foundPlaces.addAll(results);

        List<GeoPoint> validPoints = new ArrayList<>();
        for (FoodPlace place : results) {
            if (hasValidCoordinates(place)) {
                GeoPoint point = new GeoPoint(place.getLatitude(), place.getLongitude());
                addMarker(place, point);
                validPoints.add(point); // cập nhật ngay lập tức các ghim vừa cắm lên màn hình
            }
        }

        // Single invalidate after all markers are added to the clusterer
        mapView.invalidate();
        //Nếu chỉ có 1 quán: App tự động "bay" (animate) đến đúng vị trí quán đó và phóng to tối đa để người dùng thấy rõ đường đi.
        //Nếu có nhiều quán: App sử dụng hàm zoomToFitAll. Nó sẽ tự tính toán mức thu nhỏ vừa đủ để tất cả các ghim tìm thấy đều hiện ra trên màn hình, người dùng không cần phải vuốt đi đâu cả.
        if (validPoints.size() == 1) {
            animateCamera(validPoints.get(0), 17.0);
        } else if (validPoints.size() > 1) {
            zoomToFitAll(validPoints);
        }

        resultsAdapter.notifyDataSetChanged();
        //Trường hợp 1 quán: App mở ngay bảng chi tiết của quán đó (hiện ảnh to, thực đơn, đánh giá).
        //Trường hợp nhiều quán: App hiện một danh sách các thẻ (Card) nằm ngang. Người dùng có thể vuốt qua lại để xem lướt các quán trước khi chọn 1 quán cụ thể.
        if (results.size() == 1) {
            selectedPlace = results.get(0);
            Marker m = markersByPlaceId.get(selectedPlace.getId());
            if (m != null) selectMarker(selectedPlace, m);
            else showDetail(selectedPlace, false);
        } else {
            showResultsList(results.size());
        }

        for (FoodPlace place : results) {
            if (!hasValidCoordinates(place) && place.getAddress() != null && !place.getAddress().isEmpty()) {
                geocodeFallback(place);
            }
        }
    }

    // ─── Markers ──────────────────────────────────────────────────────────────

    private void addMarker(FoodPlace place, GeoPoint point) {
        boolean open = isCurrentlyOpen(place);
        int color = ContextCompat.getColor(requireContext(),
                open ? R.color.green_open : R.color.red_close);

        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(place.getName());

        String emoji = getEmojiForCategory(place.getFoodType());
        marker.setIcon(new BitmapDrawable(getResources(), createPinBitmap(color, emoji)));
        marker.setOnMarkerClickListener((m, mv) -> {
            selectMarker(place, m);
            return true;
        });

        if (place.getId() != null) {
            markersByPlaceId.put(place.getId(), marker);
            markerBaseColors.put(place.getId(), color);
            markerEmojis.put(place.getId(), emoji);
        }
        markerClusterer.add(marker);
    }

    private void selectMarker(FoodPlace place, Marker marker) {
        // Reset previous selection to its original status color
        if (selectedMarkerId != null) {
            Marker prev = markersByPlaceId.get(selectedMarkerId);
            Integer prevColor = markerBaseColors.get(selectedMarkerId);
            String prevEmoji = markerEmojis.get(selectedMarkerId);
            if (prev != null && prevColor != null) {
                prev.setIcon(new BitmapDrawable(getResources(), createPinBitmap(prevColor, prevEmoji != null ? prevEmoji : "🍲")));
            }
        }
        // Highlight new selection in orange
        int orange = ContextCompat.getColor(requireContext(), R.color.orange_main);
        String currentEmoji = getEmojiForCategory(place.getFoodType());
        marker.setIcon(new BitmapDrawable(getResources(), createPinBitmap(orange, currentEmoji)));
        selectedMarkerId = place.getId();
        mapView.invalidate();

        selectedPlace = place;
        if (hasValidCoordinates(place)) zoomToPlace(place);
        showDetail(place, foundPlaces.size() > 1);
    }

    private void clearMarkers() {
        if (mapView != null && markerClusterer != null) {
            mapView.getOverlays().remove(markerClusterer);
        }
        markerClusterer = new RadiusMarkerClusterer(requireContext());
        markerClusterer.setRadius(100);
        if (mapView != null) {
            mapView.getOverlays().add(markerClusterer);
        }
        markersByPlaceId.clear();
        markerBaseColors.clear();
        markerEmojis.clear();
        selectedMarkerId = null;

        if (mapView != null && locationCircleOverlay != null) {
            mapView.getOverlays().remove(locationCircleOverlay);
            locationCircleOverlay = null;
        }
        if (mapView != null) mapView.invalidate();
    }

    private void updateLocationCircle() {// Hàm này vẽ một vòng tròn bán kính 1km xung quanh vị trí hiện tại của người dùng nếu bộ lọc "Gần tôi" được bật. Vòng tròn này giúp người dùng dễ dàng nhận biết khu vực tìm kiếm.
        if (mapView == null) return;

        if (locationCircleOverlay != null) { // Nếu đã có vòng tròn cũ, xóa nó trước khi vẽ vòng tròn mới
            mapView.getOverlays().remove(locationCircleOverlay);
            locationCircleOverlay = null;
        }

        if (filterNearMe && myCurrentLocation != null) {//  Nếu bộ lọc "Gần tôi" được bật và chúng ta đã biết vị trí hiện tại của người dùng, vẽ vòng tròn bán kính 1km xung quanh vị trí đó
            locationCircleOverlay = new Polygon(mapView);// Tạo một đối tượng Polygon để vẽ vòng tròn
            ArrayList<GeoPoint> circlePoints = Polygon.pointsAsCircle(myCurrentLocation, NEAR_ME_RADIUS_KM * 1000.0);// Tạo danh sách các điểm tạo thành vòng tròn bán kính 1km xung quanh vị trí hiện tại
            locationCircleOverlay.setPoints(circlePoints);

            //  Set fill and stroke colors with transparency
            locationCircleOverlay.setFillColor(Color.parseColor("#15FF7A30"));
            locationCircleOverlay.setStrokeColor(Color.parseColor("#FFFF7A30"));
            locationCircleOverlay.setStrokeWidth(2.0f);

            // Thêm vòng tròn vào lớp dưới cùng (index 0) để nó nằm dưới các ghim địa điểm
            mapView.getOverlays().add(0, locationCircleOverlay);
        }
        mapView.invalidate();// Yêu cầu bản đồ vẽ lạ để hiển thị vòng tròn mới hoặc xóa vòng tròn cũ
    }

    /** Draws a pin-shaped bitmap: filled circle + triangle tail + centered emoji on a white dot background. */
    private Bitmap createPinBitmap(int color, String emoji) {
        float dp = getResources().getDisplayMetrics().density;
        int r      = (int) (18 * dp);  // circle radius (slightly larger to fit emoji nicely)
        int tailH  = (int) (12 * dp);
        int w = r * 2;
        int h = w + tailH;

        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // Draw pin background color
        paint.setColor(color);
        canvas.drawCircle(r, r, r, paint);

        // Draw tail
        Path tail = new Path();
        tail.moveTo(r - r * 0.4f, r + r * 0.35f);
        tail.lineTo(r + r * 0.4f, r + r * 0.35f);
        tail.lineTo(r, h);
        tail.close();
        canvas.drawPath(tail, paint);

        // Draw inner white circle badge
        paint.setColor(Color.WHITE);
        canvas.drawCircle(r, r, r * 0.65f, paint);

        // Draw centered emoji
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(r * 0.9f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float yOffset = (fm.descent + fm.ascent) / 2;
        canvas.drawText(emoji != null ? emoji : "🍲", r, r - yOffset, textPaint);

        return bm;
    }

    private String getEmojiForCategory(String name) {
        if (name == null) return "🍲";
        String lower = name.toLowerCase(java.util.Locale.getDefault());
        if (lower.contains("coffee") || lower.contains("cà phê") || lower.contains("cafe")) return "☕";
        if (lower.contains("phở") || lower.contains("bún") || lower.contains("hủ tiếu")) return "🍜";
        if (lower.contains("cơm tấm")) return "🍛";
        if (lower.contains("cơm")) return "🍚";
        if (lower.contains("lẩu")) return "🫕";
        if (lower.contains("ốc") || lower.contains("hải sản")) return "🦪";
        if (lower.contains("bánh") || lower.contains("bake")) return "🥐";
        if (lower.contains("nướng") || lower.contains("bbq") || lower.contains("grills")) return "🍖";
        if (lower.contains("kem") || lower.contains("ice cream")) return "🍦";
        if (lower.contains("trà sữa") || lower.contains("milk tea") || lower.contains("bubble")) return "🧋";
        if (lower.contains("trà") || lower.contains("tea")) return "🍵";
        if (lower.contains("pizza")) return "🍕";
        if (lower.contains("burger") || lower.contains("hamburger")) return "🍔";
        if (lower.contains("sushi") || lower.contains("nhật")) return "🍣";
        if (lower.contains("chè")) return "🍧";
        if (lower.contains("gà") || lower.contains("chicken")) return "🍗";
        if (lower.contains("chay") || lower.contains("vegetarian")) return "🥗";
        if (lower.contains("xôi")) return "🍙";
        if (lower.contains("cháo") || lower.contains("mì")) return "🥣";
        if (lower.contains("vặt")) return "🍢";
        return "🍲";
    }

    private void geocodeFallback(FoodPlace place) {
        new Thread(() -> {
            try {
                org.osmdroid.bonuspack.location.GeocoderNominatim geocoder =
                        new org.osmdroid.bonuspack.location.GeocoderNominatim("CuisineFinder/1.0");
                List<android.location.Address> addresses =
                        geocoder.getFromLocationName(place.getAddress() + ", Ho Chi Minh City, Vietnam", 1);

                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address addr = addresses.get(0);
                    place.setLatitude(addr.getLatitude());
                    place.setLongitude(addr.getLongitude());
                    GeoPoint point = new GeoPoint(addr.getLatitude(), addr.getLongitude());

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            boolean stillFound = false;
                            for (FoodPlace fp : foundPlaces) {
                                if (fp.getId() != null && fp.getId().equals(place.getId())) {
                                    stillFound = true;
                                    break;
                                }
                            }
                            if (stillFound) {
                                addMarker(place, point);
                                mapView.invalidate();
                                if (foundPlaces.size() == 1) {
                                    animateCamera(point, 17.0);
                                }
                            }
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    // ─── Sheet UI ─────────────────────────────────────────────────────────────

    private void showResultsList(int count) {// Hàm này hiển thị danh sách các quán ăn tìm thấy trong khung thông tin phía dưới bản đồ. Nó sẽ hiển thị số lượng quán tìm thấy và cập nhật RecyclerView để người dùng có thể vuốt qua lại xem các thẻ quán ăn.
        exploreSheet.setVisibility(View.VISIBLE);
        layoutResultsList.setVisibility(View.VISIBLE);
        layoutDetail.setVisibility(View.GONE);
        tvResultCount.setText(count + " quán tìm thấy");
        resultsAdapter.notifyDataSetChanged();
    }

    private void showDetail(FoodPlace place, boolean showBackButton) {//    Hàm này hiển thị chi tiết thông tin của một quán ăn cụ thể trong khung thông tin phía dưới bản đồ. Nó sẽ hiển thị tên quán, địa chỉ, đánh giá, trạng thái mở cửa, loại món ăn, mức giá, giờ mở cửa và hình ảnh của quán. Nếu có nhiều quán tìm thấy, nút "Quay lại danh sách" sẽ được hiển thị để người dùng có thể quay lại danh sách các quán.
        exploreSheet.setVisibility(View.VISIBLE);
        layoutResultsList.setVisibility(View.GONE);
        layoutDetail.setVisibility(View.VISIBLE);

        btnBackToList.setVisibility(showBackButton ? View.VISIBLE : View.GONE);

        tvSheetPlaceName.setText(place.getName() != null ? place.getName() : "Quán ăn");
        tvSheetAddress.setText(place.getAddress() != null
                ? "📍 " + place.getAddress() : "Chưa có địa chỉ");

        if (place.getReviewCount() > 0) {
            tvSheetInfo.setText(String.format(Locale.getDefault(),
                    "★ %.1f  ·  %d đánh giá", place.getAverageRating(), place.getReviewCount()));
        } else {
            tvSheetInfo.setText("Chưa có đánh giá");
        }

        boolean open = isCurrentlyOpen(place);
        tvSheetStatus.setText(open ? "Mở cửa" : "Đóng cửa");
        tvSheetStatus.setTextColor(requireContext().getColor(open ? R.color.green_open : R.color.red_close));
        tvSheetStatus.setBackgroundResource(open ? R.drawable.bg_chip_green : R.drawable.bg_chip_red);

        if (place.getFoodType() != null && !place.getFoodType().isEmpty()) {
            tvSheetFoodType.setText(place.getFoodType());
            tvSheetFoodType.setVisibility(View.VISIBLE);
        } else {
            tvSheetFoodType.setVisibility(View.GONE);
        }

        if (place.getPriceRange() != null && !place.getPriceRange().isEmpty()) {
            tvSheetPrice.setText(formatPrice(place.getPriceRange()));
            tvSheetPrice.setVisibility(View.VISIBLE);
        } else {
            tvSheetPrice.setVisibility(View.GONE);
        }

        if (place.getOpenTime() != null && place.getCloseTime() != null) {
            tvSheetTime.setText("🕐 " + place.getOpenTime() + " - " + place.getCloseTime());
            tvSheetTime.setVisibility(View.VISIBLE);
        } else {
            tvSheetTime.setVisibility(View.GONE);
        }

        if (place.getImageUrls() != null && !place.getImageUrls().isEmpty()) {
            Glide.with(this).load(place.getImageUrls().get(0))
                    .centerCrop().placeholder(R.drawable.bg_image_placeholder).into(ivSheetImage);
        } else {
            ivSheetImage.setImageResource(R.drawable.bg_image_placeholder);
        }

        selectedPlace = place;
    }

    private void hideSheet() {
        exploreSheet.setVisibility(View.GONE);
        layoutResultsList.setVisibility(View.GONE);
        layoutDetail.setVisibility(View.GONE);
    }

    // ─── Location ─────────────────────────────────────────────────────────────

    private void requestCurrentLocation(boolean applyFilter) { //kiểm tra quyền truy cập vị trí
        //Kiểm tra xem quyền ACCESS_FINE_LOCATION đã được cấp chưa
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            pendingApplyNearMeFilter = applyFilter;
            //Nếu chưa được cấp, ứng dụng sẽ hiện hộp thoại của Android để xin quyền
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        goToMyLocation(applyFilter); // sau khi lấy được vị trí thì map sẽ di chuyển đến vị trí của mình
    }

    private void goToMyLocation(boolean applyFilter) {//   Hàm này sẽ lấy vị trí hiện tại của người dùng và hiển thị nó trên bản đồ. Nếu đã có vị trí gần đây, nó sẽ sử dụng vị trí đó. Nếu không, nó sẽ yêu cầu cập nhật vị trí từ GPS hoặc mạng. Khi nhận được vị trí, nó sẽ gọi onLocationReceived để xử lý.
        if (myCurrentLocation != null) {
            showMyLocationMarker(myCurrentLocation);
            animateCamera(myCurrentLocation, 16.0);
            if (applyFilter) {
                if (!filterNearMe) {
                    filterNearMe = true;
                    setChipActive(chipNearMe, true);
                }
                refreshMap();
            }
            return;
        }

        LocationManager lm = (LocationManager) //dịch vụ hệ thống Android dùng để truy cập GPS và vị trí thiết bị
                requireContext().getSystemService(Context.LOCATION_SERVICE); //yêu cầu Android cung cấp service quản lý vị trí
        if (lm == null) return;
        try {
            Location last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);//    Lấy vị trí cuối cùng được biết đến từ GPS
            if (last == null) last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (last != null) {
                onLocationReceived(new GeoPoint(last.getLatitude(), last.getLongitude()), applyFilter);
            } else {
                Toast.makeText(getContext(), "Đang lấy vị trí...", Toast.LENGTH_SHORT).show();
                LocationListener listener = loc -> { // Nhận tọa độ mới từ cảm biến
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() ->
                            onLocationReceived(new GeoPoint(loc.getLatitude(), loc.getLongitude()), applyFilter));
                };
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener,//    Yêu cầu cập nhật vị trí một lần từ GPS và gọi listener khi có kết quả
                            requireActivity().getMainLooper());
                } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) { // Kiểm tra nếu định vị qua mạng khả dụng
                    lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener,//    Yêu cầu cập nhật vị trí một lần từ Mạng (Wifi/Cell) nếu GPS không khả dụng
                            requireActivity().getMainLooper());
                } else {
                    Toast.makeText(getContext(), "Vui lòng bật GPS", Toast.LENGTH_SHORT).show(); // Thông báo khi tất cả các phương thức định vị đều bị tắt
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(getContext(), "Không có quyền truy cập vị trí", Toast.LENGTH_SHORT).show(); // Xử lý khi người dùng từ chối quyền truy cập
        }
    }

    private void onLocationReceived(GeoPoint point, boolean applyFilter) { // Xử lý sau khi đã lấy được tọa độ vị trí thành công
        myCurrentLocation = point;
        showMyLocationMarker(point);
        animateCamera(point, 16.0); // Di chuyển camera bản đồ đến vị trí hiện tại và phóng to mượt mà
        if (applyFilter) {
            if (!filterNearMe) {
                filterNearMe = true;
                setChipActive(chipNearMe, true);
            }
            refreshMap();
        }
    }

    private void showMyLocationMarker(GeoPoint point) {//   Hàm này hiển thị một ghim đánh dấu vị trí hiện tại của người dùng trên bản đồ. Nếu đã có ghim cũ, nó sẽ xóa ghim đó trước khi thêm ghim mới. Ghim này được đặt ở trung tâm của vị trí hiện tại và có tiêu đề "Vị trí của bạn".
        if (myLocationMarker != null) mapView.getOverlays().remove(myLocationMarker);
        myLocationMarker = new Marker(mapView);
        myLocationMarker.setPosition(point);
        myLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        myLocationMarker.setTitle("Vị trí của bạn");
        mapView.getOverlays().add(myLocationMarker);
        mapView.invalidate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) { //Sau khi hộp thoại xin quyền của hệ thống Android hiển thị và khách hàng chọn Đồng ý hoặc Từ chối, kết quả sẽ được trả về hàm callback
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Khách hàng đồng ý cấp quyền -> Tiến hành lấy vị trí
            goToMyLocation(pendingApplyNearMeFilter);
        } else {
            Toast.makeText(getContext(), "Cần quyền vị trí để dùng tính năng này",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean hasValidCoordinates(FoodPlace place) {
        return place.getLatitude() != 0.0 || place.getLongitude() != 0.0;
    }

    private void animateCamera(GeoPoint point, double zoom, long duration) {
        if (mapView == null) return;
        mapView.getController().animateTo(point, zoom, duration, null);
    }

    private void animateCamera(GeoPoint point, double zoom) {
        animateCamera(point, zoom, 800L);
    }

    private void zoomToPlace(FoodPlace place) {
        animateCamera(new GeoPoint(place.getLatitude(), place.getLongitude()), 17.0);
    }

    private void zoomToFitAll(List<GeoPoint> points) {
        double north = -90, south = 90, east = -180, west = 180;
        for (GeoPoint p : points) {
            if (p.getLatitude()  > north) north = p.getLatitude();
            if (p.getLatitude()  < south) south = p.getLatitude();
            if (p.getLongitude() > east)  east  = p.getLongitude();
            if (p.getLongitude() < west)  west  = p.getLongitude();
        }
        BoundingBox box = new BoundingBox(north, east, south, west);
        mapView.post(() -> mapView.zoomToBoundingBox(box, true, 150));
    }

    private boolean isCurrentlyOpen(FoodPlace place) {
        if (place.getOpenTime() == null || place.getCloseTime() == null) return true;
        try {
            Calendar now = Calendar.getInstance();
            int current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            String[] o = place.getOpenTime().split(":");
            String[] c = place.getCloseTime().split(":");
            int open  = Integer.parseInt(o[0]) * 60 + Integer.parseInt(o[1]);
            int close = Integer.parseInt(c[0]) * 60 + Integer.parseInt(c[1]);
            return current >= open && current <= close;
        } catch (Exception e) {
            return true;
        }
    }

    private String formatPrice(String priceRange) {
        switch (priceRange) {
            case "CHEAP":     return "💰 Bình dân";
            case "MEDIUM":    return "💰 Vừa phải";
            case "EXPENSIVE": return "💰 Cao cấp";
            default:          return priceRange;
        }
    }

    private void openDirections(FoodPlace place) {
        String label = place.getName() != null ? place.getName() : "Quán ăn";
        Uri uri = Uri.parse("geo:" + place.getLatitude() + "," + place.getLongitude()
                + "?q=" + Uri.encode(label));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        if (getActivity() != null
                && intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Uri mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query="
                    + place.getLatitude() + "," + place.getLongitude());
            startActivity(new Intent(Intent.ACTION_VIEW, mapUri));
        }
    }

    private void hideKeyboard() {
        if (getActivity() == null || etSearch == null) return;
        InputMethodManager imm = (InputMethodManager)
                getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    // ─── Horizontal result card adapter ───────────────────────────────────────

    private static class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.VH> {

        interface OnItemClick { void click(FoodPlace place); }

        private final List<FoodPlace> items;
        private final OnItemClick listener;

        ResultsAdapter(List<FoodPlace> items, OnItemClick listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_map_result_card, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            FoodPlace place = items.get(position);
            holder.tvName.setText(place.getName() != null ? place.getName() : "Quán ăn");

            if (place.getReviewCount() > 0) {
                holder.tvRating.setText(String.format(Locale.getDefault(),
                        "★ %.1f  (%d)", place.getAverageRating(), place.getReviewCount()));
            } else {
                holder.tvRating.setText("★ Chưa có đánh giá");
            }

            boolean open = isOpen(place);
            holder.tvStatus.setText(open ? "● Mở cửa" : "● Đóng cửa");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(
                    open ? R.color.green_open : R.color.red_close));

            // Set emoji based on food type
            holder.tvEmoji.setText(getEmojiForFoodType(place.getFoodType()));

            holder.itemView.setOnClickListener(v -> listener.click(place));
        }

        @Override public int getItemCount() { return items.size(); }

        private boolean isOpen(FoodPlace place) {
            if (place.getOpenTime() == null || place.getCloseTime() == null) return true;
            try {
                Calendar now = Calendar.getInstance();
                int cur   = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
                String[] o = place.getOpenTime().split(":");
                String[] c = place.getCloseTime().split(":");
                int open  = Integer.parseInt(o[0]) * 60 + Integer.parseInt(o[1]);
                int close = Integer.parseInt(c[0]) * 60 + Integer.parseInt(c[1]);
                return cur >= open && cur <= close;
            } catch (Exception e) { return true; }
        }

        private String getEmojiForFoodType(String type) {
            if (type == null) return "🍲";
            String t = type.toLowerCase(Locale.getDefault());
            if (t.contains("phở") || t.contains("bún") || t.contains("hủ tiếu")) return "🍜";
            if (t.contains("cơm")) return "🍚";
            if (t.contains("lẩu")) return "🪵";
            if (t.contains("cà phê") || t.contains("coffee") || t.contains("cafe")) return "☕";
            if (t.contains("trà sữa") || t.contains("milk tea")) return "🦹";
            if (t.contains("bánh")) return "🥐";
            if (t.contains("nướng") || t.contains("bbq")) return "🍖";
            if (t.contains("pizza")) return "🍕";
            if (t.contains("burger")) return "🍔";
            if (t.contains("sushi") || t.contains("nhật")) return "🍣";
            if (t.contains("kem")) return "🍦";
            if (t.contains("chè")) return "🍡";
            if (t.contains("gà") || t.contains("chicken")) return "🍗";
            if (t.contains("hải sản") || t.contains("ốc")) return "🦪";
            if (t.contains("chày") || t.contains("vegetarian")) return "🥗";
            if (t.contains("xôi")) return "🍙";
            return "🍲";
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvRating, tvStatus, tvEmoji;
            VH(View v) {
                super(v);
                tvName   = v.findViewById(R.id.tvCardName);
                tvRating = v.findViewById(R.id.tvCardRating);
                tvStatus = v.findViewById(R.id.tvCardStatus);
                tvEmoji  = v.findViewById(R.id.tvCardEmoji);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
            if (pendingNearMeFilter) {
                pendingNearMeFilter = false;
                hasAutoLoadedNearby = true;
                mapView.post(() -> goToMyLocation(true));
            } else if (!hasAutoLoadedNearby) {
                // Run after map finishes its own resume setup
                mapView.post(this::tryAutoLoadNearby);
            }
        }
    }

    /**
     * Nếu đã có quyền vị trí, tự động hiển thị quán trong bán kính 1km ngay khi mở tab
     * mà không cần user gõ gì hoặc nhấn chip.
     */
    private void tryAutoLoadNearby() {
        if (!isAdded()) return;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return; // Chưa có quyền — bỏ qua, chờ user chủ động
        }
        hasAutoLoadedNearby = true;
        goToMyLocation(true); // → onLocationReceived → filterNearMe=true → refreshMap
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDetach();
        }
        markersByPlaceId.clear();
        markerBaseColors.clear();
    }
}
