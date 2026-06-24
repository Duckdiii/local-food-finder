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
    private String selectedMarkerId = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        placeRepository = new PlaceRepository();
        Configuration.getInstance().load(requireContext(),
                requireContext().getSharedPreferences("osmdroid", Context.MODE_PRIVATE));
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

        btnLocation.setOnClickListener(v -> requestCurrentLocation());
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
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new GeoPoint(10.762622, 106.660172));

        markerClusterer = new RadiusMarkerClusterer(requireContext());
        markerClusterer.setRadius(100);
        mapView.getOverlays().add(markerClusterer);
    }

    private void setupSearch() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (!etSearch.getText().toString().trim().isEmpty()) {
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
                requestCurrentLocation();
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

    private void refreshMap() {
        String q = etSearch.getText().toString().trim().toLowerCase();
        boolean hasFilters = filterOpenNow || filterNearMe || filterOpenLate || filterPrice != null || !q.isEmpty();
        if (!hasFilters) {
            clearMarkers();
            foundPlaces.clear();
            resultsAdapter.notifyDataSetChanged();
            hideSheet();
            return;
        }

        placeRepository.getCachedApprovedPlaces(new PlaceRepository.OnPlacesLoadedCallback() {
            @Override
            public void onLoaded(List<FoodPlace> allPlaces) {
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

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ─── Results display ──────────────────────────────────────────────────────

    private void handleResults(List<FoodPlace> results) {
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
                validPoints.add(point);
            }
        }

        // Single invalidate after all markers are added to the clusterer
        mapView.invalidate();

        if (validPoints.size() == 1) {
            mapView.getController().animateTo(validPoints.get(0));
            mapView.getController().setZoom(17.0);
        } else if (validPoints.size() > 1) {
            zoomToFitAll(validPoints);
        }

        resultsAdapter.notifyDataSetChanged();

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
        marker.setIcon(new BitmapDrawable(getResources(), createPinBitmap(color)));
        marker.setOnMarkerClickListener((m, mv) -> {
            selectMarker(place, m);
            return true;
        });

        if (place.getId() != null) {
            markersByPlaceId.put(place.getId(), marker);
            markerBaseColors.put(place.getId(), color);
        }
        markerClusterer.add(marker);
    }

    private void selectMarker(FoodPlace place, Marker marker) {
        // Reset previous selection to its original status color
        if (selectedMarkerId != null) {
            Marker prev = markersByPlaceId.get(selectedMarkerId);
            Integer prevColor = markerBaseColors.get(selectedMarkerId);
            if (prev != null && prevColor != null) {
                prev.setIcon(new BitmapDrawable(getResources(), createPinBitmap(prevColor)));
            }
        }
        // Highlight new selection in orange
        int orange = ContextCompat.getColor(requireContext(), R.color.orange_main);
        marker.setIcon(new BitmapDrawable(getResources(), createPinBitmap(orange)));
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
        selectedMarkerId = null;

        if (mapView != null && locationCircleOverlay != null) {
            mapView.getOverlays().remove(locationCircleOverlay);
            locationCircleOverlay = null;
        }
        if (mapView != null) mapView.invalidate();
    }

    private void updateLocationCircle() {
        if (mapView == null) return;

        if (locationCircleOverlay != null) {
            mapView.getOverlays().remove(locationCircleOverlay);
            locationCircleOverlay = null;
        }

        if (filterNearMe && myCurrentLocation != null) {
            locationCircleOverlay = new Polygon(mapView);
            ArrayList<GeoPoint> circlePoints = Polygon.pointsAsCircle(myCurrentLocation, NEAR_ME_RADIUS_KM * 1000.0);
            locationCircleOverlay.setPoints(circlePoints);

            // Styled as semi-transparent orange matching theme/app aesthetics
            locationCircleOverlay.setFillColor(Color.parseColor("#15FF7A30"));
            locationCircleOverlay.setStrokeColor(Color.parseColor("#FFFF7A30"));
            locationCircleOverlay.setStrokeWidth(2.0f);

            // Add it at the bottom (index 0) so it's under markers
            mapView.getOverlays().add(0, locationCircleOverlay);
        }
        mapView.invalidate();
    }

    /** Draws a pin-shaped bitmap: filled circle + triangle tail + white center dot. */
    private Bitmap createPinBitmap(int color) {
        float dp = getResources().getDisplayMetrics().density;
        int r      = (int) (14 * dp);  // circle radius
        int tailH  = (int) (10 * dp);
        int w = r * 2;
        int h = w + tailH;

        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(color);
        canvas.drawCircle(r, r, r, paint);

        Path tail = new Path();
        tail.moveTo(r - r * 0.4f, r + r * 0.35f);
        tail.lineTo(r + r * 0.4f, r + r * 0.35f);
        tail.lineTo(r, h);
        tail.close();
        canvas.drawPath(tail, paint);

        paint.setColor(Color.WHITE);
        canvas.drawCircle(r, r, r * 0.35f, paint);

        return bm;
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
                                    mapView.getController().animateTo(point);
                                    mapView.getController().setZoom(17.0);
                                }
                            }
                        });
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    // ─── Sheet UI ─────────────────────────────────────────────────────────────

    private void showResultsList(int count) {
        exploreSheet.setVisibility(View.VISIBLE);
        layoutResultsList.setVisibility(View.VISIBLE);
        layoutDetail.setVisibility(View.GONE);
        tvResultCount.setText(count + " quán tìm thấy");
        resultsAdapter.notifyDataSetChanged();
    }

    private void showDetail(FoodPlace place, boolean showBackButton) {
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

    private void requestCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        goToMyLocation();
    }

    private void goToMyLocation() {
        LocationManager lm = (LocationManager)
                requireContext().getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return;
        try {
            Location last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last == null) last = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (last != null) {
                onLocationReceived(new GeoPoint(last.getLatitude(), last.getLongitude()));
            } else {
                Toast.makeText(getContext(), "Đang lấy vị trí...", Toast.LENGTH_SHORT).show();
                LocationListener listener = loc -> {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() ->
                            onLocationReceived(new GeoPoint(loc.getLatitude(), loc.getLongitude())));
                };
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener,
                            requireActivity().getMainLooper());
                } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    lm.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener,
                            requireActivity().getMainLooper());
                } else {
                    Toast.makeText(getContext(), "Vui lòng bật GPS", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(getContext(), "Không có quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
        }
    }

    private void onLocationReceived(GeoPoint point) {
        myCurrentLocation = point;
        showMyLocationMarker(point);
        mapView.getController().animateTo(point);
        mapView.getController().setZoom(16.0);
        if (!filterNearMe) {
            filterNearMe = true;
            setChipActive(chipNearMe, true);
        }
        refreshMap();
    }

    private void showMyLocationMarker(GeoPoint point) {
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
            @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            goToMyLocation();
        } else {
            Toast.makeText(getContext(), "Cần quyền vị trí để dùng tính năng này",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean hasValidCoordinates(FoodPlace place) {
        return place.getLatitude() != 0.0 || place.getLongitude() != 0.0;
    }

    private void zoomToPlace(FoodPlace place) {
        mapView.getController().animateTo(new GeoPoint(place.getLatitude(), place.getLongitude()));
        mapView.getController().setZoom(17.0);
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
                        "★ %.1f", place.getAverageRating()));
            } else {
                holder.tvRating.setText("Chưa có đánh giá");
            }

            boolean open = isOpen(place);
            holder.tvStatus.setText(open ? "● Mở cửa" : "● Đóng cửa");
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(
                    open ? R.color.green_open : R.color.red_close));

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

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvRating, tvStatus;
            VH(View v) {
                super(v);
                tvName   = v.findViewById(R.id.tvCardName);
                tvRating = v.findViewById(R.id.tvCardRating);
                tvStatus = v.findViewById(R.id.tvCardStatus);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
            if (!hasAutoLoadedNearby) {
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
        goToMyLocation(); // → onLocationReceived → filterNearMe=true → refreshMap
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}
