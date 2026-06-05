package com.example.cuisine_finder;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.cuisine_finder.activities.FoodPlaceDetailActivity;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Đợi UI render xong rồi tìm View để tránh lỗi null
        view.post(() -> {
            View nearbyItem = view.findViewById(R.id.tvPlaceName);
            if (nearbyItem != null) {
                // Lấy View cha của tvPlaceName (chính là LinearLayout chứa quán ăn)
                View containerLayout = (View) nearbyItem.getParent();
                containerLayout.setOnClickListener(v -> {
                    Intent intent = new Intent(getActivity(), FoodPlaceDetailActivity.class);
                    startActivity(intent);
                });
            }
        });

        return view;
    }
}
