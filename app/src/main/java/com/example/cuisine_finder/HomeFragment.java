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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private ImageView ivUserAvatar;
    private String currentUserAvatarUrl = null;

    //mở một ứng dụng khác và nhận kết quả trả về
    private final ActivityResultLauncher<String> storyImagePickerLauncher = // nhấn nút "phóng" là ứng dụng thư viện ảnh sẽ hiện ra
            //
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> { // lấy nội dung từ máy điện thoại -> ảnh
                if (uri != null) {
                    uploadStory(uri);
                }
            });

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
    private SwipeRefreshLayout swipeRefreshLayout;
    private View mRootView;

    private static final int LOCATION_PERMISSION_REQUEST = 1002;
    //onCreate()
    //    ↓
    //onCreateView()   ← tạo layout ở đây
    //    ↓
    //onViewCreated()  ← tìm view, gán listener ở đây
    //    ↓
    //onStart()
    //    ↓
    //onResume()
    @Nullable
    @Override
    //là một lifecycle method của Fragment trong Android, được gọi khi Fragment cần tạo giao diện (UI) của nó
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Tạo và trả về View (layout) cho Fragment
        //Được gọi sau onCreate() và trước onViewCreated()
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        mRootView = view;

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.orange_main);
            swipeRefreshLayout.setOnRefreshListener(this::refreshAllData);
        }

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

        ivUserAvatar = view.findViewById(R.id.ivUserAvatar);
        //nhấn vào ảnh đại diện (Avatar) ở góc màn hình để nhảy nhanh sang trang Cá nhân (Profile)
        View cardUserAvatar = view.findViewById(R.id.cardUserAvatar);// lấy view của card chứa avatar người dùng
        if (cardUserAvatar != null) {
            cardUserAvatar.setOnClickListener(v -> {//Khi người dùng chạm tay vào vùng ảnh đại diện
                if (getActivity() != null) {//  nếu activity hiện tại không null, tức là fragment đang được hiển thị trong một activity
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                            getActivity().findViewById(R.id.bottomNavigation);
                    if (bottomNav != null) {//  nếu bottomNav không null, tức là tìm thấy BottomNavigationView trong activity
                        bottomNav.setSelectedItemId(R.id.nav_profile); // chuyển tab
                    }
                }
            });
        }

        setupRecyclerViews();

        EditText etSearch = view.findViewById(R.id.etHomeSearch);// ô nhập liệu tìm kiếm món ăn/quán ăn
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {//   Khi người dùng nhấn nút tìm kiếm trên bàn phím ảo (IME_ACTION_SEARCH)
                navigateToSearchResult(etSearch.getText().toString());
                return true;
            }
            return false;
        });

        View chipFilterOpen = view.findViewById(R.id.chipFilterOpen);
        View chipFilterNearby = view.findViewById(R.id.chipFilterNearby);
        View chipFilterRating = view.findViewById(R.id.chipFilterRating);
        View chipFilterBudget = view.findViewById(R.id.chipFilterBudget);

        if (chipFilterOpen != null) {
            chipFilterOpen.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, SearchResultFragment.newInstance("", "OPEN_NOW", ""))
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (chipFilterNearby != null) {
            chipFilterNearby.setOnClickListener(v -> {
                com.example.cuisine_finder.ExploreFragment.pendingNearMeFilter = true;
                if (getActivity() != null) {
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                            getActivity().findViewById(R.id.bottomNavigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_explore);
                    }
                }
            });
        }

        if (chipFilterRating != null) {
            chipFilterRating.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, SearchResultFragment.newInstance("", "MIN_RATING", "4.5"))
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (chipFilterBudget != null) {
            chipFilterBudget.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragmentContainer, SearchResultFragment.newInstance("", "PRICE_RANGE", "CHEAP"))
                        .addToBackStack(null)
                        .commit();
            });
        }

        loadFriendsActivity();
        loadFoodCategories();
        loadFeaturedFoodItems();
        loadTrendingPlaces();
        loadUserWelcomeName();
        loadStories();

        // Load location-based nearby restaurant recommendation
        checkLocationAndLoadNearby();

        // Setup Map text click listener
        TextView tvHomeMapNearby = view.findViewById(R.id.tvHomeMapNearby);// nút chuyển sang bản đồ khám phá
        if (tvHomeMapNearby != null) {
            tvHomeMapNearby.setOnClickListener(v -> {
                if (getActivity() != null) {
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                            getActivity().findViewById(R.id.bottomNavigation);
                    if (bottomNav != null) {
                        bottomNav.setSelectedItemId(R.id.nav_explore);// chyển tab
                    }
                }
            });
        }

        // Điều hướng từ màn hình này sang màn hình khác
        TextView tvHomeViewAllFriends = view.findViewById(R.id.tvHomeViewAllFriends);// nút chuyển sang trang bạn bè
        if (tvHomeViewAllFriends != null) {
            tvHomeViewAllFriends.setOnClickListener(v -> {
                //Tạo Intent để chuyển sang màn hình FriendsActivity
                Intent intent = new Intent(requireContext(), com.example.cuisine_finder.activities.FriendsActivity.class);
                //Thực sự mở màn hình đó
                startActivity(intent);
            });
        }

        TextView tvViewAllFeatured = view.findViewById(R.id.tvViewAllFeatured);//   nút chuyển sang trang danh sách món ăn nổi bật
        if (tvViewAllFeatured != null) {
            tvViewAllFeatured.setOnClickListener(v -> {
                //Tạo Intent mở màn hình PlacesByCategoryActivity
                Intent intent = new Intent(requireContext(), com.example.cuisine_finder.activities.PlacesByCategoryActivity.class);
                //Truyền dữ liệu kèm theo Intent
                intent.putExtra(com.example.cuisine_finder.activities.PlacesByCategoryActivity.EXTRA_MODE,
                        com.example.cuisine_finder.activities.PlacesByCategoryActivity.MODE_FEATURED);// chế độ: "nổi bật"
                // rating tối thiểu để lọc
                intent.putExtra(com.example.cuisine_finder.activities.PlacesByCategoryActivity.EXTRA_MIN_RATING, FEATURED_MIN_RATING);
                //Mở màn hình
                startActivity(intent);
            });
        }

        return view;
    }
    //khởi tạo 5 danh sách cuộn ngang trên màn hình Home, mỗi danh sách đều theo cùng một pattern
    private void setupRecyclerViews() {
        //Tạo Adapter (cầu nối giữa dữ liệu và giao diện)
        friendsAdapter = new FriendsAdapter(friendsList);
        //Gắn LayoutManager (cuộn ngang)
        rvFriends.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        //Gắn Adapter vào RecyclerView
        rvFriends.setAdapter(friendsAdapter);

        categoryAdapter = new CategoryAdapter(categoriesList);
        //Gắn LayoutManager (cuộn ngang)
        rvCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvCategories.setAdapter(categoryAdapter);

        featuredFoodAdapter = new FeaturedFoodAdapter(featuredFoodList);
        //Gắn LayoutManager (cuộn ngang)
        rvFeaturedFood.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvFeaturedFood.setAdapter(featuredFoodAdapter);

        trendingAdapter = new TrendingPlaceAdapter(trendingList);
        //Gắn LayoutManager (cuộn ngang)
        rvTrendingPlaces.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvTrendingPlaces.setAdapter(trendingAdapter);

        storyAdapter = new StoryAdapter(storyList);
        //Gắn LayoutManager (cuộn ngang)
        rvStories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvStories.setAdapter(storyAdapter);
    }

    private void loadFriendsActivity() {
        if (!authService.isLoggedIn()) return;

        String currentUserId = authService.getCurrentUser().getUid();

        friendsList.clear();
        friendsAdapter.notifyDataSetChanged();

        //Lấy danh sách quan hệ bạn bè mà currentUser đã GỬI lời mời
        //Khi Firebase trả về kết quả (bất đồng bộ) thì chạy code bên trong
        friendshipRepository.getFriendsByRequester(currentUserId).addOnCompleteListener(task -> {
            //Kiểm tra: có thành công và có dữ liệu không?
            if (task.isSuccessful() && task.getResult() != null) {
                //Duyệt qua từng document (mỗi doc = 1 quan hệ bạn bè)
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    //Lấy trạng thái của quan hệ đó
                    String status = doc.getString("status");
                    //Chỉ lấy những người đã CHẤP NHẬN lời mời
                    if (Friendship.STATUS_ACCEPTED.equals(status)) {
                        //Lấy ID của người nhận lời mời
                        String friendId = doc.getString("receiverId");
                        //Fetch thông tin profile của người đó
                        if (friendId != null) fetchFriendProfile(friendId);
                    }
                }
            }
        });
        //lấy bạn bè đã GỬI lời mời CHO currentUser
        friendshipRepository.getFriendsByReceiver(currentUserId).addOnCompleteListener(task -> {
            //Kiểm tra: có thành công và có dữ liệu không?
            if (task.isSuccessful() && task.getResult() != null) {
                //Duyệt qua từng document (mỗi doc = 1 quan hệ bạn bè)
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
    //
    private void fetchFriendProfile(String friendId) {
        userRepository.getUser(friendId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User friend = task.getResult().toObject(User.class);
                if (friend != null) {
                    friend.setId(friendId);
                    friendsList.add(friend);
                    //Thông báo cho RecyclerView cập nhật UI
                    friendsAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void loadFoodCategories() {
        categoryRepository.getAllCategories().addOnCompleteListener(task -> {
            if (categoryAdapter != null) {
                categoryAdapter.setLoading(false);
            }
            if (task.isSuccessful() && task.getResult() != null) {
                categoriesList.clear();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    FoodCategory cat = doc.toObject(FoodCategory.class);
                    if (cat != null) {
                        categoriesList.add(cat);
                    }
                }
                if (categoryAdapter != null) {
                    categoryAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void loadFeaturedFoodItems() { // averageRating
        foodItemRepository.getFeaturedFoodItems(FEATURED_MIN_RATING).addOnCompleteListener(task -> {
            if (featuredFoodAdapter != null) {
                featuredFoodAdapter.setLoading(false);
            }
            if (task.isSuccessful() && task.getResult() != null) {
                featuredFoodList.clear();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    FoodItem foodItem = doc.toObject(FoodItem.class);
                    if (foodItem != null && foodItem.getAverageRating() >= FEATURED_MIN_RATING) {
                        featuredFoodList.add(foodItem);
                    }
                }
                if (featuredFoodAdapter != null) {
                    featuredFoodAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void loadUserWelcomeName() {
        if (!authService.isLoggedIn()) return;

        String currentUserId = authService.getCurrentUser().getUid();
        userRepository.getUser(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                User user = task.getResult().toObject(User.class);
                if (user != null) {
                    if (user.getFullName() != null) {
                        tvWelcome.setText("Xin chào, " + user.getFullName());
                    }
                    currentUserAvatarUrl = user.getAvatarUrl();
                    if (ivUserAvatar != null) {
                        if (currentUserAvatarUrl != null && !currentUserAvatarUrl.isEmpty()) {
                            Glide.with(HomeFragment.this)
                                    .load(currentUserAvatarUrl)
                                    .placeholder(R.drawable.bg_image_placeholder)
                                    .error(R.drawable.bg_avatar_orange)
                                    .into(ivUserAvatar);
                        } else {
                            ivUserAvatar.setImageResource(R.drawable.bg_avatar_orange);
                        }
                    }
                    if (storyAdapter != null) {
                        storyAdapter.notifyItemChanged(0);
                    }
                }
            }
        });
    }

    private void uploadStory(android.net.Uri uri) {
        if (!authService.isLoggedIn() || getContext() == null) return;
        Toast.makeText(getContext(), "Đang đăng tin...", Toast.LENGTH_SHORT).show();

        String uid = authService.getCurrentUser().getUid();

        // Tạo đường dẫn file duy nhất trên Firebase Storage
        // VD: "stories/1719825600000_uid123.jpg"
        String path = "stories/" + System.currentTimeMillis() + "_" + uid + ".jpg";

        // Trỏ tới vị trí sẽ upload trên Firebase Storage
        com.google.firebase.storage.StorageReference ref = com.google.firebase.storage.FirebaseStorage.getInstance().getReference(path);

        ref.putFile(uri) // upload file ảnh từ điện thoại lên Firebase
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                // Upload thành công → lưu story vào Firestore
                .addOnSuccessListener(url -> {
                    saveStoryToFirestore(uid, url.toString());
                })
                // Upload thất bại (mất mạng,...) → lưu ảnh vào bộ nhớ máy thay thế
                .addOnFailureListener(e -> {
                    android.content.Context context = getContext(); //đại diện cho trạng thái hiện tại của ứng dụng và cho phép truy cập vào các tài nguyên hệ thống
                    if (context != null) {
                        // Lưu ảnh vào bộ nhớ máy
                        String localUrl = com.example.cuisine_finder.utils.ImageStorageUtils.saveImageToInternalStorage(context, uri, "stories");
                        saveStoryToFirestore(uid, localUrl);
                        //Hiển thị Toast
                        Toast.makeText(context, "Đăng tin thành công (sử dụng ảnh local do lỗi kết nối)!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Lỗi tải ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveStoryToFirestore(String uid, String imageUrl) {
        // Lấy thông tin người dùng
        userRepository.getUser(uid).addOnSuccessListener(doc -> {
            User user = doc.toObject(User.class);
            String name = (user != null && user.getFullName() != null) ? user.getFullName() : "User";
            String avatar = (user != null && user.getAvatarUrl() != null) ? user.getAvatarUrl() : "";

            //Tạo object Story
            Story story = new Story();
            story.setUserId(uid);
            story.setUserName(name);
            story.setUserAvatarUrl(avatar);
            story.setImageUrl(imageUrl);
            story.setCaption("Mới chia sẻ");
            story.setCreatedAt(System.currentTimeMillis());

            // Lưu đối tượng Story vào Firestore thông qua repository
            storyRepository.uploadStory(story).addOnSuccessListener(documentReference -> {
                // Chỉ hiện Toast nếu ảnh là URL thật (không phải ảnh local)
                if (imageUrl != null && !imageUrl.startsWith("file://")) {
                    Toast.makeText(getContext(), "Đăng tin thành công!", Toast.LENGTH_SHORT).show();
                }
                loadStories(); // Tải lại danh sách tin mới nhất
            }).addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi lưu tin: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void loadTrendingPlaces() { // Xu hướng tuần này
        //Lấy dữ liệu từ Firestore
        placeRepository.getApprovedPlaces().get()
                .addOnCompleteListener(task -> {
                    if (trendingAdapter != null) {
                        trendingAdapter.setLoading(false);
                    }
                    if (task.isSuccessful() && task.getResult() != null) {
                        trendingList.clear();
                        //Chuyển documents thành objects
                        List<FoodPlace> tempPlaces = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            FoodPlace place = doc.toObject(FoodPlace.class);// document → object
                            if (place != null) {
                                place.setId(doc.getId());// gán ID thủ công (Firestore không tự map)
                                tempPlaces.add(place);
                            }
                        }
                        // Sắp xếp giảm dần theo favoriteCount (nhiều tim nhất lên đầu)
                        Collections.sort(tempPlaces, (p1, p2) -> Integer.compare(p2.getFavoriteCount(), p1.getFavoriteCount()));

                        // Chỉ lấy tối đa 10 quán đầu tiên
                        for (int i = 0; i < Math.min(10, tempPlaces.size()); i++) {
                            trendingList.add(tempPlaces.get(i));
                        }
                        if (trendingAdapter != null) {
                            trendingAdapter.notifyDataSetChanged();
                        }
                    } else {
                        android.util.Log.e("HomeFragment", "Error loading trending places: ", task.getException());
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
                storyAdapter.notifyDataSetChanged();
            }
        });
    }


    private void refreshAllData() {
        if (!isAdded()) return;

        if (categoryAdapter != null) categoryAdapter.setLoading(true);
        if (featuredFoodAdapter != null) featuredFoodAdapter.setLoading(true);
        if (trendingAdapter != null) trendingAdapter.setLoading(true);

        loadFriendsActivity();
        loadFoodCategories();
        loadFeaturedFoodItems();
        loadTrendingPlaces();
        loadUserWelcomeName();
        loadStories();
        checkLocationAndLoadNearby();

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.postDelayed(() -> {
                if (isAdded() && swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }, 1200);
        }
    }

    private void applyPulseAnimation(View view) {
        if (view == null) return;
        android.view.animation.AlphaAnimation pulse = new android.view.animation.AlphaAnimation(0.5f, 1.0f);
        pulse.setDuration(800);
        pulse.setRepeatMode(android.view.animation.Animation.REVERSE);
        pulse.setRepeatCount(android.view.animation.Animation.INFINITE);
        view.startAnimation(pulse);
    }


    private void checkLocationAndLoadNearby() {
        if (!isAdded()) return;

        View rootView = mRootView != null ? mRootView : getView();
        View banner = rootView != null ? rootView.findViewById(R.id.cardLocationPermissionBanner) : null;
        View btnGrant = rootView != null ? rootView.findViewById(R.id.btnGrantLocation) : null;

        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            if (banner != null) {
                banner.setVisibility(View.GONE);
            }
            retrieveLocationAndLoad();
        } else {
            if (banner != null) {
                banner.setVisibility(View.VISIBLE);
                if (btnGrant != null) {
                    btnGrant.setOnClickListener(v -> {
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_PERMISSION_REQUEST);
                    });
                }
            }
            loadFallbackNearbyRestaurant();
        }
    }

    private void retrieveLocationAndLoad() { //lấy vị trí GPS và load quán gần đây
        if (!isAdded()) return;// Fragment còn tồn tại không?
        android.location.LocationManager lm = (android.location.LocationManager)
                requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        if (lm == null) {
            loadFallbackNearbyRestaurant();// GPS không khả dụng → dùng fallback
            return;
        }
        try {
            //Thử lấy vị trí đã biết gần nhất
            // Thử lấy vị trí cuối cùng từ GPS
            android.location.Location last = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            // Nếu GPS không có → thử lấy từ mạng (WiFi/4G)
            if (last == null) last = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);

            //Nếu có vị trí sẵn rồi
            if (last != null) {
                // Dùng luôn tọa độ đã có
                loadNearbyRestaurant(last.getLatitude(), last.getLongitude());
                //Nếu chưa có vị trí nào
            } else {
                // Hiện fallback trước để UI không bị trống
                loadFallbackNearbyRestaurant();
                // Tạo listener chờ GPS trả về vị trí thật
                android.location.LocationListener listener = new android.location.LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull android.location.Location loc) {
                        if (!isAdded())
                            return;
                        // Khi có vị trí thật → load lại quán gần đây
                        loadNearbyRestaurant(loc.getLatitude(), loc.getLongitude());
                    }
                    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                    @Override public void onProviderEnabled(@NonNull String provider) {}
                    @Override public void onProviderDisabled(@NonNull String provider) {}
                };
                // Đăng ký nhận 1 lần vị trí từ GPS hoặc mạng
                if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                    lm.requestSingleUpdate(android.location.LocationManager.GPS_PROVIDER, listener,
                             requireActivity().getMainLooper());
                } else if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    lm.requestSingleUpdate(android.location.LocationManager.NETWORK_PROVIDER, listener,
                             requireActivity().getMainLooper());
                }
            }
        } catch (SecurityException e) {
            loadFallbackNearbyRestaurant();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            View rootView = mRootView != null ? mRootView : getView();
            View banner = rootView != null ? rootView.findViewById(R.id.cardLocationPermissionBanner) : null;
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (banner != null) {
                    banner.setVisibility(View.GONE);
                }
                retrieveLocationAndLoad();
            } else {
                if (banner != null) {
                    banner.setVisibility(View.VISIBLE);
                }
                loadFallbackNearbyRestaurant();
            }
        }
    }

    private void loadNearbyRestaurant(double userLat, double userLon) { // Tìm và hiển thị quán ăn gần nhất dựa trên tọa độ GPS của người dùng.
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
        btnRandomSuggestion.setText("🎲  Đang kết nối...");

        placeRepository.getCachedApprovedPlaces(new PlaceRepository.OnPlacesLoadedCallback() {
            @Override
            public void onLoaded(List<FoodPlace> places) {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (places.isEmpty()) {
                        btnRandomSuggestion.setEnabled(true);
                        btnRandomSuggestion.setText("✨  Không biết ăn gì? Thử vận may!  ✨");
                        Toast.makeText(getContext(), "Chưa có dữ liệu quán ăn", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    startRollingEffect(places);
                });
            }

            @Override
            public void onError() {
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    btnRandomSuggestion.setEnabled(true);
                    btnRandomSuggestion.setText("✨  Không biết ăn gì? Thử vận may!  ✨");
                    Toast.makeText(getContext(), "Lỗi kết nối, thử lại sau", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void startRollingEffect(List<FoodPlace> places) {
        final int rollCount = 15;
        final long rollInterval = 100;
        final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        final int[] count = {0};
        final java.util.Random random = new java.util.Random();

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;

                if (count[0] < rollCount) {
                    FoodPlace randomPlace = places.get(random.nextInt(places.size()));
                    btnRandomSuggestion.setText("🎰  " + randomPlace.getName() + "...");
                    count[0]++;
                    handler.postDelayed(this, rollInterval);
                } else {
                    btnRandomSuggestion.setEnabled(true);
                    btnRandomSuggestion.setText("✨  Không biết ăn gì? Thử vận may!  ✨");

                    List<FoodPlace> shuffled = new ArrayList<>(places);
                    Collections.shuffle(shuffled);
                    showSuggestionDialog(shuffled, 0);
                }
            }
        };
        handler.post(runnable);
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
        private boolean isLoading = true;

        public CategoryAdapter(List<FoodCategory> items) {
            this.items = items;
        }

        public void setLoading(boolean loading) {
            this.isLoading = loading;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_food_category, parent, false);
            return new ViewHolder(v);
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

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (isLoading) {
                holder.tvName.setText("          ");
                holder.tvName.setBackgroundResource(R.drawable.bg_badge_soft);
                holder.ivIcon.setVisibility(View.GONE);
                holder.tvEmoji.setVisibility(View.VISIBLE);
                holder.tvEmoji.setText("⚪");
                applyPulseAnimation(holder.itemView);
                holder.itemView.setOnClickListener(null);
                return;
            }

            holder.itemView.clearAnimation();
            holder.tvName.setBackground(null);

            FoodCategory cat = items.get(position);
            holder.tvName.setText(cat.getName());

            // Always use clean system emoji to avoid AI-generated icons
            holder.ivIcon.setVisibility(View.GONE);
            holder.tvEmoji.setVisibility(View.VISIBLE);
            holder.tvEmoji.setText(getEmojiForCategory(cat.getName()));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), PlacesByCategoryActivity.class);
                intent.putExtra(PlacesByCategoryActivity.EXTRA_CATEGORY_NAME, cat.getName());
                intent.putExtra(PlacesByCategoryActivity.EXTRA_CATEGORY_DESCRIPTION, cat.getDescription());
                // Pass empty string to avoid displaying AI hero icons on detail page
                intent.putExtra(PlacesByCategoryActivity.EXTRA_CATEGORY_ICON_URL, "");
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return isLoading ? 6 : items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvName;
            ImageView ivIcon;
            ViewHolder(View v) {
                super(v);
                tvEmoji = v.findViewById(R.id.tvCategoryEmoji);
                tvName = v.findViewById(R.id.tvCategoryName);
                ivIcon = v.findViewById(R.id.ivCategoryIcon);
            }
        }
    }

    private class FeaturedFoodAdapter extends RecyclerView.Adapter<FeaturedFoodAdapter.ViewHolder> {
        private final List<FoodItem> items;
        private boolean isLoading = true;

        FeaturedFoodAdapter(List<FoodItem> items) {
            this.items = items;
        }

        public void setLoading(boolean loading) {
            this.isLoading = loading;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_featured_food, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (isLoading) {
                holder.tvFeaturedFoodName.setText("                    ");
                holder.tvFeaturedFoodName.setBackgroundResource(R.drawable.bg_badge_soft);
                holder.tvFeaturedTag.setText("      ");
                holder.tvFeaturedTag.setBackgroundResource(R.drawable.bg_badge_soft);
                holder.tvFeaturedFoodRating.setText("★ --");
                holder.ivFeaturedFood.setImageResource(R.drawable.bg_image_placeholder);
                applyPulseAnimation(holder.itemView);
                holder.itemView.setOnClickListener(null);
                return;
            }

            holder.itemView.clearAnimation();
            holder.tvFeaturedFoodName.setBackground(null);
            holder.tvFeaturedTag.setBackgroundResource(R.drawable.bg_tag_orange); // restore original background

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
            return isLoading ? 3 : items.size();
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
        private boolean isLoading = true;

        TrendingPlaceAdapter(List<FoodPlace> items) {
            this.items = items;
        }

        public void setLoading(boolean loading) {
            this.isLoading = loading;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending_place, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (isLoading) {
                holder.tvName.setText("                    ");
                holder.tvName.setBackgroundResource(R.drawable.bg_badge_soft);
                holder.tvStats.setText("                             ");
                holder.tvStats.setBackgroundResource(R.drawable.bg_badge_soft);
                holder.ivImage.setImageResource(R.drawable.bg_image_placeholder);
                applyPulseAnimation(holder.itemView);
                holder.itemView.setOnClickListener(null);
                return;
            }

            holder.itemView.clearAnimation();
            holder.tvName.setBackground(null);
            holder.tvStats.setBackground(null);

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
            return isLoading ? 3 : items.size();
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

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 0 : 1;
        }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_circle, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (getItemViewType(position) == 0) {
                holder.tvName.setText("Thêm tin");
                if (currentUserAvatarUrl != null && !currentUserAvatarUrl.isEmpty()) {
                    Glide.with(holder.ivThumb.getContext())
                            .load(currentUserAvatarUrl)
                            .placeholder(R.drawable.bg_image_placeholder)
                            .error(R.drawable.bg_avatar_orange)
                            .into(holder.ivThumb);
                } else {
                    Glide.with(holder.ivThumb.getContext())
                            .load("https://img.icons8.com/color/96/plus--v1.png")
                            .placeholder(R.drawable.bg_image_placeholder)
                            .into(holder.ivThumb);
                }
                holder.itemView.setOnClickListener(v -> {
                    storyImagePickerLauncher.launch("image/*");
                });
            } else {
                Story story = items.get(position - 1);
                holder.tvName.setText(story.getUserName() != null ? story.getUserName() : "User");
                Glide.with(holder.ivThumb.getContext())
                        .load(story.getImageUrl())
                        .placeholder(R.drawable.bg_image_placeholder)
                        .into(holder.ivThumb);
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), StoryViewerActivity.class);
                    intent.putExtra("START_INDEX", position - 1);
                    startActivity(intent);
                });
            }
        }

        @Override public int getItemCount() { return items.size() + 1; }

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
