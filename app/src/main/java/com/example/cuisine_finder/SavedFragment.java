package com.example.cuisine_finder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.cuisine_finder.activities.FoodPlaceDetailActivity;
import com.example.cuisine_finder.adapters.FavoriteAdapter;
import com.example.cuisine_finder.models.Favorite;
import com.example.cuisine_finder.repositories.InteractionRepository;
import com.google.firebase.auth.FirebaseAuth;
import java.util.List;

public class SavedFragment extends Fragment {

    private RecyclerView rvSavedPlaces;
    private TextView tvEmptyState;
    private FavoriteAdapter adapter;
    private InteractionRepository interactionRepository;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved, container, false);
        
        interactionRepository = new InteractionRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        initViews(view);
        setupRecyclerView();
        loadFavorites();

        return view;
    }

    private void initViews(View view) {
        rvSavedPlaces = view.findViewById(R.id.rvSavedPlaces);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
    }

    private void setupRecyclerView() {
        adapter = new FavoriteAdapter();
        rvSavedPlaces.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSavedPlaces.setAdapter(adapter);

        adapter.setOnItemClickListener(favorite -> {
            Intent intent = new Intent(getActivity(), FoodPlaceDetailActivity.class);
            intent.putExtra("PLACE_ID", favorite.getPlaceId());
            startActivity(intent);
        });
    }

    private void loadFavorites() {
        if (currentUserId == null) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Vui lòng đăng nhập để xem quán đã lưu.");
            return;
        }

        interactionRepository.getFavoritesByUser(currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<Favorite> favorites = value.toObjects(Favorite.class);
                        adapter.setFavorites(favorites);
                        
                        if (favorites.isEmpty()) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                        }
                    }
                });
    }
}
