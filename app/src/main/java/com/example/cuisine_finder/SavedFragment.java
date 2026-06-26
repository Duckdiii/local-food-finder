package com.example.cuisine_finder;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.activities.ExploredPlacesActivity;
import com.example.cuisine_finder.activities.FoodPlaceDetailActivity;
import com.example.cuisine_finder.adapters.FavoriteAdapter;
import com.example.cuisine_finder.models.Favorite;
import com.example.cuisine_finder.repositories.InteractionRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SavedFragment extends Fragment {

    private enum TabFilter { ALL, RECENT, ALPHA, NIGHT }

    private RecyclerView rvSavedPlaces;
    private LinearLayout layoutEmptyState;
    private TextView tvSavedCount;
    private TextView tvStatSavedNum;
    private MaterialCardView chipTabAll, chipTabRecent, chipTabAlpha, chipTabNight;
    private TextView tvChipTabAll, tvChipTabRecent, tvChipTabAlpha, tvChipTabNight;
    private FavoriteAdapter adapter;
    private InteractionRepository interactionRepository;
    private String currentUserId;

    private final List<Favorite> allFavorites = new ArrayList<>();
    private TabFilter currentTab = TabFilter.ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved, container, false);

        interactionRepository = new InteractionRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        initViews(view);
        setupRecyclerView();
        setupTabs();
        setupExploredCard(view);
        loadFavorites();

        return view;
    }

    private void initViews(View view) {
        rvSavedPlaces = view.findViewById(R.id.rvSavedPlaces);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        tvSavedCount = view.findViewById(R.id.tvSavedCount);
        tvStatSavedNum = view.findViewById(R.id.tvStatSavedNum);
        chipTabAll = view.findViewById(R.id.chipTabAll);
        chipTabRecent = view.findViewById(R.id.chipTabRecent);
        chipTabAlpha = view.findViewById(R.id.chipTabAlpha);
        chipTabNight = view.findViewById(R.id.chipTabNight);
        tvChipTabAll = view.findViewById(R.id.tvChipTabAll);
        tvChipTabRecent = view.findViewById(R.id.tvChipTabRecent);
        tvChipTabAlpha = view.findViewById(R.id.tvChipTabAlpha);
        tvChipTabNight = view.findViewById(R.id.tvChipTabNight);
    }

    private void setupRecyclerView() {
        adapter = new FavoriteAdapter();
        rvSavedPlaces.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSavedPlaces.setAdapter(adapter);

        adapter.setOnItemClickListener(favorite -> {
            Intent intent = new Intent(getActivity(), FoodPlaceDetailActivity.class);
            intent.putExtra(FoodPlaceDetailActivity.EXTRA_PLACE_ID, favorite.getPlaceId());
            startActivity(intent);
        });

        adapter.setOnRemoveListener(favorite -> {
            if (currentUserId == null) return;
            adapter.removeItem(favorite);
            allFavorites.remove(favorite);
            updateCountDisplay();
            interactionRepository.removeFavorite(currentUserId, favorite.getPlaceId())
                    .addOnSuccessListener(v ->
                            Toast.makeText(getContext(), "Đã bỏ khỏi yêu thích", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e -> {
                        allFavorites.add(0, favorite);
                        applyTab();
                        Toast.makeText(getContext(), "Có lỗi xảy ra", Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void setupTabs() {
        chipTabAll.setOnClickListener(v -> selectTab(TabFilter.ALL));
        chipTabRecent.setOnClickListener(v -> selectTab(TabFilter.RECENT));
        chipTabAlpha.setOnClickListener(v -> selectTab(TabFilter.ALPHA));
        chipTabNight.setOnClickListener(v -> selectTab(TabFilter.NIGHT));
        renderTabState();
    }

    private void selectTab(TabFilter tab) {
        currentTab = tab;
        renderTabState();
        applyTab();
    }

    private void renderTabState() {
        setTabActive(chipTabAll, tvChipTabAll, currentTab == TabFilter.ALL);
        setTabActive(chipTabRecent, tvChipTabRecent, currentTab == TabFilter.RECENT);
        setTabActive(chipTabAlpha, tvChipTabAlpha, currentTab == TabFilter.ALPHA);
        setTabActive(chipTabNight, tvChipTabNight, currentTab == TabFilter.NIGHT);
    }

    private void setTabActive(MaterialCardView chip, TextView label, boolean active) {
        chip.setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                active ? R.color.orange_main : R.color.white));
        chip.setStrokeWidth(active ? 0 : (int) (requireContext().getResources().getDisplayMetrics().density));
        label.setTextColor(ContextCompat.getColor(requireContext(),
                active ? R.color.white : R.color.text_dark));
        label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void setupExploredCard(View view) {
        View cardExplored = view.findViewById(R.id.cardExplored);
        if (cardExplored != null) {
            cardExplored.setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), ExploredPlacesActivity.class)));
        }
    }

    private void applyTab() {
        List<Favorite> result = new ArrayList<>(allFavorites);
        long thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;

        switch (currentTab) {
            case RECENT:
                result.removeIf(f -> f.getCreatedAt() < thirtyDaysAgo);
                result.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
            case ALPHA:
                result.sort(Comparator.comparing(
                        f -> f.getPlaceName() != null ? f.getPlaceName() : "",
                        String.CASE_INSENSITIVE_ORDER));
                break;
            case NIGHT:
                result.removeIf(f -> !f.isOpenLate());
                break;
            case ALL:
            default:
                result.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                break;
        }

        adapter.setFavorites(result);
        updateEmptyAndCount(result.size());
    }

    private void loadFavorites() {
        if (currentUserId == null) {
            showEmptyState();
            tvSavedCount.setText("Đăng nhập để xem quán đã lưu");
            return;
        }

        interactionRepository.getFavoritesByUser(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (!isAdded()) return;
                    if (value != null) {
                        allFavorites.clear();
                        allFavorites.addAll(value.toObjects(Favorite.class));
                        applyTab();
                    }
                });
    }

    private void updateCountDisplay() {
        int count = allFavorites.size();
        if (tvStatSavedNum != null) tvStatSavedNum.setText(String.valueOf(count));
        if (count == 0) {
            showEmptyState();
            tvSavedCount.setText("Các quán đã lưu của bạn");
        } else {
            hideEmptyState();
            tvSavedCount.setText(count + " quán đã lưu");
        }
    }

    private void updateEmptyAndCount(int visibleCount) {
        int totalCount = allFavorites.size();
        if (tvStatSavedNum != null) tvStatSavedNum.setText(String.valueOf(totalCount));
        if (totalCount == 0) {
            showEmptyState();
            tvSavedCount.setText("Các quán đã lưu của bạn");
        } else {
            hideEmptyState();
            tvSavedCount.setText(totalCount + " quán đã lưu");
            if (visibleCount == 0) showEmptyState();
            else hideEmptyState();
        }
    }

    private void showEmptyState() {
        layoutEmptyState.setVisibility(View.VISIBLE);
        rvSavedPlaces.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        layoutEmptyState.setVisibility(View.GONE);
        rvSavedPlaces.setVisibility(View.VISIBLE);
    }
}
