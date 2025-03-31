package com.br.betterreads.service;

import com.br.betterreads.model.OpenLibraryTrendingResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class OpenLibraryClient {

    private final RestTemplate restTemplate;

    public OpenLibraryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Perform a search against Open Library with the specified field
     * (e.g. “title=foo” or “author=bar”).
     */
    public Map<String, Object> searchBooks(String searchType, String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = String.format(
                "https://openlibrary.org/search.json?%s=%s&limit=100&fields=*,author_name,cover_i,isbn,first_publish_year,description,subject,works",
                searchType, encoded
        );
        return performRequest(url);
    }

    /**
     * Fetch the .json details for a given /works/<workKey>.
     */
    public Map<String, Object> fetchWorkDetails(String workKey) {
        String normalizedKey = normalizeWorkKey(workKey);
        if (normalizedKey.isEmpty()) {
            return null;
        }
        String url = "https://openlibrary.org" + normalizedKey + ".json";
        return performRequest(url);
    }

    /**
     * Fetch the first 10 editions for a /works/<workKey>.
     */
    public Map<String, Object> fetchEditions(String workKey) {
        String normalizedKey = normalizeWorkKey(workKey);
        if (normalizedKey.isEmpty()) {
            return null;
        }
        String url = "https://openlibrary.org" + normalizedKey + "/editions.json?limit=10&fields=isbn_10,isbn_13";
        return performRequest(url);
    }

    /**
     * Fetch basic data for a single ISBN from Open Library’s /api/books.
     */
    public Map<String, Object> fetchBookData(String isbn) {
        String isbnUrl = String.format(
                "https://openlibrary.org/api/books?bibkeys=ISBN:%s&format=json&jscmd=data",
                isbn
        );
        return performRequest(isbnUrl);
    }

    /**
     * Fetch trending (weekly) data from “/trending/weekly.json”.
     */
    public List<OpenLibraryTrendingResponse.TrendingBook> fetchTrendingBooks(int limit) {
        String apiUrl = "https://openlibrary.org/trending/weekly.json?limit=" + limit;
        OpenLibraryTrendingResponse body = restTemplate
                .exchange(apiUrl, HttpMethod.GET, null, OpenLibraryTrendingResponse.class)
                .getBody();
        if (body == null || body.getWorks() == null) {
            return List.of();
        }
        return body.getWorks();
    }

    /**
     * If you also fetch authors with /authors/<key>.
     */
    public Map<String, Object> fetchAuthorDetails(String authorKey) {
        if (authorKey == null) {
            return null;
        }
        String normalizedKey = authorKey.startsWith("/authors/") ? authorKey : "/authors/" + authorKey;
        String url = "https://openlibrary.org" + normalizedKey;
        return performRequest(url + ".json");
    }

    /**
     * Common utility to call the URL and parse into a Map.
     */
    public Map<String, Object> performRequest(String url) {
        try {
            return restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            ).getBody();
        } catch (Exception e) {
            System.err.println("API request failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Ensure we have a leading /works/ for keys like “OL12345W,” etc.
     */
    private String normalizeWorkKey(String workKey) {
        if (workKey == null) {
            return "";
        }
        if (!workKey.startsWith("/works/")) {
            return "/works/" + workKey.replace("/", "");
        }
        return workKey;
    }
}
