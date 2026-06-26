package com.example.cuisine_finder;

import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.cuisine_finder.activities.FoodPlaceDetailActivity;
import com.example.cuisine_finder.activities.CommunityChatRoomsActivity;
import com.example.cuisine_finder.adapters.CommunityCommentAdapter;
import com.example.cuisine_finder.adapters.CommunityPostAdapter;
import com.example.cuisine_finder.adapters.ExistingPlacePickerAdapter;
import com.example.cuisine_finder.models.CommunityComment;
import com.example.cuisine_finder.models.CommunityPost;
import com.example.cuisine_finder.models.FoodPlace;
import com.example.cuisine_finder.repositories.CommunityRepository;
import com.example.cuisine_finder.repositories.PlaceRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommunityFragment extends Fragment {
    private static final int MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;

    private enum FeedFilter {
        FEATURED,
        LATEST,
        NEARBY,
        NIGHT
    }

    private CommunityRepository communityRepository;
    private CommunityPostAdapter postAdapter;
    private ListenerRegistration postsRegistration;
    private final List<CommunityPost> allPosts = new ArrayList<>();
    private final java.util.Map<String, FoodPlace> placesCache = new java.util.HashMap<>();
    private PlaceRepository placeRepository;

    private TextView tvCurrentDistrictRoom;
    private TextView tvCommunityEmpty;
    private View btnFeaturedTab;
    private TextView tvFeaturedTab;
    private TextView btnSwitchRoomTab;
    private TextView btnNearbyRoom;
    private TextView btnNightRoom;
    private View tabFeaturedUnderline;

    private FeedFilter currentFilter = FeedFilter.FEATURED;
    private String searchQuery = "";
    private String currentUserId;

    private Uri selectedPostImageUri;
    private LinearLayout layoutSelectedImage;
    private ImageView ivSelectedPostImage;
    private TextView tvSelectedImageName;
    private TextView btnSubmitPost;
    private EditText etPostCaption;
    private ActivityResultLauncher<String> postImagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        postImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    long size = getUriSize(uri);
                    if (size > MAX_IMAGE_SIZE_BYTES) {
                        selectedPostImageUri = null;
                        Toast.makeText(requireContext(), "Ảnh phải nhỏ hơn hoặc bằng 5MB", Toast.LENGTH_SHORT).show();
                        updateSelectedImagePreview();
                        updateSubmitPostButton();
                        return;
                    }

                    selectedPostImageUri = uri;
                    updateSelectedImagePreview();
                    updateSubmitPostButton();
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_community, container, false);
        communityRepository = new CommunityRepository();
        placeRepository = new PlaceRepository();
        currentUserId = FirebaseAuth.getInstance().getUid();

        initViews(view);
        setupRecyclerView(view);
        setupActions(view);

        placeRepository.getCachedApprovedPlaces(new PlaceRepository.OnPlacesLoadedCallback() {
            @Override
            public void onLoaded(List<FoodPlace> places) {
                if (!isAdded()) return;
                placesCache.clear();
                if (places != null) {
                    for (FoodPlace p : places) {
                        placesCache.put(p.getId(), p);
                    }
                }
                applyFilter();
            }

            @Override
            public void onError() {}
        });

        startListeningPosts();
        return view;
    }

    @Override
    public void onDestroyView() {
        if (postsRegistration != null) {
            postsRegistration.remove();
            postsRegistration = null;
        }
        super.onDestroyView();
    }

    private void initViews(View view) {
        tvCurrentDistrictRoom = view.findViewById(R.id.tvCurrentDistrictRoom);
        tvCommunityEmpty = view.findViewById(R.id.tvCommunityEmpty);
        btnFeaturedTab = view.findViewById(R.id.btnFeaturedTab);
        tvFeaturedTab = view.findViewById(R.id.tvFeaturedTab);
        btnSwitchRoomTab = view.findViewById(R.id.btnSwitchRoomTab);
        btnNearbyRoom = view.findViewById(R.id.btnNearbyRoom);
        btnNightRoom = view.findViewById(R.id.btnNightRoom);
        tabFeaturedUnderline = view.findViewById(R.id.tabFeaturedUnderline);
        setActiveTab(FeedFilter.FEATURED);
    }

    private void setupRecyclerView(View view) {
        RecyclerView rvCommunityPosts = view.findViewById(R.id.rvCommunityPosts);
        postAdapter = new CommunityPostAdapter(new CommunityPostAdapter.OnPostActionListener() {
            @Override
            public void onLikeClicked(CommunityPost post) {
                handleLike(post);
            }

            @Override
            public void onCommentClicked(CommunityPost post) {
                showComments(post);
            }

            @Override
            public void onShareClicked(CommunityPost post) {
                sharePost(post);
            }

            @Override
            public void onPlaceClicked(CommunityPost post) {
                openPlace(post);
            }

            @Override
            public void onTagClicked(String tag) {
                searchQuery = tag;
                currentFilter = FeedFilter.FEATURED;
                setActiveTab(currentFilter);
                applyFilter();
            }

            @Override
            public void onReportClicked(CommunityPost post, View anchorView) {
                showPostOptionsMenu(post, anchorView);
            }
        });
        postAdapter.setCurrentUserId(currentUserId);
        rvCommunityPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCommunityPosts.setAdapter(postAdapter);
    }

    private void setupActions(View view) {
        view.findViewById(R.id.btnCommunitySearch).setOnClickListener(v -> showSearchDialog());
        view.findViewById(R.id.btnCommunityCompose).setOnClickListener(v -> showCreatePostDialog());
        view.findViewById(R.id.btnCommunityChatRooms).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CommunityChatRoomsActivity.class)));

        view.findViewById(R.id.btnFeaturedTab).setOnClickListener(v -> {
            currentFilter = FeedFilter.FEATURED;
            setActiveTab(currentFilter);
            applyFilter();
        });

        btnSwitchRoomTab.setOnClickListener(v -> {
            currentFilter = FeedFilter.LATEST;
            setActiveTab(currentFilter);
            applyFilter();
        });

        btnNearbyRoom.setOnClickListener(v -> {
            currentFilter = FeedFilter.NEARBY;
            setActiveTab(currentFilter);
            applyFilter();
        });

        btnNightRoom.setOnClickListener(v -> {
            currentFilter = FeedFilter.NIGHT;
            setActiveTab(currentFilter);
            applyFilter();
        });
    }

    private void startListeningPosts() {
        tvCommunityEmpty.setVisibility(View.VISIBLE);
        tvCommunityEmpty.setText("Đang tải bài viết...");
        communityRepository.removeLegacyMockPosts()
                .addOnCompleteListener(task -> {
                    if (isAdded()) {
                        listenToRealPosts();
                    }
                });
    }

    private void listenToRealPosts() {
        postsRegistration = communityRepository.listenPosts(new CommunityRepository.PostsListener() {
            @Override
            public void onPostsChanged(List<CommunityPost> posts) {
                allPosts.clear();
                allPosts.addAll(posts);
                applyFilter();
            }

            @Override
            public void onError(Exception error) {
                if (!isAdded()) return;
                tvCommunityEmpty.setVisibility(View.VISIBLE);
                tvCommunityEmpty.setText("Không tải được bài viết cộng đồng");
                Toast.makeText(requireContext(), "Lỗi tải bài viết: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilter() {
        List<CommunityPost> visiblePosts = new ArrayList<>();
        String normalizedQuery = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.getDefault());

        android.location.Location userLoc = getUserLocation();
        for (CommunityPost post : allPosts) {
            if (!matchesSearch(post, normalizedQuery)) continue;
            if (currentFilter == FeedFilter.NIGHT && !isNightPost(post)) continue;

            // Calculate dynamic GPS distance if available
            FoodPlace place = placesCache.get(post.getPlaceId());
            if (place != null && userLoc != null) {
                double dist = calculateDistanceKm(userLoc, place);
                post.setDistanceKm(dist);
            } else if (post.getDistanceKm() <= 0) {
                post.setDistanceKm(Double.MAX_VALUE);
            }

            visiblePosts.add(post);
        }

        if (currentFilter == FeedFilter.FEATURED) {
            visiblePosts.sort((left, right) -> Integer.compare(
                    right.getLikeCount() + right.getCommentCount(),
                    left.getLikeCount() + left.getCommentCount()
            ));
        } else if (currentFilter == FeedFilter.LATEST) {
            visiblePosts.sort((left, right) -> Long.compare(right.getCreatedAt(), left.getCreatedAt()));
        } else if (currentFilter == FeedFilter.NEARBY) {
            visiblePosts.sort(Comparator.comparingDouble(post ->
                    post.getDistanceKm() > 0 ? post.getDistanceKm() : Double.MAX_VALUE
            ));
        }

        postAdapter.setPosts(visiblePosts);
        updateEmptyState(visiblePosts.isEmpty());
        updateSubtitle(normalizedQuery);
    }

    private boolean matchesSearch(CommunityPost post, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) return true;
        StringBuilder searchable = new StringBuilder();
        appendSearchText(searchable, post.getCaption());
        appendSearchText(searchable, post.getAuthorName());
        appendSearchText(searchable, post.getPlaceName());
        appendSearchText(searchable, post.getPlaceAddress());
        if (post.getTags() != null) {
            for (String tag : post.getTags()) appendSearchText(searchable, tag);
        }
        return searchable.toString().toLowerCase(Locale.getDefault()).contains(normalizedQuery);
    }

    private void appendSearchText(StringBuilder builder, String value) {
        if (!TextUtils.isEmpty(value)) {
            builder.append(' ').append(value);
        }
    }

    private boolean isNightPost(CommunityPost post) {
        if (post.isOpenLate()) return true;
        if (post.getTags() == null) return false;
        for (String tag : post.getTags()) {
            String normalized = tag == null ? "" : tag.toLowerCase(Locale.getDefault());
            if (normalized.contains("ankhuya") || normalized.contains("demkhuya")) {
                return true;
            }
        }
        return false;
    }

    private void updateEmptyState(boolean empty) {
        tvCommunityEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (!empty) return;
        if (!TextUtils.isEmpty(searchQuery)) {
            tvCommunityEmpty.setText("Không tìm thấy kết quả cho '" + searchQuery + "'");
        } else {
            tvCommunityEmpty.setText("Chưa có bài viết nào");
        }
    }

    private void updateSubtitle(String normalizedQuery) {
        if (!TextUtils.isEmpty(normalizedQuery)) {
            tvCurrentDistrictRoom.setText("Đang lọc: " + searchQuery);
            return;
        }
        switch (currentFilter) {
            case LATEST:
                tvCurrentDistrictRoom.setText("bài mới nhất");
                break;
            case NEARBY:
                tvCurrentDistrictRoom.setText("món ngon gần tui");
                break;
            case NIGHT:
                tvCurrentDistrictRoom.setText("quán ăn đêm khuya");
                break;
            case FEATURED:
            default:
                tvCurrentDistrictRoom.setText("món ngon Sài Gòn");
                break;
        }
    }

    private void setActiveTab(FeedFilter filter) {
        setChipState(btnFeaturedTab, tvFeaturedTab, filter == FeedFilter.FEATURED);
        setChipState(btnSwitchRoomTab, btnSwitchRoomTab, filter == FeedFilter.LATEST);
        setChipState(btnNearbyRoom, btnNearbyRoom, filter == FeedFilter.NEARBY);
        setChipState(btnNightRoom, btnNightRoom, filter == FeedFilter.NIGHT);
        tabFeaturedUnderline.setVisibility(View.GONE);
    }

    private void setChipState(View chipView, TextView textView, boolean active) {
        chipView.setBackgroundResource(active ? R.drawable.bg_community_tab_selected : R.drawable.bg_community_tab_unselected);
        if (active) {
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange_main));
        } else {
            textView.setTextColor(0xCCFFFFFF); // semi-transparent white on gradient header
        }
        textView.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void handleLike(CommunityPost post) {
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để thả tim", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean nextLikedState = !post.isLikedBy(currentUserId);
        postAdapter.applyOptimisticLike(post.getId(), currentUserId, nextLikedState);
        communityRepository.toggleLike(post.getId(), currentUserId)
                .addOnFailureListener(error -> {
                    postAdapter.applyOptimisticLike(post.getId(), currentUserId, !nextLikedState);
                    Toast.makeText(requireContext(), "Không cập nhật được lượt thích", Toast.LENGTH_SHORT).show();
                });
    }

    private void showPostOptionsMenu(CommunityPost post, View anchorView) {
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để thực hiện tác vụ này", Toast.LENGTH_SHORT).show();
            return;
        }

        android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), anchorView);
        boolean isAuthor = currentUserId.equals(post.getAuthorId());

        if (isAuthor) {
            popup.getMenu().add("Xóa bài viết");
        } else {
            popup.getMenu().add("Báo cáo vi phạm");
        }

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if ("Xóa bài viết".equals(title)) {
                new AlertDialog.Builder(requireActivity())
                        .setTitle("Xóa bài viết?")
                        .setMessage("Bạn có chắc chắn muốn xóa bài viết này không?")
                        .setPositiveButton("Xóa", (dialog, which) -> {
                            communityRepository.deletePost(post.getId())
                                    .addOnSuccessListener(unused -> Toast.makeText(requireContext(), "Đã xóa bài viết thành công", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(requireContext(), "Không thể xóa bài viết: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
                return true;
            } else if ("Báo cáo vi phạm".equals(title)) {
                showReportPostDialog(post);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showReportPostDialog(CommunityPost post) {
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để báo cáo bài viết", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUserId.equals(post.getAuthorId())) {
            Toast.makeText(requireContext(), "Bạn không thể báo cáo bài viết của chính mình", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = {"Spam", "Nội dung không phù hợp", "Thông tin sai lệch"};
        String[] reasons = {"spam", "inappropriate", "misinformation"};
        new AlertDialog.Builder(requireActivity())
                .setTitle("Báo cáo bài viết")
                .setItems(labels, (dialog, which) ->
                        communityRepository.reportPost(post.getId(), currentUserId, reasons[which])
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(requireContext(), "Đã gửi báo cáo bài viết", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(error ->
                                        Toast.makeText(requireContext(), "Không thể báo cáo bài viết", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void showComments(CommunityPost post) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_community_comments, null);
        RecyclerView rvComments = view.findViewById(R.id.rvCommunityComments);
        TextView tvCommentsEmpty = view.findViewById(R.id.tvCommentsEmpty);
        EditText etComment = view.findViewById(R.id.etCommunityComment);
        TextView btnSend = view.findViewById(R.id.btnSendCommunityComment);

        CommunityCommentAdapter commentAdapter = new CommunityCommentAdapter();
        rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComments.setAdapter(commentAdapter);
        setButtonEnabled(btnSend, false);

        etComment.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                setButtonEnabled(btnSend, editable.toString().trim().length() > 0);
            }
        });

        final ListenerRegistration[] commentsRegistration = new ListenerRegistration[1];
        commentsRegistration[0] = communityRepository.listenComments(post.getId(), new CommunityRepository.CommentsListener() {
            @Override
            public void onCommentsChanged(List<CommunityComment> comments) {
                commentAdapter.setComments(comments);
                tvCommentsEmpty.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(Exception error) {
                Toast.makeText(requireContext(), "Không tải được bình luận", Toast.LENGTH_SHORT).show();
            }
        });

        btnSend.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
                return;
            }
            String content = etComment.getText().toString().trim();
            if (content.isEmpty()) return;

            setButtonEnabled(btnSend, false);
            CommunityComment comment = new CommunityComment();
            comment.setAuthorId(currentUserId);
            comment.setAuthorName(getCurrentUserName());
            comment.setAuthorAvatarUrl(getCurrentUserAvatar());
            comment.setContent(content);
            communityRepository.addComment(post.getId(), comment)
                    .addOnSuccessListener(unused -> etComment.setText(""))
                    .addOnFailureListener(error -> {
                        setButtonEnabled(btnSend, true);
                        Toast.makeText(requireContext(), "Gửi bình luận thất bại", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.setOnDismissListener(d -> {
            if (commentsRegistration[0] != null) {
                commentsRegistration[0].remove();
            }
        });
        dialog.setContentView(view);
        dialog.show();
    }

    private void showSearchDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_community_search, null);
        EditText input = view.findViewById(R.id.etCommunitySearch);
        TextView btnClear = view.findViewById(R.id.btnClearCommunitySearch);
        TextView btnReset = view.findViewById(R.id.btnResetCommunitySearch);
        TextView btnApply = view.findViewById(R.id.btnApplyCommunitySearch);

        input.setText(searchQuery);
        input.setSelection(input.getText().length());
        btnClear.setOnClickListener(v -> input.setText(""));
        btnReset.setOnClickListener(v -> {
            searchQuery = "";
            applyFilter();
            dialog.dismiss();
        });
        btnApply.setOnClickListener(v -> {
            searchQuery = input.getText().toString().trim();
            applyFilter();
            dialog.dismiss();
        });

        bindSearchChip(view, R.id.chipSearchNight, "#AnKhuya", input);
        bindSearchChip(view, R.id.chipSearchNear, "#ThuDuc", input);
        bindSearchChip(view, R.id.chipSearchBunBo, "#BunBo", input);
        bindSearchChip(view, R.id.chipSearchRecommend, "#XinGoiY", input);

        dialog.setContentView(view);
        dialog.show();
        input.requestFocus();
    }

    private void bindSearchChip(View root, int chipId, String value, EditText input) {
        root.findViewById(chipId).setOnClickListener(v -> input.setText(value));
    }

    private void showCreatePostDialog() {
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để tạo bài viết", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedPostImageUri = null;
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_create_community_post, null);
        etPostCaption = view.findViewById(R.id.etPostCaption);
        EditText etPostPlaceName = view.findViewById(R.id.etPostPlaceName);
        EditText etPostAddress = view.findViewById(R.id.etPostAddress);
        EditText etPostTags = view.findViewById(R.id.etPostTags);
        TextView btnExistingOption = view.findViewById(R.id.btnExistingPlaceOption);
        TextView btnNewOption = view.findViewById(R.id.btnNewPlaceOption);
        TextView btnChooseExisting = view.findViewById(R.id.btnChooseExistingPlace);
        TextView tvSelectedExisting = view.findViewById(R.id.tvSelectedExistingPlace);
        View layoutExisting = view.findViewById(R.id.layoutExistingPlace);
        View layoutNew = view.findViewById(R.id.layoutNewPlace);
        TextView btnPickPostImage = view.findViewById(R.id.btnPickPostImage);
        btnSubmitPost = view.findViewById(R.id.btnSubmitPost);
        TextView btnCancelPost = view.findViewById(R.id.btnCancelPost);
        layoutSelectedImage = view.findViewById(R.id.layoutSelectedImage);
        ivSelectedPostImage = view.findViewById(R.id.ivSelectedPostImage);
        tvSelectedImageName = view.findViewById(R.id.tvSelectedImageName);
        TextView btnRemovePostImage = view.findViewById(R.id.btnRemovePostImage);
        btnRemovePostImage.setOnClickListener(v -> {
            selectedPostImageUri = null;
            updateSelectedImagePreview();
            updateSubmitPostButton();
        });
        final boolean[] useExistingPlace = {true};
        final FoodPlace[] selectedExistingPlace = {null};

        setButtonEnabled(btnSubmitPost, false);
        etPostCaption.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                updateSubmitPostButton();
            }
        });

        btnPickPostImage.setOnClickListener(v -> postImagePickerLauncher.launch("image/*"));
        btnExistingOption.setOnClickListener(v -> {
            useExistingPlace[0] = true;
            setPlaceOptionState(btnExistingOption, btnNewOption, layoutExisting, layoutNew, true);
        });
        btnNewOption.setOnClickListener(v -> {
            useExistingPlace[0] = false;
            setPlaceOptionState(btnExistingOption, btnNewOption, layoutExisting, layoutNew, false);
        });
        btnChooseExisting.setOnClickListener(v ->
                showExistingPlacePicker(selectedExistingPlace, tvSelectedExisting));
        btnCancelPost.setOnClickListener(v -> {
            if (hasDraft(etPostCaption, etPostPlaceName, etPostAddress, etPostTags)) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Bỏ bài viết này?")
                        .setMessage("Nội dung đang nhập sẽ không được lưu.")
                        .setPositiveButton("Bỏ", (confirmDialog, which) -> dialog.dismiss())
                        .setNegativeButton("Tiếp tục viết", null)
                        .show();
            } else {
                dialog.dismiss();
            }
        });

        btnSubmitPost.setOnClickListener(v -> submitPost(
                dialog,
                etPostPlaceName,
                etPostAddress,
                etPostTags,
                useExistingPlace[0],
                selectedExistingPlace[0]
        ));

        dialog.setOnDismissListener(d -> {
            selectedPostImageUri = null;
            layoutSelectedImage = null;
            ivSelectedPostImage = null;
            tvSelectedImageName = null;
            btnSubmitPost = null;
            etPostCaption = null;
        });
        dialog.setContentView(view);
        dialog.show();
    }

    private void setPlaceOptionState(
            TextView btnExisting,
            TextView btnNew,
            View layoutExisting,
            View layoutNew,
            boolean useExisting
    ) {
        btnExisting.setBackgroundResource(
                useExisting ? R.drawable.bg_community_tab_active : R.drawable.bg_community_tab_inactive
        );
        btnNew.setBackgroundResource(
                useExisting ? R.drawable.bg_community_tab_inactive : R.drawable.bg_community_tab_active
        );
        btnExisting.setTextColor(ContextCompat.getColor(
                requireContext(), useExisting ? R.color.orange_main : R.color.text_gray
        ));
        btnNew.setTextColor(ContextCompat.getColor(
                requireContext(), useExisting ? R.color.text_gray : R.color.orange_main
        ));
        btnExisting.setTypeface(null, useExisting ? Typeface.BOLD : Typeface.NORMAL);
        btnNew.setTypeface(null, useExisting ? Typeface.NORMAL : Typeface.BOLD);
        layoutExisting.setVisibility(useExisting ? View.VISIBLE : View.GONE);
        layoutNew.setVisibility(useExisting ? View.GONE : View.VISIBLE);
    }

    private void showExistingPlacePicker(FoodPlace[] selectedPlaceHolder, TextView selectedPlaceText) {
        selectedPlaceText.setText("Đang tải danh sách quán...");
        new PlaceRepository().getAllPlaces().get()
                .addOnSuccessListener(snapshot -> {
                    List<FoodPlace> places = new ArrayList<>();
                    snapshot.getDocuments().forEach(document -> {
                        FoodPlace place = document.toObject(FoodPlace.class);
                        if (place != null) {
                            place.setId(document.getId());
                            places.add(place);
                        }
                    });

                    if (places.isEmpty()) {
                        selectedPlaceText.setText("Chưa có quán nào trên app");
                        Toast.makeText(requireContext(), "Chưa có quán để lựa chọn", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    showPlacePickerDialog(places, selectedPlaceHolder, selectedPlaceText);
                })
                .addOnFailureListener(error -> {
                    selectedPlaceText.setText("Không tải được danh sách quán");
                    Toast.makeText(requireContext(), "Không tải được danh sách quán", Toast.LENGTH_SHORT).show();
                });
    }

    private void showPlacePickerDialog(
            List<FoodPlace> places,
            FoodPlace[] selectedPlaceHolder,
            TextView selectedPlaceText
    ) {
        View pickerView = getLayoutInflater().inflate(R.layout.dialog_existing_place_picker, null);
        EditText searchInput = pickerView.findViewById(R.id.etSearchExistingPlace);
        TextView resultCount = pickerView.findViewById(R.id.tvPlacePickerCount);
        TextView emptyState = pickerView.findViewById(R.id.tvPlacePickerEmpty);
        RecyclerView placesView = pickerView.findViewById(R.id.rvExistingPlaces);
        AlertDialog pickerDialog = new AlertDialog.Builder(requireContext())
                .setView(pickerView)
                .create();

        ExistingPlacePickerAdapter pickerAdapter = new ExistingPlacePickerAdapter(places, selected -> {
            selectedPlaceHolder[0] = selected;
            String address = TextUtils.isEmpty(selected.getAddress())
                    ? "Chưa có địa chỉ"
                    : selected.getAddress();
            selectedPlaceText.setText(selected.getName() + "\n" + address);
            selectedPlaceText.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dark));
            pickerDialog.dismiss();
        });
        placesView.setLayoutManager(new LinearLayoutManager(requireContext()));
        placesView.setAdapter(pickerAdapter);
        updatePlacePickerState(places.size(), resultCount, emptyState, placesView);

        searchInput.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {
                int count = pickerAdapter.filter(editable.toString());
                updatePlacePickerState(count, resultCount, emptyState, placesView);
            }
        });
        pickerView.findViewById(R.id.btnClosePlacePicker).setOnClickListener(view -> pickerDialog.dismiss());
        pickerDialog.setOnShowListener(dialog -> searchInput.requestFocus());
        pickerDialog.show();
    }

    private void updatePlacePickerState(
            int count,
            TextView resultCount,
            TextView emptyState,
            RecyclerView placesView
    ) {
        resultCount.setText(count + " quán phù hợp");
        emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        placesView.setVisibility(count == 0 ? View.GONE : View.VISIBLE);
    }

    private boolean hasDraft(EditText... fields) {
        if (selectedPostImageUri != null) return true;
        for (EditText field : fields) {
            if (field != null && field.getText().toString().trim().length() > 0) {
                return true;
            }
        }
        return false;
    }

    private void submitPost(
            BottomSheetDialog dialog,
            EditText etPostPlaceName,
            EditText etPostAddress,
            EditText etPostTags,
            boolean useExistingPlace,
            FoodPlace selectedExistingPlace
    ) {
        String caption = etPostCaption.getText().toString().trim();
        if (useExistingPlace && selectedExistingPlace == null) {
            Toast.makeText(requireContext(), "Vui lòng chọn một quán đã có trên app", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!useExistingPlace
                && (etPostPlaceName.getText().toString().trim().isEmpty()
                || etPostAddress.getText().toString().trim().isEmpty())) {
            Toast.makeText(requireContext(), "Nhập tên quán và địa chỉ quán mới", Toast.LENGTH_SHORT).show();
            return;
        }
        if (caption.isEmpty() && selectedPostImageUri == null) {
            Toast.makeText(requireContext(), "Nhập caption hoặc chọn ảnh trước khi đăng", Toast.LENGTH_SHORT).show();
            return;
        }

        setButtonEnabled(btnSubmitPost, false);
        btnSubmitPost.setText("Đang đăng...");

        CommunityPost post = new CommunityPost();
        post.setAuthorId(currentUserId);
        post.setAuthorName(getCurrentUserName());
        post.setAuthorAvatarUrl(getCurrentUserAvatar());
        post.setCaption(caption);
        if (useExistingPlace) {
            post.setPlaceId(selectedExistingPlace.getId());
            post.setPlaceName(selectedExistingPlace.getName());
            post.setPlaceAddress(selectedExistingPlace.getAddress());
            post.setPlaceRating(selectedExistingPlace.getAverageRating());
            post.setOpenLate(selectedExistingPlace.isOpenLate());
            if (selectedExistingPlace.getImageUrls() != null && !selectedExistingPlace.getImageUrls().isEmpty()) {
                post.setPlaceImageUrl(selectedExistingPlace.getImageUrls().get(0));
            }
        } else {
            post.setPlaceName(etPostPlaceName.getText().toString().trim());
            post.setPlaceAddress(etPostAddress.getText().toString().trim());
        }
        post.setTags(new ArrayList<>(parseTags(caption, etPostTags.getText().toString())));
        post.setCreatedAt(System.currentTimeMillis());

        if (selectedPostImageUri != null) {
            uploadImageThenCreatePost(post, dialog);
        } else {
            createPost(post, dialog);
        }
    }

    private void uploadImageThenCreatePost(CommunityPost post, BottomSheetDialog dialog) {
        StorageReference imageRef = FirebaseStorage.getInstance()
                .getReference("community_posts/" + currentUserId + "/" + System.currentTimeMillis() + ".jpg");
        UploadTask uploadTask = imageRef.putFile(selectedPostImageUri);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                Exception exception = task.getException();
                if (exception != null) throw exception;
            }
            return imageRef.getDownloadUrl();
        }).addOnSuccessListener(uri -> {
            post.setImageUrl(uri.toString());
            createPost(post, dialog);
        }).addOnFailureListener(error -> {
            if (btnSubmitPost != null) {
                btnSubmitPost.setText("Đăng bài");
                setButtonEnabled(btnSubmitPost, true);
            }
            Toast.makeText(requireContext(), "Upload ảnh thất bại: " + error.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void createPost(CommunityPost post, BottomSheetDialog dialog) {
        communityRepository.createPost(post)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(requireContext(), "Đã đăng bài", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .addOnFailureListener(error -> {
                    if (btnSubmitPost != null) {
                        btnSubmitPost.setText("Đăng bài");
                        setButtonEnabled(btnSubmitPost, true);
                    }
                    Toast.makeText(requireContext(), "Đăng bài thất bại", Toast.LENGTH_SHORT).show();
                });
    }

    private Set<String> parseTags(String caption, String rawTags) {
        Set<String> tags = new LinkedHashSet<>();
        addHashtagsFromText(tags, caption);
        addHashtagsFromText(tags, rawTags);

        for (String rawTag : rawTags.split("[,\\s]+")) {
            String tag = rawTag.trim();
            if (tag.isEmpty()) continue;
            if (!tag.startsWith("#")) tag = "#" + tag;
            tags.add(tag);
        }
        return tags;
    }

    private void addHashtagsFromText(Set<String> tags, String text) {
        if (TextUtils.isEmpty(text)) return;
        Matcher matcher = Pattern.compile("#[\\p{L}\\p{N}_]+").matcher(text);
        while (matcher.find()) {
            tags.add(matcher.group());
        }
    }

    private void updateSelectedImagePreview() {
        if (layoutSelectedImage == null || ivSelectedPostImage == null || tvSelectedImageName == null) return;
        if (selectedPostImageUri == null) {
            layoutSelectedImage.setVisibility(View.GONE);
            return;
        }
        layoutSelectedImage.setVisibility(View.VISIBLE);
        Glide.with(this)
                .load(selectedPostImageUri)
                .placeholder(R.drawable.bg_image_placeholder)
                .centerCrop()
                .into(ivSelectedPostImage);
        tvSelectedImageName.setText(getUriDisplayName(selectedPostImageUri));
    }

    private void updateSubmitPostButton() {
        if (btnSubmitPost == null || etPostCaption == null) return;
        boolean enabled = etPostCaption.getText().toString().trim().length() > 0 || selectedPostImageUri != null;
        setButtonEnabled(btnSubmitPost, enabled);
    }

    private void setButtonEnabled(TextView button, boolean enabled) {
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.45f);
    }

    private void sharePost(CommunityPost post) {
        StringBuilder shareText = new StringBuilder();
        if (!TextUtils.isEmpty(post.getCaption())) {
            shareText.append(post.getCaption());
        }
        if (!TextUtils.isEmpty(post.getPlaceName())) {
            shareText.append("\n\nQuán: ").append(post.getPlaceName());
        }
        if (!TextUtils.isEmpty(post.getPlaceAddress())) {
            shareText.append("\nĐịa chỉ: ").append(post.getPlaceAddress());
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        startActivity(Intent.createChooser(intent, "Chia sẻ bài viết"));
    }

    private void openPlace(CommunityPost post) {
        if (!TextUtils.isEmpty(post.getPlaceId())) {
            Intent intent = new Intent(requireContext(), FoodPlaceDetailActivity.class);
            intent.putExtra(FoodPlaceDetailActivity.EXTRA_PLACE_ID, post.getPlaceId());
            startActivity(intent);
            return;
        }

        String message = !TextUtils.isEmpty(post.getPlaceAddress())
                ? post.getPlaceAddress()
                : "Bài này chưa gắn quán có sẵn trên app";
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private String getCurrentUserName() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return "Người dùng";
        if (!TextUtils.isEmpty(user.getDisplayName())) return user.getDisplayName();
        if (!TextUtils.isEmpty(user.getEmail())) {
            return user.getEmail().split("@")[0];
        }
        return "Người dùng";
    }

    private String getCurrentUserAvatar() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";
    }

    private long getUriSize(Uri uri) {
        if (uri == null) return 0;
        Cursor cursor = null;
        try {
            cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex >= 0) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }

        try {
            return requireContext().getContentResolver().openAssetFileDescriptor(uri, "r").getLength();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String getUriDisplayName(Uri uri) {
        if (uri == null) return "Ảnh đã chọn";
        Cursor cursor = null;
        try {
            cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return "Ảnh đã chọn";
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    private android.location.Location getUserLocation() {
        if (getContext() == null) return null;
        if (androidx.core.app.ActivityCompat.checkSelfPermission(requireContext(), 
                android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        android.location.LocationManager lm = (android.location.LocationManager)
                requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
        if (lm == null) return null;
        try {
            android.location.Location loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (loc == null) {
                loc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            }
            return loc;
        } catch (Exception e) {
            return null;
        }
    }

    private double calculateDistanceKm(android.location.Location userLoc, FoodPlace place) {
        if (userLoc == null || place == null) return Double.MAX_VALUE;
        float[] results = new float[1];
        android.location.Location.distanceBetween(
                userLoc.getLatitude(), userLoc.getLongitude(),
                place.getLatitude(), place.getLongitude(),
                results
        );
        return results[0] / 1000.0;
    }
}
