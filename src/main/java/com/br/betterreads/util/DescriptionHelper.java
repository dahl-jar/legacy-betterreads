package com.br.betterreads.util;

import java.util.Map;

public class DescriptionHelper {

    public static String getDescription(Object descObj) {
        if (descObj instanceof String) {
            return (String) descObj;
        } else if (descObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> descMap = (Map<String, Object>) descObj;
            Object value = descMap.get("value");
            return (value instanceof String) ? (String) value : "No description available";
        }
        return "No description available";
    }

    public static String getSubtitle(Object subtitleObj) {
        if (subtitleObj instanceof String) {
            return (String) subtitleObj;
        } else if (subtitleObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> subMap = (Map<String, Object>) subtitleObj;
            Object value = subMap.get("value");
            return (value instanceof String) ? (String) value : null;
        }
        return null;
    }
}
