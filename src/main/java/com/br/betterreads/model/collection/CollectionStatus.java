package com.br.betterreads.model.collection;

public enum CollectionStatus {
    HAVE_READ,
    WANT_TO_READ,
    CURRENTLY_READING;

    public String getFormattedName() {
        String[] words = name().replace('_', ' ').toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1));
        }

        return result.toString();
    }
}
