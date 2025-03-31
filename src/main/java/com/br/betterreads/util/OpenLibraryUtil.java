package com.br.betterreads.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public final class OpenLibraryUtil {

    private OpenLibraryUtil() {
    }

    /**
     * Extract a /works/... key from a JSON doc node.
     * Both BookProcessingService and IsbnService can use this method.
     */
    public static String extractWorkKey(JsonNode doc) {
        // If the doc has a "key" that starts with "/works/"
        if (doc.has("key") && doc.get("key").asText().startsWith("/works/")) {
            return doc.get("key").asText();
        }
        if (doc.has("works") && doc.get("works").isArray() && !doc.get("works").isEmpty()) {
            JsonNode firstWork = doc.get("works").get(0);
            if (firstWork.has("key")) {
                return firstWork.get("key").asText();
            }
        }
        return null;
    }

    /**
     * Parse a description that can be either a string or a map with a "value" field.
     * Returns a fallback if null or unrecognized.
     */
    @SuppressWarnings("unchecked")
    public static String getDescription(Object descObj) {
        if (descObj == null) {
            return "No description available";
        }
        if (descObj instanceof String) {
            return (String) descObj;
        }
        if (descObj instanceof Map) {
            Map<String, Object> descMap = (Map<String, Object>) descObj;
            Object val = descMap.get("value");
            if (val instanceof String) {
                return (String) val;
            }
        }
        return "No description available";
    }
}

