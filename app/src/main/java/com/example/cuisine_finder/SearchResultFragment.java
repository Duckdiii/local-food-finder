package com.example.cuisine_finder;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.models.FoodItem;
import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.repositories.FoodItemRepository;
import com.example.cuisine_finder.repositories.FoodPlaceRepository;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SearchResultFragment extends Fragment {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_RESULT = 1;
    private static final int TYPE_SEPARATOR = 2;
    private static final int CHEAP_MAX_PRICE = 100000;
    private static final int MEDIUM_MAX_PRICE = 300000;

    private String initialQuery = "";
    private String currentQuery = "";
    private EditText etSearchBox;
    private TextView btnFilter;
    private TextView btnClearFilter;
    private TextView tvFilterSummary;
    private TextView tvResultHeadline;
    private TextView tvResultSubline;
    private TextView tvSummaryTotal;
    private TextView tvSummaryFoods;
    private TextView tvSummaryPlaces;
    private RecyclerView rvSearchResults;
    private SearchResultAdapter adapter;
    private FoodItemRepository foodItemRepository;
    private FoodPlaceRepository foodPlaceRepository;
    private boolean dataLoaded;

    private final List<Object> displayList = new ArrayList<>();
    private final List<FoodItem> allFoodItems = new ArrayList<>();
    private final List<FoodPlace> allFoodPlaces = new ArrayList<>();
    private final Map<String, FoodPlace> foodPlaceById = new HashMap<>();
    private final FilterState filterState = new FilterState();

    public static SearchResultFragment newInstance(String query) {
        SearchResultFragment fragment = new SearchResultFragment();
        Bundle args = new Bundle();
        args.putString("query", query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialQuery = getArguments().getString("query", "");
        }
        currentQuery = initialQuery != null ? initialQuery.trim() : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_result, container, false);

        foodItemRepository = new FoodItemRepository();
        foodPlaceRepository = new FoodPlaceRepository();

        etSearchBox = view.findViewById(R.id.etSearchBox);
        btnFilter = view.findViewById(R.id.btnFilter);
        btnClearFilter = view.findViewById(R.id.btnClearFilter);
        tvFilterSummary = view.findViewById(R.id.tvFilterSummary);
        tvResultHeadline = view.findViewById(R.id.tvResultHeadline);
        tvResultSubline = view.findViewById(R.id.tvResultSubline);
        tvSummaryTotal = view.findViewById(R.id.tvSummaryTotal);
        tvSummaryFoods = view.findViewById(R.id.tvSummaryFoods);
        tvSummaryPlaces = view.findViewById(R.id.tvSummaryPlaces);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        etSearchBox.setText(initialQuery);
        setupRecyclerView();
        setupSearch();
        setupFilterActions();
        updateFilterSummary();
        updateResultSummary(0, 0);
        loadSearchData();

        return view;
    }

    private void setupRecyclerView() {
        adapter = new SearchResultAdapter();
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearchBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearchBox.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void setupFilterActions() {
        btnFilter.setOnClickListener(v -> showFilterDialog());
        btnClearFilter.setOnClickListener(v -> {
            filterState.clear();
            updateFilterSummary();
            applySearchAndFilters();
        });
    }

    private void loadSearchData() {
        Tasks.whenAllSuccess(
                        foodPlaceRepository.getApprovedPlaces(),
                        foodItemRepository.getAllFoodItems()
                )
                .addOnSuccessListener(results -> {
                    if (!isAdded()) {
                        return;
                    }

                    QuerySnapshot placesSnapshot = (QuerySnapshot) results.get(0);
                    QuerySnapshot foodItemsSnapshot = (QuerySnapshot) results.get(1);

                    allFoodPlaces.clear();
                    foodPlaceById.clear();
                    for (DocumentSnapshot doc : placesSnapshot.getDocuments()) {
                        FoodPlace place = doc.toObject(FoodPlace.class);
                        if (place != null) {
                            if (TextUtils.isEmpty(place.getId())) {
                                place.setId(doc.getId());
                            }
                            allFoodPlaces.add(place);
                            foodPlaceById.put(place.getId(), place);
                        }
                    }

                    allFoodItems.clear();
                    for (DocumentSnapshot doc : foodItemsSnapshot.getDocuments()) {
                        FoodItem foodItem = doc.toObject(FoodItem.class);
                        if (foodItem != null) {
                            if (TextUtils.isEmpty(foodItem.getId())) {
                                foodItem.setId(doc.getId());
                            }
                            allFoodItems.add(foodItem);
                        }
                    }

                    dataLoaded = true;
                    applySearchAndFilters();
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(getContext(), "Không thể tải dữ liệu tìm kiếm", Toast.LENGTH_SHORT).show();
                });
    }

    private void performSearch(String query) {
        currentQuery = query != null ? query.trim() : "";
        if (!dataLoaded) {
            loadSearchData();
            return;
        }
        applySearchAndFilters();
    }

    private void applySearchAndFilters() {
        displayList.clear();

        String normalizedQuery = normalizeText(currentQuery);
        if (normalizedQuery.isEmpty()) {
            updateResultSummary(0, 0);
            displayList.add(ResultRow.empty(
                    "Nhập từ khóa để tìm món ăn hoặc quán ăn",
                    "Bạn có thể lọc theo loại món, giờ mở, mức giá và rating"
            ));
            adapter.notifyDataSetChanged();
            return;
        }

        List<ResultRow> foodItemRows = buildFoodItemRows(normalizedQuery);
        List<ResultRow> foodPlaceRows = buildFoodPlaceRows(normalizedQuery);

        updateResultSummary(foodItemRows.size(), foodPlaceRows.size());

        if (!foodItemRows.isEmpty()) {
            displayList.add(new SectionHeader("Món ăn", foodItemRows.size(), "Các món khớp với từ khóa của bạn"));
            displayList.addAll(foodItemRows);
        }

        if (!foodItemRows.isEmpty() && !foodPlaceRows.isEmpty()) {
            displayList.add(new Separator());
        }

        if (!foodPlaceRows.isEmpty()) {
            displayList.add(new SectionHeader("Quán ăn", foodPlaceRows.size(), "Những quán phù hợp với bộ lọc hiện tại"));
            displayList.addAll(foodPlaceRows);
        }

        if (displayList.isEmpty()) {
            displayList.add(ResultRow.empty(
                    "Không tìm thấy kết quả phù hợp",
                    emptyResultSubtitle()
            ));
        }

        adapter.notifyDataSetChanged();
    }

    private List<ResultRow> buildFoodItemRows(String normalizedQuery) {
        List<ResultRow> rows = new ArrayList<>();
        for (FoodItem foodItem : allFoodItems) {
            if (!matchesFoodItem(foodItem, normalizedQuery)) {
                continue;
            }
            rows.add(buildFoodItemRow(foodItem));
        }
        sortRows(rows);
        return rows;
    }

    private List<ResultRow> buildFoodPlaceRows(String normalizedQuery) {
        List<ResultRow> rows = new ArrayList<>();
        for (FoodPlace place : allFoodPlaces) {
            if (!matchesFoodPlace(place, normalizedQuery)) {
                continue;
            }
            rows.add(buildFoodPlaceRow(place));
        }
        sortRows(rows);
        return rows;
    }

    private boolean matchesFoodItem(FoodItem foodItem, String normalizedQuery) {
        FoodPlace linkedPlace = getPlaceForItem(foodItem);
        if (foodItem == null || !matchesQuery(foodItem, normalizedQuery)) {
            return false;
        }
        if (!TextUtils.isEmpty(foodItem.getPlaceId()) && linkedPlace == null) {
            return false;
        }
        if (!matchesFoodType(foodItem.getFoodType(), foodItem.getCategoryName(), linkedPlace)) {
            return false;
        }
        if (!matchesMinRating(foodItem.getAverageRating())) {
            return false;
        }
        if (!matchesPrice(foodItem.getPrice())) {
            return false;
        }
        return matchesOpenTime(linkedPlace);
    }

    private boolean matchesFoodPlace(FoodPlace place, String normalizedQuery) {
        if (place == null || !matchesQuery(place, normalizedQuery)) {
            return false;
        }
        if (!matchesFoodType(place.getFoodType(), joinTexts(place.getTags()), place)) {
            return false;
        }
        if (!matchesMinRating(place.getAverageRating())) {
            return false;
        }
        if (!matchesPrice(place.getPriceRange())) {
            return false;
        }
        return matchesOpenTime(place);
    }

    private boolean matchesQuery(FoodItem foodItem, String normalizedQuery) {
        FoodPlace linkedPlace = getPlaceForItem(foodItem);
        return containsNormalized(foodItem.getName(), normalizedQuery)
                || containsNormalized(foodItem.getDescription(), normalizedQuery)
                || containsNormalized(foodItem.getFoodType(), normalizedQuery)
                || containsNormalized(foodItem.getCategoryName(), normalizedQuery)
                || containsNormalized(linkedPlace != null ? linkedPlace.getName() : null, normalizedQuery)
                || containsNormalized(linkedPlace != null ? linkedPlace.getAddress() : null, normalizedQuery);
    }

    private boolean matchesQuery(FoodPlace place, String normalizedQuery) {
        return containsNormalized(place.getName(), normalizedQuery)
                || containsNormalized(place.getDescription(), normalizedQuery)
                || containsNormalized(place.getAddress(), normalizedQuery)
                || containsNormalized(place.getFoodType(), normalizedQuery)
                || containsNormalized(joinTexts(place.getTags()), normalizedQuery);
    }

    private boolean matchesFoodType(String primaryType, String secondaryType, @Nullable FoodPlace linkedPlace) {
        if (TextUtils.isEmpty(filterState.foodType)) {
            return true;
        }
        String normalizedFilter = normalizeText(filterState.foodType);
        return containsNormalized(primaryType, normalizedFilter)
                || containsNormalized(secondaryType, normalizedFilter)
                || containsNormalized(linkedPlace != null ? linkedPlace.getFoodType() : null, normalizedFilter)
                || containsNormalized(linkedPlace != null ? joinTexts(linkedPlace.getTags()) : null, normalizedFilter);
    }

    private boolean matchesMinRating(double rating) {
        return filterState.minRating == null || rating >= filterState.minRating;
    }

    private boolean matchesPrice(double actualPrice) {
        if (filterState.minPrice != null && actualPrice < filterState.minPrice) {
            return false;
        }
        return filterState.maxPrice == null || actualPrice <= filterState.maxPrice;
    }

    private boolean matchesPrice(String priceRange) {
        if (filterState.minPrice == null && filterState.maxPrice == null) {
            return true;
        }

        double bucketMin = getBucketMinPrice(priceRange);
        double bucketMax = getBucketMaxPrice(priceRange);
        if (bucketMin < 0 || bucketMax < 0) {
            return false;
        }

        double selectedMin = filterState.minPrice != null ? filterState.minPrice : 0d;
        double selectedMax = filterState.maxPrice != null ? filterState.maxPrice : Double.MAX_VALUE;
        return bucketMax >= selectedMin && bucketMin <= selectedMax;
    }

    private boolean matchesOpenTime(@Nullable FoodPlace place) {
        if (filterState.openAtMinutes == null) {
            return true;
        }
        if (place == null) {
            return false;
        }

        Integer openMinutes = parseTimeToMinutes(place.getOpenTime());
        Integer closeMinutes = parseTimeToMinutes(place.getCloseTime());
        if (openMinutes == null || closeMinutes == null) {
            return false;
        }

        int filterMinutes = filterState.openAtMinutes;
        if (openMinutes <= closeMinutes) {
            return filterMinutes >= openMinutes && filterMinutes <= closeMinutes;
        }
        return filterMinutes >= openMinutes || filterMinutes <= closeMinutes;
    }

    private ResultRow buildFoodItemRow(FoodItem foodItem) {
        FoodPlace linkedPlace = getPlaceForItem(foodItem);
        return ResultRow.result(
                fallback(foodItem.getName(), "Món ăn"),
                firstNonEmpty(
                        linkedPlace != null ? linkedPlace.getName() : null,
                        foodItem.getDescription(),
                        "Món ăn nổi bật"
                ),
                firstNonEmpty(foodItem.getFoodType(), foodItem.getCategoryName(), "Món ăn"),
                formatCurrency(foodItem.getPrice()),
                formatRating(foodItem.getAverageRating()),
                linkedPlace != null ? formatTimeRange(linkedPlace.getOpenTime(), linkedPlace.getCloseTime()) : null,
                "Món",
                foodItem.getAverageRating()
        );
    }

    private ResultRow buildFoodPlaceRow(FoodPlace place) {
        return ResultRow.result(
                fallback(place.getName(), "Quán ăn"),
                firstNonEmpty(place.getAddress(), place.getDescription(), "Quán ăn phù hợp"),
                firstNonEmpty(place.getFoodType(), "Quán ăn"),
                formatPriceRange(place.getPriceRange()),
                formatRating(place.getAverageRating()),
                formatTimeRange(place.getOpenTime(), place.getCloseTime()),
                "Quán",
                place.getAverageRating()
        );
    }

    private FoodPlace getPlaceForItem(FoodItem foodItem) {
        if (foodItem == null || TextUtils.isEmpty(foodItem.getPlaceId())) {
            return null;
        }
        return foodPlaceById.get(foodItem.getPlaceId());
    }

    private void showFilterDialog() {
        if (!isAdded()) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_search_filter, null, false);
        EditText etFilterFoodType = dialogView.findViewById(R.id.etFilterFoodType);
        TextView tvFilterOpenTime = dialogView.findViewById(R.id.tvFilterOpenTime);
        EditText etFilterMinPrice = dialogView.findViewById(R.id.etFilterMinPrice);
        EditText etFilterMaxPrice = dialogView.findViewById(R.id.etFilterMaxPrice);
        EditText etFilterMinRating = dialogView.findViewById(R.id.etFilterMinRating);

        etFilterFoodType.setText(filterState.foodType);
        tvFilterOpenTime.setText(filterState.openAtMinutes != null ? formatMinutes(filterState.openAtMinutes) : "Tất cả");
        etFilterMinPrice.setText(filterState.minPrice != null ? formatNumber(filterState.minPrice) : "");
        etFilterMaxPrice.setText(filterState.maxPrice != null ? formatNumber(filterState.maxPrice) : "");
        etFilterMinRating.setText(filterState.minRating != null ? formatNumber(filterState.minRating) : "");

        tvFilterOpenTime.setOnClickListener(v -> showTimePicker(tvFilterOpenTime));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Bộ lọc tìm kiếm")
                .setView(dialogView)
                .setNegativeButton("Hủy", null)
                .setNeutralButton("Đặt lại", (dialogInterface, which) -> {
                    filterState.clear();
                    updateFilterSummary();
                    applySearchAndFilters();
                })
                .setPositiveButton("Áp dụng", null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    if (!applyFilterInputs(etFilterFoodType, tvFilterOpenTime, etFilterMinPrice, etFilterMaxPrice, etFilterMinRating)) {
                        return;
                    }
                    dialog.dismiss();
                    updateFilterSummary();
                    applySearchAndFilters();
                })
        );
        dialog.show();
    }

    private void showTimePicker(TextView target) {
        int initialHour = 8;
        int initialMinute = 0;
        if (filterState.openAtMinutes != null) {
            initialHour = filterState.openAtMinutes / 60;
            initialMinute = filterState.openAtMinutes % 60;
        }

        TimePickerDialog pickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> target.setText(formatMinutes(hourOfDay * 60 + minute)),
                initialHour,
                initialMinute,
                true
        );
        pickerDialog.show();
    }

    private boolean applyFilterInputs(
            EditText etFilterFoodType,
            TextView tvFilterOpenTime,
            EditText etFilterMinPrice,
            EditText etFilterMaxPrice,
            EditText etFilterMinRating
    ) {
        Double minPrice = parseDoubleOrNull(etFilterMinPrice.getText().toString());
        Double maxPrice = parseDoubleOrNull(etFilterMaxPrice.getText().toString());
        Double minRating = parseDoubleOrNull(etFilterMinRating.getText().toString());
        Integer openAtMinutes = parseTimeToMinutes("Tất cả".contentEquals(tvFilterOpenTime.getText()) ? null : tvFilterOpenTime.getText().toString());

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            Toast.makeText(getContext(), "Giá từ phải nhỏ hơn giá đến", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (minRating != null && (minRating < 0d || minRating > 5d)) {
            Toast.makeText(getContext(), "Rating phải nằm trong khoảng 0 đến 5", Toast.LENGTH_SHORT).show();
            return false;
        }

        filterState.foodType = cleanInput(etFilterFoodType.getText().toString());
        filterState.openAtMinutes = openAtMinutes;
        filterState.minPrice = minPrice;
        filterState.maxPrice = maxPrice;
        filterState.minRating = minRating;
        return true;
    }

    private void updateFilterSummary() {
        String summary = buildActiveFilterSummary();
        boolean hasFilters = !TextUtils.isEmpty(summary);

        tvFilterSummary.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
        tvFilterSummary.setText(summary);
        btnClearFilter.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
    }

    private void updateResultSummary(int foodCount, int placeCount) {
        int totalCount = foodCount + placeCount;
        String trimmedQuery = currentQuery != null ? currentQuery.trim() : "";

        if (trimmedQuery.isEmpty()) {
            tvResultHeadline.setText("Khám phá theo từ khóa");
            tvResultSubline.setText("Nhập tên món ăn hoặc quán ăn để xem kết quả phù hợp nhất.");
        } else {
            tvResultHeadline.setText("Kết quả cho \"" + trimmedQuery + "\"");
            if (totalCount == 0) {
                tvResultSubline.setText(emptyResultSubtitle());
            } else if (!TextUtils.isEmpty(buildActiveFilterSummary())) {
                tvResultSubline.setText(totalCount + " kết quả sau khi áp dụng bộ lọc hiện tại.");
            } else {
                tvResultSubline.setText("Các kết quả được sắp theo độ phù hợp và rating.");
            }
        }

        tvSummaryTotal.setText(String.valueOf(totalCount));
        tvSummaryFoods.setText(String.valueOf(foodCount));
        tvSummaryPlaces.setText(String.valueOf(placeCount));
    }

    private String emptyResultSubtitle() {
        String activeFilters = buildActiveFilterSummary();
        if (TextUtils.isEmpty(activeFilters)) {
            return "Thử đổi từ khóa hoặc mở rộng phạm vi tìm kiếm.";
        }
        return "Hãy nới điều kiện bộ lọc để xem thêm kết quả phù hợp.";
    }

    private String buildActiveFilterSummary() {
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(filterState.foodType)) {
            parts.add("Loại: " + filterState.foodType);
        }
        if (filterState.openAtMinutes != null) {
            parts.add("Mở lúc: " + formatMinutes(filterState.openAtMinutes));
        }
        if (filterState.minPrice != null || filterState.maxPrice != null) {
            String min = filterState.minPrice != null ? formatNumber(filterState.minPrice) : "0";
            String max = filterState.maxPrice != null ? formatNumber(filterState.maxPrice) : "max";
            parts.add("Giá: " + min + "-" + max);
        }
        if (filterState.minRating != null) {
            parts.add("Từ " + formatNumber(filterState.minRating) + " sao");
        }
        return joinWithDivider(parts.toArray(new String[0]));
    }

    private static void sortRows(List<ResultRow> rows) {
        rows.sort((left, right) -> {
            int ratingCompare = Double.compare(right.rating, left.rating);
            if (ratingCompare != 0) {
                return ratingCompare;
            }
            return left.title.compareToIgnoreCase(right.title);
        });
    }

    private static boolean containsNormalized(String source, String normalizedTarget) {
        return !TextUtils.isEmpty(source) && normalizeText(source).contains(normalizedTarget);
    }

    private static String normalizeText(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private static String cleanInput(String text) {
        String cleaned = text != null ? text.trim() : "";
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String fallback(String primary, String backup) {
        return TextUtils.isEmpty(primary) ? backup : primary;
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private static String joinTexts(List<String> texts) {
        return texts == null || texts.isEmpty() ? null : TextUtils.join(" ", texts);
    }

    private static String joinWithDivider(String... parts) {
        List<String> filteredParts = new ArrayList<>();
        for (String part : parts) {
            if (!TextUtils.isEmpty(part)) {
                filteredParts.add(part);
            }
        }
        return filteredParts.isEmpty() ? "" : TextUtils.join(" | ", filteredParts);
    }

    private static String formatRating(double rating) {
        if (rating <= 0d) {
            return null;
        }
        return String.format(Locale.getDefault(), "★ %.1f", rating);
    }

    private static String formatCurrency(double price) {
        if (price <= 0d) {
            return null;
        }
        return formatNumber(price) + "đ";
    }

    private static String formatPriceRange(String priceRange) {
        if (TextUtils.isEmpty(priceRange)) {
            return null;
        }
        switch (priceRange.toUpperCase(Locale.ROOT)) {
            case "CHEAP":
                return "0-" + CHEAP_MAX_PRICE + "đ";
            case "MEDIUM":
                return CHEAP_MAX_PRICE + "-" + MEDIUM_MAX_PRICE + "đ";
            case "EXPENSIVE":
                return ">=" + MEDIUM_MAX_PRICE + "đ";
            default:
                return priceRange;
        }
    }

    private static double getBucketMinPrice(String priceRange) {
        if (TextUtils.isEmpty(priceRange)) {
            return -1d;
        }
        switch (priceRange.toUpperCase(Locale.ROOT)) {
            case "CHEAP":
                return 0d;
            case "MEDIUM":
                return CHEAP_MAX_PRICE;
            case "EXPENSIVE":
                return MEDIUM_MAX_PRICE;
            default:
                return -1d;
        }
    }

    private static double getBucketMaxPrice(String priceRange) {
        if (TextUtils.isEmpty(priceRange)) {
            return -1d;
        }
        switch (priceRange.toUpperCase(Locale.ROOT)) {
            case "CHEAP":
                return CHEAP_MAX_PRICE;
            case "MEDIUM":
                return MEDIUM_MAX_PRICE;
            case "EXPENSIVE":
                return Double.MAX_VALUE;
            default:
                return -1d;
        }
    }

    private static Integer parseTimeToMinutes(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }

        String[] parts = value.split(":");
        if (parts.length != 2) {
            return null;
        }

        try {
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return null;
            }
            return (hour * 60) + minute;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatTimeRange(String openTime, String closeTime) {
        if (TextUtils.isEmpty(openTime) || TextUtils.isEmpty(closeTime)) {
            return null;
        }
        return openTime + " - " + closeTime;
    }

    private static String formatMinutes(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
    }

    private static Double parseDoubleOrNull(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String formatNumber(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        DecimalFormat formatter = new DecimalFormat("#,##0.##", symbols);
        return formatter.format(value);
    }

    private static class FilterState {
        private String foodType;
        private Integer openAtMinutes;
        private Double minPrice;
        private Double maxPrice;
        private Double minRating;

        private void clear() {
            foodType = null;
            openAtMinutes = null;
            minPrice = null;
            maxPrice = null;
            minRating = null;
        }
    }

    private static class SectionHeader {
        private final String title;
        private final int count;
        private final String caption;

        private SectionHeader(String title, int count, String caption) {
            this.title = title;
            this.count = count;
            this.caption = caption;
        }
    }

    private static class Separator {
    }

    private static class ResultRow {
        private final String title;
        private final String subtitle;
        private final String typeChip;
        private final String priceChip;
        private final String ratingChip;
        private final String timeChip;
        private final String iconLabel;
        private final double rating;
        private final boolean emptyState;

        private ResultRow(
                String title,
                String subtitle,
                String typeChip,
                String priceChip,
                String ratingChip,
                String timeChip,
                String iconLabel,
                double rating,
                boolean emptyState
        ) {
            this.title = title;
            this.subtitle = subtitle;
            this.typeChip = typeChip;
            this.priceChip = priceChip;
            this.ratingChip = ratingChip;
            this.timeChip = timeChip;
            this.iconLabel = iconLabel;
            this.rating = rating;
            this.emptyState = emptyState;
        }

        private static ResultRow result(
                String title,
                String subtitle,
                String typeChip,
                String priceChip,
                String ratingChip,
                String timeChip,
                String iconLabel,
                double rating
        ) {
            return new ResultRow(title, subtitle, typeChip, priceChip, ratingChip, timeChip, iconLabel, rating, false);
        }

        private static ResultRow empty(String title, String subtitle) {
            return new ResultRow(title, subtitle, null, null, null, null, null, 0d, true);
        }
    }

    private class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            Object item = displayList.get(position);
            if (item instanceof SectionHeader) {
                return TYPE_HEADER;
            }
            if (item instanceof Separator) {
                return TYPE_SEPARATOR;
            }
            return TYPE_RESULT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                return new HeaderViewHolder(inflater.inflate(R.layout.item_search_header, parent, false));
            }
            if (viewType == TYPE_SEPARATOR) {
                return new SeparatorViewHolder(inflater.inflate(R.layout.view_separator, parent, false));
            }
            return new ItemViewHolder(inflater.inflate(R.layout.item_search_result, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = displayList.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).bind((SectionHeader) item);
                return;
            }

            if (holder instanceof ItemViewHolder) {
                ((ItemViewHolder) holder).bind((ResultRow) item);
            }
        }

        @Override
        public int getItemCount() {
            return displayList.size();
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            private final TextView tvTitle;
            private final TextView tvCaption;
            private final TextView tvCount;

            HeaderViewHolder(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvHeaderTitle);
                tvCaption = itemView.findViewById(R.id.tvHeaderCaption);
                tvCount = itemView.findViewById(R.id.tvHeaderCount);
            }

            void bind(SectionHeader header) {
                tvTitle.setText(header.title);
                tvCaption.setText(header.caption);
                tvCount.setText(String.valueOf(header.count));
            }
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            private final View badgeContainer;
            private final TextView tvBadgeLabel;
            private final TextView tvName;
            private final TextView tvAddress;
            private final LinearLayout chipsRow;
            private final TextView tvTypeChip;
            private final TextView tvPriceChip;
            private final TextView tvRatingChip;
            private final TextView tvTimeChip;
            private final TextView tvArrow;

            ItemViewHolder(View itemView) {
                super(itemView);
                badgeContainer = itemView.findViewById(R.id.resultBadgeContainer);
                tvBadgeLabel = itemView.findViewById(R.id.tvResultBadge);
                tvName = itemView.findViewById(R.id.tvPlaceName);
                tvAddress = itemView.findViewById(R.id.tvPlaceAddress);
                chipsRow = itemView.findViewById(R.id.layoutResultChips);
                tvTypeChip = itemView.findViewById(R.id.tvTypeChip);
                tvPriceChip = itemView.findViewById(R.id.tvPriceChip);
                tvRatingChip = itemView.findViewById(R.id.tvRatingChip);
                tvTimeChip = itemView.findViewById(R.id.tvTimeChip);
                tvArrow = itemView.findViewById(R.id.tvArrow);
            }

            void bind(ResultRow row) {
                tvName.setText(row.title);
                tvAddress.setText(row.subtitle);

                if (row.emptyState) {
                    badgeContainer.setVisibility(View.GONE);
                    chipsRow.setVisibility(View.GONE);
                    tvArrow.setVisibility(View.GONE);
                    return;
                }

                badgeContainer.setVisibility(View.VISIBLE);
                tvArrow.setVisibility(View.VISIBLE);
                tvBadgeLabel.setText(row.iconLabel);

                int visibleChipCount = 0;
                visibleChipCount += bindChip(tvTypeChip, row.typeChip);
                visibleChipCount += bindChip(tvPriceChip, row.priceChip);
                visibleChipCount += bindChip(tvRatingChip, row.ratingChip);
                visibleChipCount += bindChip(tvTimeChip, row.timeChip);
                chipsRow.setVisibility(visibleChipCount > 0 ? View.VISIBLE : View.GONE);
            }

            private int bindChip(TextView chipView, String text) {
                if (TextUtils.isEmpty(text)) {
                    chipView.setVisibility(View.GONE);
                    return 0;
                }
                chipView.setVisibility(View.VISIBLE);
                chipView.setText(text);
                return 1;
            }
        }

        class SeparatorViewHolder extends RecyclerView.ViewHolder {
            SeparatorViewHolder(View itemView) {
                super(itemView);
            }
        }
    }
}
