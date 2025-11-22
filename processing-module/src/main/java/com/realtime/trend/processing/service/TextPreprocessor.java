package com.realtime.trend.processing.service;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * 텍스트 전처리 서비스
 */
@Service
public class TextPreprocessor {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile(
            "[\\x00-\\x1F\\x7F]"
    );

    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile(
            "\\s+"
    );

    public String preprocess(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String processed = text;

        // 1. HTML 태그 제거
        processed = removeHtmlTags(processed);

        // 2. URL 제거
        processed = removeUrls(processed);

        // 3. 이메일 제거
        processed = removeEmails(processed);

        // 4. 특수 제어 문자 제거
        processed = removeSpecialChars(processed);

        // 5. 공백/줄바꿈 정규화
        processed = normalizeWhitespace(processed);

        return processed.trim();
    }

    private String removeHtmlTags(String text) {
        return Jsoup.parse(text).text();
    }

    private String removeUrls(String text) {
        return URL_PATTERN.matcher(text).replaceAll("");
    }

    private String removeEmails(String text) {
        return EMAIL_PATTERN.matcher(text).replaceAll("");
    }

    private String removeSpecialChars(String text) {
        return SPECIAL_CHARS_PATTERN.matcher(text).replaceAll("");
    }

    private String normalizeWhitespace(String text) {
        return MULTIPLE_SPACES_PATTERN.matcher(text).replaceAll(" ");
    }
}
