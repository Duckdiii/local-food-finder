package com.example.cuisine_finder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cuisine_finder.activities.FoodPlaceDetailActivity;
import com.example.cuisine_finder.activities.PlacesByCategoryActivity;
import com.example.cuisine_finder.activities.StoryViewerActivity;
import com.example.cuisine_finder.activities.UserProfileActivity;
import com.example.cuisine_finder.models.FoodCategory;
import com.example.cuisine_finder.models.FoodItem;
import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.models.Friendship;
import com.example.cuisine_finder.models.Story;
import com.example.cuisine_finder.models.User;
import com.example.cuisine_finder.repositories.CategoryRepository;
import com.example.cuisine_finder.repositories.FoodItemRepository;
import com.example.cuisine_finder.repositories.FriendshipRepository;
import com.example.cuisine_finder.repositories.PlaceRepository;
import com.example.cuisine_finder.repositories.StoryRepository;
import com.example.cuisine_finder.repositories.UserRepository;
import com.example.cuisine_finder.services.AuthService;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private static final double FEATURED_MIN_RATING = 4.8d;

    private RecyclerView rvFriends, rvCategories, rvFeaturedFood, rvTrendingPlaces, rvStories;
    private FriendsAdapter friendsAdapter;
    private CategoryAdapter categoryAdapter;
    private FeaturedFoodAdapter featuredFoodAdapter;
    private TrendingPlaceAdapter trendingAdapter;
    private StoryAdapter storyAdapter;
    
    private TextView tvWelcome;

    private List<User> friendsList = new ArrayList<>();
    private List<FoodCategory> categoriesList = new ArrayList<>();
    private List<FoodItem> featuredFoodList = new ArrayList<>();
    private List<FoodPlace> trendingList = new ArrayList<>();
    private List<Story> storyList = new ArrayList<>();

    private AuthService authService;
    private FriendshipRepository friendshipRepository;
    private UserRepository userRepository;
    private CategoryRepository categoryRepository;
    private FoodItemRepository foodItemRepository;
    private PlaceRepository placeRepository;
    private StoryRepository storyRepository;

    private TextView btnRandomSuggestion;

    private static final int LOCATION_PERMISSION_REQUEST = 1002;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        authService = new AuthService();
        friendshipRepository = new FriendshipRepository();
        userRepository = new UserRepository();
        categoryRepository = new CategoryRepository();
        foodItemRepository = new FoodItemRepository();
        placeRepository = new PlaceRepository();
        storyRepository = new StoryRepository();

        rvFriends = view.findViewById(R.id.rvFriendsEating);
        rvCategories = view.findViewById(R.id.rvFoodCategories);
        rvFeaturedFood = view.findViewById(R.id.rvFeaturedFood);
        rvTrendingPlaces = view.findViewById(R.id.rvTrendingPlaces);
        rvStories = view.findViewById(R.id.rvStories);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        btnRandomSuggestion = view.findViewById(R.id.btnRandomSuggestion);
        btnRandomSuggestion.setOnClickListener(v -> startRandomSuggestion());

        setupRecyclerViews();

        EditText etSearch = view.findViewById(R.id.etHomeSearch);
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                navigateToSearchResult(etSearch.getText().toString());
                return true;
            }
            return false;
        });

        loadFriendsActivity();
        loadFoodCategories();
        loadFeaturedFoodItems();
        loadTrendingPlaces();
        loadUserWelcomeName();
        loadStories();

        // Load location-based nearby restaurant recommendation
        checkLocationAndLoadNearby();

        // Setup Map text click listener
        TextView tvHomeMapNearby = view.findViewById(R.id.tvHomeMapNearby);
        if (tvHomeMapNearby != null) {
            tvHomeMapNearby.setOnClickListener(v -> {
                if (getActivity() != null) {
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                            getActivity().findViewById(R.id.bottomNavigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_explore);
                    }
                }
            });
        }

        // Setup View All Friends click listener
        TextView tvHomeViewAllFriends = view.findViewById(R.id.tvHomeViewAllFriends);
        if (tvHomeViewAllFriends != null) {
            tvHomeViewAllFriends.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), com.example.cuisine_finder.activities.FriendsActivity.class);
                startActivity(intent);
            });
        }

        return view;
    }

    private void setupRecyclerViews() {
        friendsAdapter = new FriendsAdapter(friendsList);
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFriends.setAdapter(friendsAdapter);

        categoryAdapter = new CategoryAdapter(categoriesList);
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        featuredFoodAdapter = new FeaturedFoodAdapter(featuredFoodList);
        rvFeaturedFood.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFeaturedFood.setAdapter(featuredFoodAdapter);

        trendingAdapter = new TrendingPlaceAdapter(trendingList);
        rvTrendingPlaces.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTrendingPlaces.setAdapter(trendingAdapter);

        storyAdapter = new StoryAdapter(storyList);
        rvStories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvStories.setAdapter(storyAdapter);
    }

    private void loadFriendsActivity() {
        if (!authService.isLoggedIn()) return;

        String currentUserId = authService.getCurrentUser().getUid();

        friendsList.clear();
        friendsAdapter.notifyDataSetChanged();

        friendshipRepository.getFriendsByRequester(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String status = doc.getString("status");
                    if (Friendship.STATUS_ACCEPTED.equals(status)) {
                        String friendId = doc.getString("receiverId");
                        if (friendId != null) fetchFriendProfile(friendId);
                    }
                }
            }
        });

        friendshipRepository.getFriendsByReceiver(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String status = doc.getString("status");
                    if (Friendship.STATUS_ACCEPTED.equals(status)) {
                        String friendId = doc.getString("requesterId");
                        if (friendId != null) fetchFriendProfile(friendId);
                    }
                }
            }
        });
    }

    private void fetchFriendProfile(String friendId) {
        userRepository.getUser(friendId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User friend = task.getResult().toObject(User.class);
                if (friend != null) {
                    friend.setId(friendId);
                    friendsList.add(friend);
                    friendsAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void loadFoodCategories() {
        categoryRepository.getAllCategories().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                categoriesList.clear();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    FoodCategory cat = doc.toObject(FoodCategory.class);
                    if (cat != null) {
                        categoriesList.add(cat);
                    }
                }
                categoryAdapter.notifyDataSetChanged();
            }
        });
    }

    private void loadFeaturedFoodItems() {
        foodItemRepository.getFeaturedFoodItems(FEATURED_MIN_RATING).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                featuredFoodList.clear();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    FoodItem foodItem = doc.toObject(FoodItem.class);
                    if (foodItem != null && foodItem.getAverageRating() >= FEATURED_MIN_RATING) {
                        featuredFoodList.add(foodItem);
                    }
                }
                featuredFoodAdapter.notifyDataSetChanged();
            }
        });
    }

    private void loadUserWelcomeName() {
        if (!authService.isLoggedIn()) return;

        String currentUserId = authService.getCurrentUser().getUid();
        userRepository.getUser(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                if (user != null && user.getFullName() != null) {
                    tvWelcome.setText("Xin chào, " + user.getFullName());
                }
            }
        });
    }

    private void loadTrendingPlaces() {
        placeRepository.getApprovedPlaces().orderBy("favoriteCount", Query.Direction.DESCENDING).limit(10).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        trendingList.clear();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            FoodPlace place = doc.toObject(FoodPlace.class);
                            if (place != null) {
                                place.setId(doc.getId());
                                trendingList.add(place);
                            }
                        }
                        trendingAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void loadStories() {
        storyRepository.getActiveStories().get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                storyList.clear();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Story story = doc.toObject(Story.class);
                    if (story != null) storyList.add(story);
                }
                if (storyList.isEmpty()) {
                    createMockStories();
                } else {
                    storyAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void createMockStories() {
        String[] names = {"Nguyễn Văn A", "Trần Thị B", "Lê Văn C"};
        String[] captions = {"🍜 Bún bò Huế ngon xỉu!", "🍕 Pizza tối nay nè", "☕ Cà phê sáng sảng khoái"};
        String[] storyImages = {
            "https://images.unsplash.com/photo-1583085293629-77ab47743d22?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=800&q=80"
        };
        String[] avatars = {
            "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&q=80",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&q=80"
        };

        for (int i = 0; i < names.length; i++) {
            Story story = new Story();
            story.setUserName(names[i]);
            story.setCaption(captions[i]);
            story.setImageUrl(storyImages[i]);
            story.setUserAvatarUrl(avatars[i]);
            story.setUserId("mock_user_" + i);
            storyRepository.uploadStory(story);
            storyList.add(story);
        }
        storyAdapter.notifyDataSetChanged();
    }

    private void checkLocationAndLoadNearby() {
        if (!isAdded()) return;
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        retrieveLocationAndLoad();
    }

    private void retrieveLocationAndLoad() {
        if (!isAdded()) return;
        android.location.LocationManager lm = (android.location.LocationManager)
                requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        if (lm == null) {
            loadFallbackNearbyRestaurant();
            return;
        }
        try {
            android.location.Location last = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (last == null) last = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);

            if (last != null) {
                loadNearbyRestaurant(last.getLatitude(), last.getLongitude());
            } else {
                android.location.LocationListener listener = new android.location.LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull android.location.Location loc) {
                        if (!isAdded()) return;
                        loadNearbyRestaurant(loc.getLatitude(), loc.getLongitude());
                    }
                    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override public void onProviderEnabled(@NonNull String provider) {}
                    @Override public void onProviderDisabled(@NonNull String provider) {}
                };
                if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    lm.requestSingleUpdate(android.location.LocationManager.GPS_PROVIDER, listener,
                            requireActivity().getMainLooper());
                } else if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    lm.requestSingleUpdate(android.location.LocationManager.NETWORK_PROVIDER, listener,
                            requireActivity().getMainLooper());
                } else {
                    loadFallbackNearbyRestaurant();
                }
            }
        } catch (SecurityException e) {
            loadFallbackNearbyRestaurant();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                retrieveLocationAndLoad();
            } else {
                loadFallbackNearbyRestaurant();
            }
        }
    }

    private void loadNearbyRestaurant(double userLat, double userLon) {
        placeRepository.getCachedApprovedPlaces(new PlaceRepository.OnPlacesLoadedCallback() {
            @Override
            public void onLoaded(List<FoodPlace> places) {
                if (!isAdded() || places.isEmpty()) return;
                FoodPlace nearest = null;
                double minDistance = Double.MAX_VALUE;
                for (FoodPlace place : places) {
                    double dist = distanceKm(userLat, userLon, place.getLatitude(), place.getLongitude());
                    if (dist < minDistance) {
                        minDistance = dist;
                        nearest = place;
                    }
                }
                if (nearest != null) {
                    bindNearbyRestaurant(nearest, minDistance);
                }
            }

            @Override
            public void onError() {
                loadFallbackNearbyRestaurant();
            }
        });
    }

    private void loadFallbackNearbyRestaurant() {
        placeRepository.getCachedApprovedPlaces(new PlaceRepository.OnPlacesLoadedCallback() {
            @Override
            public void onLoaded(List<FoodPlace> places) {
                if (!isAdded() || places.isEmpty()) return;
                bindNearbyRestaurant(places.get(0), -1.0);
            }
            @Override
            public void onError() {}
        });
    }

    private void bindNearbyRestaurant(FoodPlace place, double distanceKm) {
        if (!isAdded() || getView() == null) return;
        ImageView ivPlaceImage = getView().findViewById(R.id.ivPlaceImage);
        TextView tvPlaceName = getView().findViewById(R.id.tvPlaceName);
        TextView tvPlaceInfo = getView().findViewById(R.id.tvPlaceInfo);
        TextView tvPlaceStats = getView().findViewById(R.id.tvPlaceStats);
        View cardNearby = getView().findViewById(R.id.cardNearbyRestaurant);

        if (tvPlaceName != null) tvPlaceName.setText(place.getName() != null ? place.getName() : "Quán ăn");
        if (tvPlaceInfo != null) {
            String info = (place.getFoodType() != null ? place.getFoodType() : "Món ăn")
                    + " • " + (place.getAddress() != null ? place.getAddress() : "Chưa có địa chỉ");
            tvPlaceInfo.setText(info);
        }
        if (tvPlaceStats != null) {
            if (distanceKm >= 0) {
                String distStr;
                if (distanceKm < 1.0) {
                    distStr = String.format(Locale.getDefault(), "%d m", (int) (distanceKm * 1000));
                } else {
                    distStr = String.format(Locale.getDefault(), "%.1f km", distanceKm);
                }
                tvPlaceStats.setText(String.format(Locale.getDefault(), "⭐ %.1f • %s", place.getAverageRating(), distStr));
            } else {
                tvPlaceStats.setText(String.format(Locale.getDefault(), "⭐ %.1f", place.getAverageRating()));
            }
        }
        if (ivPlaceImage != null) {
            if (place.getImageUrls() != null && !place.getImageUrls().isEmpty()) {
                Glide.with(this).load(place.getImageUrls().get(0))
                        .placeholder(R.drawable.bg_image_placeholder).centerCrop().into(ivPlaceImage);
            } else {
                ivPlaceImage.setImageResource(R.drawable.bg_image_placeholder);
            }
        }
        if (cardNearby != null) {
            cardNearby.setOnClickListener(v -> {
                if (place.getId() != null) {
                    Intent intent = new Intent(getActivity(), FoodPlaceDetailActivity.class);
                    intent.putExtra(FoodPlaceDetailActivity.EXTRA_PLACE_ID, place.getId());
                    startActivity(intent);
                }
            });
        }
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ─── Random suggestion ────────────────────────────────────────────────────

    private void startRandomSuggestion() {
        btnRandomSuggestion.setEnabled(false);
        btnRandomSuggestion.setText("🎲  Đang chọn...");

        placeRepository.getCachedApprovedPlaces(new PlaceRepository.OnPlacesLoadedCallback() {
            @Override
            public void onLoaded(List<FoodPlace> places) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnRandomSuggestion.setEnabled(true);
                    btnRandomSuggestion.setText("🎲  Không biết ăn gì? Để tôi chọn!");
                    if (places.isEmpty()) {
                        Toast.makeText(getContext(), "Chưa có dữ liệu quán ăn", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<FoodPlace> shuffled = new ArrayList<>(places);
                    Collections.shuffle(shuffled);
                    showSuggestionDialog(shuffled, 0);
                });
            }

            @Override
            public void onError() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnRandomSuggestion.setEnabled(true);
                    btnRandomSuggestion.setText("🎲  Không biết ăn gì? Để tôi chọn!");
                    Toast.makeText(getContext(), "Lỗi kết nối, thử lại sau", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSuggestionDialog(List<FoodPlace> shuffled, int tryIndex) {
        if (!isAdded()) return;
        FoodPlace place = shuffled.get(tryIndex % shuffled.size());

        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_random_suggestion, null, false);

        TextView tvCounter     = dialogView.findViewById(R.id.tvSuggestionCounter);
        TextView tvName        = dialogView.findViewById(R.id.tvSuggestedName);
        TextView tvStatus      = dialogView.findViewById(R.id.tvSuggestedStatus);
        TextView tvFoodType    = dialogView.findViewById(R.id.tvSuggestedFoodType);
        TextView tvAddress     = dialogView.findViewById(R.id.tvSuggestedAddress);
        TextView tvRating      = dialogView.findViewById(R.id.tvSuggestedRating);
        TextView tvPrice       = dialogView.findViewById(R.id.tvSuggestedPrice);
        TextView tvHours       = dialogView.findViewById(R.id.tvSuggestedHours);
        TextView btnTryAgain   = dialogView.findViewById(R.id.btnTryAgain);
        TextView btnDetail     = dialogView.findViewById(R.id.btnSuggestionDetail);

        tvCounter.setText("Gợi ý #" + (tryIndex + 1));
        tvName.setText(place.getName() != null ? place.getName() : "Quán ăn");

        boolean open = isCurrentlyOpen(place);
        tvStatus.setText(open ? "Mở cửa" : "Đóng cửa");
        tvStatus.setTextColor(requireContext().getColor(open ? R.color.green_open : R.color.red_close));
        tvStatus.setBackgroundResource(open ? R.drawable.bg_chip_green : R.drawable.bg_chip_red);

        if (place.getFoodType() != null && !place.getFoodType().isEmpty()) {
            tvFoodType.setText(place.getFoodType());
            tvFoodType.setVisibility(View.VISIBLE);
        }

        tvAddress.setText(place.getAddress() != null && !place.getAddress().isEmpty()
            ? "📍 " + place.getAddress() : "Chưa có địa chỉ");

        if (place.getAverageRating() > 0) {
            tvRating.setText(String.format(Locale.getDefault(), "★ %.1f", place.getAverageRating()));
        } else {
            tvRating.setText("Chưa có đánh giá");
        }

        String formattedPrice = formatPriceRange(place.getPriceRange());
        if (formattedPrice != null) {
            tvPrice.setText(formattedPrice);
            tvPrice.setVisibility(View.VISIBLE);
        }

        if (place.getOpenTime() != null && place.getCloseTime() != null) {
            tvHours.setText("🕐 " + place.getOpenTime() + " – " + place.getCloseTime());
            tvHours.setVisibility(View.VISIBLE);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_card);
        }

        btnTryAgain.setOnClickListener(v -> {
            dialog.dismiss();
            showSuggestionDialog(shuffled, tryIndex + 1);
        });

        btnDetail.setOnClickListener(v -> {
            dialog.dismiss();
            if (place.getId() != null) {
                Intent intent = new Intent(getActivity(), FoodPlaceDetailActivity.class);
                intent.putExtra(FoodPlaceDetailActivity.EXTRA_PLACE_ID, place.getId());
                startActivity(intent);
            }
        });

        dialog.show();
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
            if (open <= close) return current >= open && current <= close;
            return current >= open || current <= close; // crosses midnight
        } catch (Exception e) {
            return true;
        }
    }

    private String formatPriceRange(String priceRange) {
        if (priceRange == null || priceRange.isEmpty()) return null;
        switch (priceRange.toUpperCase(Locale.ROOT)) {
            case "CHEAP":     return "💰 Bình dân";
            case "MEDIUM":    return "💰 Vừa phải";
            case "EXPENSIVE": return "💰 Cao cấp";
            default:          return priceRange;
        }
    }

    private void navigateToSearchResult(String query) {
        if (query.isEmpty()) return;

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, SearchResultFragment.newInstance(query))
                .addToBackStack(null)
                .commit();
    }

    private class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {
        private List<User> items;

        public FriendsAdapter(List<User> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_avatar, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User friend = items.get(position);
            holder.tvName.setText(friend.getFullName() != null ? friend.getFullName() : "Bạn bè");
            String[] emojis = {"🧑‍🍳", "🍕", "🍜", "🍔", "🍣"};
            holder.tvEmoji.setText(emojis[position % emojis.length]);

            holder.itemView.setOnClickListener(v -> {
                if (friend.getId() != null) {
                    Intent intent = new Intent(requireContext(), UserProfileActivity.class);
                    intent.putExtra(UserProfileActivity.EXTRA_USER_ID, friend.getId());
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvName;
            ViewHolder(View v) {
                super(v);
                tvEmoji = v.findViewById(R.id.tvFriendEmoji);
                tvName = v.findViewById(R.id.tvFriendName);
            }
        }
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
        private List<FoodCategory> items;

        public CategoryAdapter(List<FoodCategory> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_category, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FoodCategory cat = items.get(position);
            holder.tvName.setText(cat.getName());
            String emoji = "🍲";
            String name = cat.getName().toLowerCase();
            if (name.contains("coffee") || name.contains("cà phê")) emoji = "☕";
            else if (name.contains("bún")) emoji = "🍜";
            else if (name.contains("cơm")) emoji = "🍚";
            else if (name.contains("lẩu")) emoji = "🍲";
            else if (name.contains("ốc")) emoji = "🐚";

            holder.tvEmoji.setText(emoji);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), PlacesByCategoryActivity.class);
                intent.putExtra(PlacesByCategoryActivity.EXTRA_CATEGORY_NAME, cat.getName());
                intent.putExtra(PlacesByCategoryActivity.EXTRA_CATEGORY_DESCRIPTION, cat.getDescription());
                intent.putExtra(PlacesByCategoryActivity.EXTRA_CATEGORY_ICON_URL, cat.getIconUrl());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvName;
            ViewHolder(View v) {
                super(v);
                tvEmoji = v.findViewById(R.id.tvCategoryEmoji);
                tvName = v.findViewById(R.id.tvCategoryName);
            }
        }
    }

    private class FeaturedFoodAdapter extends RecyclerView.Adapter<FeaturedFoodAdapter.ViewHolder> {
        private final List<FoodItem> items;

        FeaturedFoodAdapter(List<FoodItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured_food, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FoodItem foodItem = items.get(position);
            String foodName = foodItem.getName() != null ? foodItem.getName() : "Món nổi bật";
            String featuredTag = foodItem.getCategoryName() != null && !foodItem.getCategoryName().trim().isEmpty()
                    ? foodItem.getCategoryName()
                    : "Nổi bật";

            holder.tvFeaturedFoodName.setText(foodName);
            holder.tvFeaturedTag.setText(featuredTag);
            holder.tvFeaturedFoodRating.setText(String.format(Locale.getDefault(), "★ %.1f", foodItem.getAverageRating()));
            
            if (foodItem.getImageUrls() != null && !foodItem.getImageUrls().isEmpty()) {
                Glide.with(holder.ivFeaturedFood.getContext())
                        .load(foodItem.getImageUrls().get(0))
                        .placeholder(R.drawable.bg_image_placeholder)
                        .centerCrop()
                        .into(holder.ivFeaturedFood);
            } else {
                holder.ivFeaturedFood.setImageResource(R.drawable.bg_image_placeholder);
            }

            holder.itemView.setOnClickListener(v -> {
                if (foodItem.getPlaceId() != null) {
                    Intent intent = new Intent(holder.itemView.getContext(), FoodPlaceDetailActivity.class);
                    intent.putExtra(FoodPlaceDetailActivity.EXTRA_PLACE_ID, foodItem.getPlaceId());
                    holder.itemView.getContext().startActivity(intent);
                } else {
                    Toast.makeText(holder.itemView.getContext(), "Món ăn chưa có thông tin quán", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivFeaturedFood;
            private final TextView tvFeaturedTag;
            private final TextView tvFeaturedFoodName;
            private final TextView tvFeaturedFoodRating;

            ViewHolder(View itemView) {
                super(itemView);
                ivFeaturedFood = itemView.findViewById(R.id.ivFeaturedFood);
                tvFeaturedTag = itemView.findViewById(R.id.tvFeaturedTag);
                tvFeaturedFoodName = itemView.findViewById(R.id.tvFeaturedFoodName);
                tvFeaturedFoodRating = itemView.findViewById(R.id.tvFeaturedFoodRating);
            }
        }
    }

    private class TrendingPlaceAdapter extends RecyclerView.Adapter<TrendingPlaceAdapter.ViewHolder> {
        private final List<FoodPlace> items;

        TrendingPlaceAdapter(List<FoodPlace> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending_place, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            FoodPlace place = items.get(position);
            holder.tvName.setText(place.getName());
            holder.tvStats.setText(String.format(Locale.getDefault(), "❤ %d  ·  ✍ %d reviews", place.getFavoriteCount(), place.getReviewCount()));
            
            if (place.getImageUrls() != null && !place.getImageUrls().isEmpty()) {
                Glide.with(holder.ivImage.getContext()).load(place.getImageUrls().get(0)).placeholder(R.drawable.bg_image_placeholder).into(holder.ivImage);
            } else {
                holder.ivImage.setImageResource(R.drawable.bg_image_placeholder);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), FoodPlaceDetailActivity.class);
                intent.putExtra(FoodPlaceDetailActivity.EXTRA_PLACE_ID, place.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView ivImage;
            private final TextView tvName;
            private final TextView tvStats;

            ViewHolder(View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.ivTrendingImage);
                tvName = itemView.findViewById(R.id.tvTrendingName);
                tvStats = itemView.findViewById(R.id.tvTrendingStats);
            }
        }
    }

    private class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.ViewHolder> {
        private List<Story> items;
        StoryAdapter(List<Story> items) { this.items = items; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_circle, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Story story = items.get(position);
            holder.tvName.setText(story.getUserName() != null ? story.getUserName() : "User");
            Glide.with(holder.ivThumb.getContext()).load(story.getImageUrl()).placeholder(R.drawable.bg_image_placeholder).into(holder.ivThumb);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), StoryViewerActivity.class);
                intent.putExtra("START_INDEX", position);
                startActivity(intent);
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumb;
            TextView tvName;
            ViewHolder(View v) {
                super(v);
                ivThumb = v.findViewById(R.id.ivStoryThumb);
                tvName = v.findViewById(R.id.tvStoryUserName);
            }
        }
    }
}
