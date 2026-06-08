package com.example.cuisine_finder;

import android.location.Address;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import java.util.ArrayList;
import java.util.List;

public class SearchResultFragment extends Fragment {

    private String initialQuery;
    private EditText etSearchBox;
    private RecyclerView rvSearchResults;
    private SearchResultAdapter adapter;
    
    private List<Object> displayList = new ArrayList<>();
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_DISH = 1;
    private static final int TYPE_RESTAURANT = 2;
    private static final int TYPE_SEPARATOR = 3;

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
            initialQuery = getArguments().getString("query");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search_result, container, false);

        etSearchBox = view.findViewById(R.id.etSearchBox);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        etSearchBox.setText(initialQuery);
        setupRecyclerView();
        setupSearch();

        performSearch(initialQuery);

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

    private void performSearch(String query) {
        if (query == null || query.isEmpty()) return;

        new Thread(() -> {
            try {
                // Mock search for Dishes (Món ăn)
                List<String> mockDishes = new ArrayList<>();
                if ("phở".equalsIgnoreCase(query) || query.toLowerCase().contains("phở")) {
                    mockDishes.add("Phở Bò Tái Lăn");
                    mockDishes.add("Phở Gà Lá Chanh");
                } else if ("bún".equalsIgnoreCase(query) || query.toLowerCase().contains("bún")) {
                    mockDishes.add("Bún Bò Huế Đặc Biệt");
                    mockDishes.add("Bún Chả Hà Nội");
                } else {
                    mockDishes.add(query + " Thơm Ngon");
                }

                // Real search for Restaurants (Quán ăn)
                GeocoderNominatim geocoder = new GeocoderNominatim("CuisineFinder/1.0");
                List<Address> addresses = geocoder.getFromLocationName(query, 5);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        displayList.clear();
                        
                        // Add Dishes section
                        displayList.add("Món ăn"); // Header
                        displayList.addAll(mockDishes);
                        
                        // Add Separator
                        displayList.add(new Separator());
                        
                        // Add Restaurants section
                        displayList.add("Quán ăn"); // Header
                        if (addresses != null) {
                            displayList.addAll(addresses);
                        }
                        
                        adapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }

    private static class Separator {}

    private class SearchResultAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemViewType(int position) {
            Object item = displayList.get(position);
            if (item instanceof String) return TYPE_HEADER;
            if (item instanceof Address) return TYPE_RESTAURANT;
            if (item instanceof Separator) return TYPE_SEPARATOR;
            return TYPE_DISH;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                return new HeaderViewHolder(inflater.inflate(R.layout.item_search_header, parent, false));
            } else if (viewType == TYPE_SEPARATOR) {
                return new SeparatorViewHolder(inflater.inflate(R.layout.view_separator, parent, false));
            } else {
                return new ItemViewHolder(inflater.inflate(R.layout.item_search_result, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Object item = displayList.get(position);
            
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).tvTitle.setText((String) item);
            } else if (holder instanceof ItemViewHolder) {
                ItemViewHolder itemHolder = (ItemViewHolder) holder;
                if (item instanceof String) {
                    // It's a Dish
                    itemHolder.tvName.setText((String) item);
                    itemHolder.tvAddress.setText("Món ăn phổ biến");
                } else if (item instanceof Address) {
                    // It's a Restaurant
                    Address addr = (Address) item;
                    itemHolder.tvName.setText(addr.getFeatureName());
                    itemHolder.tvAddress.setText(addr.getAddressLine(0));
                }
            }
        }

        @Override
        public int getItemCount() {
            return displayList.size();
        }

        class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            HeaderViewHolder(View v) { super(v); tvTitle = v.findViewById(R.id.tvHeaderTitle); }
        }

        class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvAddress;
            ItemViewHolder(View v) { 
                super(v); 
                tvName = v.findViewById(R.id.tvPlaceName);
                tvAddress = v.findViewById(R.id.tvPlaceAddress);
            }
        }
        
        class SeparatorViewHolder extends RecyclerView.ViewHolder {
            SeparatorViewHolder(View v) { super(v); }
        }
    }
}
