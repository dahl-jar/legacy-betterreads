package com.br.betterreads.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GenreService {

    private static final Set<String> PRIMARY_GENRES = Set.of(
            "fiction", "fantasy", "science fiction", "mystery", "thriller", "horror",
            "romance", "historical fiction", "young adult", "adventure", "epic fantasy",
            "biography", "non-fiction", "history", "science", "philosophy", "poetry",
            "drama", "comedy", "classic", "crime", "dystopian", "urban fantasy",
            "paranormal", "western", "memoir", "contemporary", "literary fiction",
            "suspense", "gothic", "cyberpunk", "steampunk", "mythology", "fairy tale",
            "folklore", "supernatural", "magical realism", "satire", "war", "espionage",
            "action", "short stories", "graphic novel", "coming of age", "historical",
            "political", "psychological", "alternate history", "post-apocalyptic"
    );

    private static final Set<String> GENRE_MODIFIERS = Set.of(
            "epic", "dark", "high", "low", "historical", "urban", "contemporary",
            "literary", "classic", "modern", "gothic", "hard", "soft", "military",
            "paranormal", "erotic", "psychological", "philosophical", "political"
    );

    private static final Set<String> EXCLUSION_TERMS = Set.of(
            "character", "battle", "seasons", "kings and rulers", "winter", "invierno",
            "imaginary wars", "imaginary places", "award winner", "bestseller",
            "new york times", "courts and courtiers", "civil war", "good and evil",
            "bien y mal"
    );

    /**
     * Determine genres from an array of subjectNodes in the JSON.
     */
    public String[] determineGenres(JsonNode subjectNodes) {
        if (subjectNodes == null || !subjectNodes.isArray()) return new String[0];
        List<String> subjects = new ArrayList<>();
        subjectNodes.forEach(node -> subjects.add(node.asText().toLowerCase()));
        return processSubjects(subjects).toArray(new String[0]);
    }

    /**
     * Determine genres from a raw List<?> (like from “subjects” in a work details map).
     */
    public String[] determineGenres(List<?> rawSubjects) {
        if (rawSubjects == null || rawSubjects.isEmpty()) return new String[]{"Fiction"};
        List<String> subjects = rawSubjects.stream()
                .filter(obj -> obj instanceof String)
                .map(obj -> ((String) obj).toLowerCase())
                .collect(Collectors.toList());
        return processSubjects(subjects).toArray(new String[0]);
    }

    private List<String> processSubjects(List<String> subjects) {
        Set<String> result = new LinkedHashSet<>();
        for (String subject : subjects) {
            if (shouldExclude(subject)) {
                continue;
            }
            if (PRIMARY_GENRES.contains(subject)) {
                result.add(capitalize(subject));
            } else {
                for (String mod : GENRE_MODIFIERS) {
                    if (subject.contains(mod)) {
                        result.add(capitalize(subject));
                        break;
                    }
                }
            }
        }
        if (result.isEmpty()) {
            return List.of("Fiction");
        }
        return result.stream().limit(5).collect(Collectors.toList());
    }

    private boolean shouldExclude(String subject) {
        return EXCLUSION_TERMS.stream().anyMatch(subject::contains);
    }

    private String capitalize(String input) {
        if (input == null || input.isEmpty()) return input;
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
