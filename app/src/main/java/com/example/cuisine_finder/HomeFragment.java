package com.example.cuisine_finder;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        EditText etSearch = view.findViewById(R.id.etHomeSearch);
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                navigateToSearchResult(etSearch.getText().toString());
                return true;
            }
            return false;
        });

        return view;
    }

    private void navigateToSearchResult(String query) {
        if (query.isEmpty()) return;
        
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, SearchResultFragment.newInstance(query))
                .addToBackStack(null)
                .commit();
    }
}
