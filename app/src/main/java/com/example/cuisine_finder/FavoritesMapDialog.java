package com.example.cuisine_finder;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.cuisine_finder.activities.FoodPlaceDetailActivity;
import com.example.cuisine_finder.models.Favorite;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavoritesMapDialog extends DialogFragment {

    private MapView mapView;
    private List<Favorite> favorites = new ArrayList<>();

    public void setFavorites(List<Favorite> favorites) {
        this.favorites = favorites;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        View view = inflater.inflate(R.layout.dialog_favorites_map, container, false);

        mapView = view.findViewById(R.id.mapFavorites);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        view.findViewById(R.id.btnMapClose).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btnMapCenterFit).setOnClickListener(v -> zoomToFitAll());

        setupMarkers();
        zoomToFitAll();

        return view;
    }

    private void setupMarkers() {
        if (mapView == null || favorites == null || favorites.isEmpty()) return;

        for (Favorite fav : favorites) {
            if (fav.getLatitude() == 0.0 && fav.getLongitude() == 0.0) continue;

            GeoPoint point = new GeoPoint(fav.getLatitude(), fav.getLongitude());
            Marker marker = new Marker(mapView);
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(fav.getPlaceName());
            marker.setSubDescription(fav.getPlaceAddress());

            int orange = Color.parseColor("#FF7A30");
            String emoji = getEmojiForCategory(fav.getFoodType());
            marker.setIcon(new BitmapDrawable(getResources(), createPinBitmap(orange, emoji)));

            marker.setOnMarkerClickListener((m, mv) -> {
                Intent intent = new Intent(getActivity(), FoodPlaceDetailActivity.class);
                intent.putExtra(FoodPlaceDetailActivity.EXTRA_PLACE_ID, fav.getPlaceId());
                startActivity(intent);
                dismiss();
                return true;
            });

            mapView.getOverlays().add(marker);
        }
        mapView.invalidate();
    }

    private void zoomToFitAll() {
        if (mapView == null || favorites == null || favorites.isEmpty()) return;

        List<GeoPoint> points = new ArrayList<>();
        for (Favorite fav : favorites) {
            if (fav.getLatitude() != 0.0 || fav.getLongitude() != 0.0) {
                points.add(new GeoPoint(fav.getLatitude(), fav.getLongitude()));
            }
        }

        if (points.isEmpty()) {
            mapView.getController().setZoom(13.0);
            mapView.getController().setCenter(new GeoPoint(10.7769, 106.7009));
            return;
        }

        if (points.size() == 1) {
            mapView.getController().setZoom(16.0);
            mapView.getController().setCenter(points.get(0));
            return;
        }

        double north = -90, south = 90, east = -180, west = 180;
        for (GeoPoint p : points) {
            if (p.getLatitude()  > north) north = p.getLatitude();
            if (p.getLatitude()  < south) south = p.getLatitude();
            if (p.getLongitude() > east)  east  = p.getLongitude();
            if (p.getLongitude() < west)  west  = p.getLongitude();
        }
        BoundingBox box = new BoundingBox(north, east, south, west);
        mapView.post(() -> mapView.zoomToBoundingBox(box, true, 120));
    }

    private Bitmap createPinBitmap(int color, String emoji) {
        float dp = getResources().getDisplayMetrics().density;
        int r      = (int) (18 * dp);
        int tailH  = (int) (12 * dp);
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
        canvas.drawCircle(r, r, r * 0.65f, paint);

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
        String lower = name.toLowerCase(Locale.getDefault());
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

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }
}
