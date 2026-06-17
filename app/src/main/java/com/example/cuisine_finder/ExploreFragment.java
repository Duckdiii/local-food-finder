package com.example.cuisine_finder;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.preference.PreferenceManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.bonuspack.location.NominatimPOIProvider;
import org.osmdroid.bonuspack.location.POI;
import java.util.ArrayList;
import java.util.Locale;

public class ExploreFragment extends Fragment {

    private MapView mapView;
    private EditText etSearch;
    private TextView tvSheetPlaceName, tvSheetInfo, tvSheetStatus, tvSheetFoodType, tvSheetPrice, tvSheetTime;
    private TextView btnZoomIn, btnZoomOut;
    private Marker searchMarker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_explore, container, false);

        mapView = view.findViewById(R.id.mapView);
        etSearch = view.findViewById(R.id.etSearch);
        tvSheetPlaceName = view.findViewById(R.id.tvSheetPlaceName);
        tvSheetInfo = view.findViewById(R.id.tvSheetInfo);
        tvSheetStatus = view.findViewById(R.id.tvSheetStatus);
        tvSheetFoodType = view.findViewById(R.id.tvSheetFoodType);
        tvSheetPrice = view.findViewById(R.id.tvSheetPrice);
        tvSheetTime = view.findViewById(R.id.tvSheetTime);
        btnZoomIn = view.findViewById(R.id.btnZoomIn);
        btnZoomOut = view.findViewById(R.id.btnZoomOut);

        // Thêm đoạn này trước khi gọi findViewById hoặc setup mapView
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
// BẮT BUỘC CÓ: Khai báo User Agent bằng tên package của app
        Configuration.getInstance().setUserAgentValue(ctx.getPackageName());

// Cấu hình nguồn bản đồ (Thường dùng MAPNIK)
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        setupMap();
        setupSearch();
        setupZoomControls();

        return view;
    }



    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);

        // Center on Saigon (Ho Chi Minh City)
        GeoPoint startPoint = new GeoPoint(10.762622, 106.660172);
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(startPoint);
    }

    private void setupSearch() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearch.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void setupZoomControls() {
        btnZoomIn.setOnClickListener(v -> {
            if (mapView.getController() != null) {
                mapView.getController().zoomIn();
            }
        });

        btnZoomOut.setOnClickListener(v -> {
            if (mapView.getController() != null) {
                mapView.getController().zoomOut();
            }
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) return;

        // Use a background thread for geocoding
        new Thread(() -> {
            try {
                // Use GeocoderNominatim for address search
                org.osmdroid.bonuspack.location.GeocoderNominatim geocoder =
                        new org.osmdroid.bonuspack.location.GeocoderNominatim("CuisineFinder/1.0");

                java.util.List<android.location.Address> addresses =
                        geocoder.getFromLocationName(query, 1);

                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address address = addresses.get(0);
                    GeoPoint location = new GeoPoint(address.getLatitude(), address.getLongitude());

                    getActivity().runOnUiThread(() -> updateMapAndSheet(address.getFeatureName(), address.getAddressLine(0), location));
                } else {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Không tìm thấy địa điểm", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Lỗi tìm kiếm: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void updateMapAndSheet(String name, String description, GeoPoint location) {
        if (location == null) return;

        mapView.getController().animateTo(location);
        mapView.getController().setZoom(17.0);

        if (searchMarker != null) {
            mapView.getOverlays().remove(searchMarker);
        }

        searchMarker = new Marker(mapView);
        searchMarker.setPosition(location);
        searchMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        searchMarker.setTitle(name);
        mapView.getOverlays().add(searchMarker);
        mapView.invalidate();

        // Update Bottom Sheet
        String displayName = name;
        if (displayName == null || displayName.matches("^[0-9].*")) {
            // If name is null or just starts with numbers (like an address), try to use the first part of description
            if (description != null && description.contains(",")) {
                displayName = description.split(",")[0];
            } else {
                displayName = "Địa điểm đã tìm";
            }
        }

        tvSheetPlaceName.setText(displayName);
        tvSheetInfo.setText(String.format(Locale.getDefault(), "★ 4.5 • %s", description != null ? description : "Thông tin chưa cập nhật"));
        tvSheetFoodType.setText("Ẩm thực");
        tvSheetStatus.setText("Đang mở");
        tvSheetPrice.setText("Giá dao động");
        tvSheetTime.setText("08:00 - 22:00");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
}
