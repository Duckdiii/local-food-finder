package com.example.cuisine_finder.activities;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.example.cuisine_finder.R;
import com.example.cuisine_finder.models.Story;
import com.example.cuisine_finder.repositories.StoryRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class StoryViewerActivity extends AppCompatActivity {

    private ImageView ivStoryFull, ivUserAvatar;
    private TextView tvUserName, tvCaption;
    private LinearLayout layoutProgress;
    private List<Story> storyList = new ArrayList<>();
    private int currentIndex = 0;
    private final Handler handler = new Handler();
    private Runnable autoAdvanceRunnable;
    private static final long STORY_DURATION = 5000; // 5 seconds per story

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);

        ivStoryFull = findViewById(R.id.ivStoryFull);
        ivUserAvatar = findViewById(R.id.ivUserAvatar);
        tvUserName = findViewById(R.id.tvUserName);
        tvCaption = findViewById(R.id.tvCaption);
        layoutProgress = findViewById(R.id.layoutProgress);

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        loadActiveStories();
    }

    private void loadActiveStories() {
        new StoryRepository().getActiveStories().get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                Story story = doc.toObject(Story.class);
                if (story != null) storyList.add(story);
            }

            if (!storyList.isEmpty()) {
                setupProgressBars();
                showStory(0);
            } else {
                finish();
            }
        });
    }

    private void setupProgressBars() {
        layoutProgress.removeAllViews();
        for (int i = 0; i < storyList.size(); i++) {
            View progressView = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
            params.setMargins(4, 0, 4, 0);
            progressView.setLayoutParams(params);
            progressView.setBackgroundColor(getColor(R.color.white));
            progressView.setAlpha(0.3f);
            layoutProgress.addView(progressView);
        }
    }

    private void showStory(int index) {
        if (index < 0 || index >= storyList.size()) {
            finish();
            return;
        }

        currentIndex = index;
        Story story = storyList.get(index);

        tvUserName.setText(story.getUserName());
        tvCaption.setText(story.getCaption());

        Glide.with(this).load(story.getImageUrl()).into(ivStoryFull);
        if (story.getUserAvatarUrl() != null) {
            Glide.with(this).load(story.getUserAvatarUrl()).into(ivUserAvatar);
        }

        // Highlight progress bar
        for (int i = 0; i < layoutProgress.getChildCount(); i++) {
            layoutProgress.getChildAt(i).setAlpha(i <= index ? 1.0f : 0.3f);
        }

        // Auto advance
        if (autoAdvanceRunnable != null) handler.removeCallbacks(autoAdvanceRunnable);
        autoAdvanceRunnable = () -> showStory(currentIndex + 1);
        handler.postDelayed(autoAdvanceRunnable, STORY_DURATION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoAdvanceRunnable != null) handler.removeCallbacks(autoAdvanceRunnable);
    }
}
