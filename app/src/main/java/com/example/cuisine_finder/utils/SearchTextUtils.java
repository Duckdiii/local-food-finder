package com.example.cuisine_finder.utils;

import java.text.Normalizer;
import java.util.Locale;

public final class SearchTextUtils {
    private SearchTextUtils() {
    }

    public static String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.replaceAll("\\s+", " ");
    }

    public static boolean matchesAllTerms(String searchableText, String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) return true;

        String normalizedText = normalize(searchableText);
        for (String term : normalizedQuery.split(" ")) {
            if (!normalizedText.contains(term)) return false;
        }
        return true;
    }
}
