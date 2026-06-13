package com.example.cuisine_finder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.cuisine_finder.utils.SearchTextUtils;
import org.junit.Test;

public class SearchTextUtilsTest {
    @Test
    public void matchesVietnameseTextWithoutDiacritics() {
        assertTrue(SearchTextUtils.matchesAllTerms(
                "Cơm Tấm Cổng Trường, Thủ Đức",
                "com tam thu duc"
        ));
    }

    @Test
    public void matchesTermsInAnyOrder() {
        assertTrue(SearchTextUtils.matchesAllTerms(
                "Bún bò ngon tại Quận 1",
                "quan 1 bun"
        ));
    }

    @Test
    public void requiresEverySearchTerm() {
        assertFalse(SearchTextUtils.matchesAllTerms(
                "Bánh mì chảo HCMUTE",
                "banh mi thu duc"
        ));
    }
}
