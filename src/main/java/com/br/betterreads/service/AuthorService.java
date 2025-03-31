package com.br.betterreads.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AuthorService {

    private final OpenLibraryClient openLibraryClient;

    public AuthorService(OpenLibraryClient openLibraryClient) {
        this.openLibraryClient = openLibraryClient;
    }

    /**
     * Extract the author from a doc. If doc has “author_name,” we just join them;
     * otherwise, we try the first “author_key” and look up the name from the author’s .json.
     */
    public String extractAuthorName(JsonNode doc) {
        if (doc.has("author_name") && doc.get("author_name").isArray()) {
            return joinAuthorNames(doc.get("author_name"));
        }
        return fetchAuthorNameFromKey(doc);
    }

    private String joinAuthorNames(JsonNode authorNodes) {
        List<String> names = new ArrayList<>();
        authorNodes.forEach(node -> names.add(node.asText()));
        return String.join(", ", names);
    }

    private String fetchAuthorNameFromKey(JsonNode doc) {
        if (!doc.has("author_key") || !doc.get("author_key").isArray() || doc.get("author_key").isEmpty()) {
            return "Unknown Author";
        }
        JsonNode firstKey = doc.get("author_key").get(0);
        if (firstKey == null) {
            return "Unknown Author";
        }
        String authorKey = "/authors/" + firstKey.asText();
        Map<String, Object> authorData = openLibraryClient.fetchAuthorDetails(authorKey);
        return extractNameFromAuthorData(authorData);
    }

    private String extractNameFromAuthorData(Map<String, Object> authorData) {
        if (authorData != null && authorData.containsKey("name")) {
            Object name = authorData.get("name");
            return (name instanceof String) ? (String) name : "Unknown Author";
        }
        return "Unknown Author";
    }
}
