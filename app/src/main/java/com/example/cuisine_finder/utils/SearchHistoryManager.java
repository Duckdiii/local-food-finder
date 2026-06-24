package com.example.cuisine_finder.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchHistoryManager {
    private static final String PREF_NAME = "search_history_prefs";
    private static final String KEY_HISTORY = "recent_searches";
    private static final int MAX_HISTORY = 10;
    private final SharedPreferences prefs;

    public SearchHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        
        List<String> history = getHistory();
        // Remove if already exists to move it to the top
        history.remove(query.trim());
        
        // Add to the beginning
        history.add(0, query.trim());
        
        // Keep only top 10
        if (history.size() > MAX_HISTORY) {
            history = history.subList(0, MAX_HISTORY);
        }
        
        prefs.edit().putString(KEY_HISTORY, String.join(",", history)).apply();
    }

    public List<String> getHistory() {
        String historyStr = prefs.getString(KEY_HISTORY, "");
        if (historyStr.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(historyStr.split(",")));
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}
