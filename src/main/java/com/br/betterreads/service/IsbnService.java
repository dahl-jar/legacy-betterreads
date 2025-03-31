package com.br.betterreads.service;

import com.br.betterreads.util.OpenLibraryUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class IsbnService {

    private final OpenLibraryClient openLibraryClient;

    public IsbnService(OpenLibraryClient openLibraryClient) {
        this.openLibraryClient = openLibraryClient;
    }

    /**
     * Extract the first ISBN from a doc, if present, else fetch from /works/.
     */
    public String extractIsbn(JsonNode doc) {
        if (doc.has("isbn") && doc.get("isbn").isArray() && !doc.get("isbn").isEmpty()) {
            return normalizeIsbn(doc.get("isbn").get(0).asText());
        }
        String workKey = OpenLibraryUtil.extractWorkKey(doc);
        if (workKey == null) {
            return generateFallbackIsbn();
        }
        return fetchIsbnFromWork(workKey);
    }

    /**
     * Attempt to fetch an ISBN by scanning up to 10 editions of a work.
     */
    private String fetchIsbnFromWork(String workKey) {
        if (workKey == null) {
            return generateFallbackIsbn();
        }
        Map<String, Object> editions = openLibraryClient.fetchEditions(workKey);
        if (editions == null || !editions.containsKey("entries")) {
            return generateFallbackIsbn();
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entries = (List<Map<String, Object>>) editions.get("entries");
        // ISBN_13
        @SuppressWarnings("unchecked")
        Optional<String> isbn13 = entries.stream()
                .filter(e -> e.containsKey("isbn_13"))
                .map(e -> ((List<String>) e.get("isbn_13")).getFirst())
                .filter(s -> s != null && !s.isEmpty())
                .findFirst();
        if (isbn13.isPresent()) {
            return normalizeIsbn(isbn13.get());
        }
        // ISBN_10
        @SuppressWarnings("unchecked")
        Optional<String> isbn10 = entries.stream()
                .filter(e -> e.containsKey("isbn_10"))
                .map(e -> ((List<String>) e.get("isbn_10")).getFirst())
                .filter(s -> s != null && !s.isEmpty())
                .findFirst();
        if (isbn10.isPresent()) {
            return normalizeIsbn(isbn10.get());
        }
        return generateFallbackIsbn();
    }

    /**
     * Clean up ISBN to be 13 characters only [0-9X].
     */
    public String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return generateFallbackIsbn();
        }
        String cleaned = isbn.replaceAll("[^0-9X]", "");
        if (cleaned.length() > 13) {
            return cleaned.substring(0, 13);
        } else if (cleaned.length() < 13) {
            return cleaned + "0".repeat(13 - cleaned.length());
        }
        return cleaned;
    }

    public String generateFallbackIsbn() {
        return "0000000000000";
    }
}
