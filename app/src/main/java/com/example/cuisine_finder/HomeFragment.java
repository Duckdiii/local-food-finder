package com.example.cuisine_finder;

import android.os.Bundle;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.models.FoodCategory;
import com.example.cuisine_finder.models.FoodItem;
import com.example.cuisine_finder.repositories.CategoryRepository;
import com.example.cuisine_finder.repositories.FoodItemRepository;
import com.example.cuisine_finder.repositories.FriendshipRepository;
import com.example.cuisine_finder.repositories.UserRepository;
import com.example.cuisine_finder.services.AuthService;
import com.example.cuisine_finder.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {
    private static final double FEATURED_MIN_RATING = 4.8d;

    private RecyclerView rvFriends, rvCategories, rvFeaturedFood;
    private FriendsAdapter friendsAdapter;
    private CategoryAdapter categoryAdapter;
    private FeaturedFoodAdapter featuredFoodAdapter;
    private List<User> friendsList = new ArrayList<>();
    private List<FoodCategory> categoriesList = new ArrayList<>();
    private List<FoodItem> featuredFoodList = new ArrayList<>();
    private AuthService authService;
    private FriendshipRepository friendshipRepository;
    private UserRepository userRepository;
    private CategoryRepository categoryRepository;
    private FoodItemRepository foodItemRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        authService = new AuthService();
        friendshipRepository = new FriendshipRepository();
        userRepository = new UserRepository();
        categoryRepository = new CategoryRepository();
        foodItemRepository = new FoodItemRepository();

        rvFriends = view.findViewById(R.id.rvFriendsEating);
        rvCategories = view.findViewById(R.id.rvFoodCategories);
        rvFeaturedFood = view.findViewById(R.id.rvFeaturedFood);
        
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
    }

    private void loadFriendsActivity() {
        if (!authService.isLoggedIn()) return;

        String currentUserId = authService.getCurrentUser().getUid();
        
        friendshipRepository.getFriendsByRequester(currentUserId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                friendsList.clear();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String friendId = doc.getString("receiverId");
                    if (friendId != null) {
                        fetchFriendProfile(friendId);
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
            // For emoji, we can use a default or randomize for demo
            String[] emojis = {"🧑‍🍳", "🍕", "🍜", "🍔", "🍣"};
            holder.tvEmoji.setText(emojis[position % emojis.length]);
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
            // Map common names to emojis for demo
            String emoji = "🍲";
            String name = cat.getName().toLowerCase();
            if (name.contains("coffee") || name.contains("cà phê")) emoji = "☕";
            else if (name.contains("bún")) emoji = "🍜";
            else if (name.contains("cơm")) emoji = "🍚";
            else if (name.contains("lẩu")) emoji = "🍲";
            else if (name.contains("ốc")) emoji = "🐚";
            
            holder.tvEmoji.setText(emoji);
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
            String foodName = foodItem.getName() != null ? foodItem.getName() : "Mon noi bat";
            String featuredTag = foodItem.getCategoryName() != null && !foodItem.getCategoryName().trim().isEmpty()
                    ? foodItem.getCategoryName()
                    : "Noi bat";

            holder.tvFeaturedFoodName.setText(foodName);
            holder.tvFeaturedTag.setText(featuredTag);
            holder.tvFeaturedFoodRating.setText(String.format(Locale.getDefault(), "\u2605 %.1f", foodItem.getAverageRating()));
            holder.ivFeaturedFood.setImageResource(R.drawable.bg_image_placeholder);
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
}


