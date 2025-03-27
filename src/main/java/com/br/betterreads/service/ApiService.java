package com.br.betterreads.service;

import com.br.betterreads.model.OpenLibraryApi;
import com.br.betterreads.model.OpenLibraryTrendingResponse;
import com.br.betterreads.util.BookMapper;
import com.br.betterreads.model.Book;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ApiService {

    private final RestTemplate restTemplate;
    private final BookMapper bookMapper;

    public ApiService(RestTemplate restTemplate, BookMapper bookMapper) {
        this.restTemplate = restTemplate;
        this.bookMapper = bookMapper;
    }


    /**
     * Fetches ISBN from work editions endpoint if available
     *
     * @param workKey Work key (e.g., "/works/OL27482W")
     * @return Valid ISBN or fallback
     */
    private String fetchIsbnFromWork(String workKey) {
        if (workKey == null) return "0000000000000";

        try {
            String editionsUrl = "https://openlibrary.org" + workKey + "/editions.json";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    editionsUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> editions = response.getBody();
            if (editions != null && editions.containsKey("entries")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> entries = (List<Map<String, Object>>) editions.get("entries");

                for (Map<String, Object> entry : entries) {
                    if (entry.containsKey("isbn_13")) {
                        @SuppressWarnings("unchecked")
                        List<String> isbn13List = (List<String>) entry.get("isbn_13");
                        if (isbn13List != null && !isbn13List.isEmpty()) {
                            return normalizeIsbn(isbn13List.getFirst());
                        }
                    }
                }

                for (Map<String, Object> entry : entries) {
                    if (entry.containsKey("isbn_10")) {
                        @SuppressWarnings("unchecked")
                        List<String> isbn10List = (List<String>) entry.get("isbn_10");
                        if (isbn10List != null && !isbn10List.isEmpty()) {
                            return normalizeIsbn(isbn10List.getFirst());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching editions: " + e.getMessage());
        }

        return generateIsbnFromKey(workKey);
    }

    /**
     * Fetches work details from Open Library API
     *
     * @param workKey The work key from Open Library (should include the leading slash)
     * @return Map containing work details or null if not found/error
     */
    private Map<String, Object> fetchWorkDetails(String workKey) {
        if (workKey == null) return null;
        if (!workKey.startsWith("/")) {
            workKey = "/" + workKey;
        }

        String workUrl = "https://openlibrary.org" + workKey + ".json";
        try {
            ResponseEntity<Map<String, Object>> workResponse = restTemplate.exchange(
                    workUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return workResponse.getBody();
        } catch (Exception e) {
            System.err.println("Error fetching work details: " + e.getMessage());
            return null;
        }
    }

    private Map<String, Object> performSearchRequest(String url) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error during API request: " + e.getMessage());
            return null;
        }
    }

    public List<Book> searchBookByAuthor(String author) {
        try {
            String searchUrl = String.format(
                    "https://openlibrary.org/search.json?author=%s&limit=100",
                    URLEncoder.encode(author, StandardCharsets.UTF_8)
            );
            return processSearchRequest(searchUrl);
        } catch (Exception e) {
            System.err.println("Error searching books by author: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Book> searchBookByTitle(String title) {
        try {
            String searchUrl = String.format(
                    "https://openlibrary.org/search.json?title=%s&limit=100",
                    URLEncoder.encode(title, StandardCharsets.UTF_8)
            );
            return processSearchRequest(searchUrl);
        } catch (Exception e) {
            System.err.println("Error searching books by title: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<Book> extractBooksFromSearchResult(JsonNode root) {
        // Using a composite key of title+author to identify unique books
        Map<String, Book> uniqueBooks = new HashMap<>();
        List<Book> resultBooks = new ArrayList<>();


        if (root != null && root.has("docs")) {
            JsonNode docs = root.get("docs");

            for (JsonNode doc : docs) {
                // Create book and fill basic information
                Book book = new Book();

                // Set title
                String title = doc.has("title") ? doc.get("title").asText() : "Unknown Title";
                book.setTitle(title);
                book.setSubtitle("");

                // Set authors
                String author = "Unknown Author";
                if (doc.has("author_name") && doc.get("author_name").isArray()) {
                    StringBuilder authorBuilder = new StringBuilder();
                    JsonNode authorNodes = doc.get("author_name");
                    for (int i = 0; i < authorNodes.size(); i++) {
                        if (i > 0) authorBuilder.append(", ");
                        authorBuilder.append(authorNodes.get(i).asText());
                    }
                    author = authorBuilder.toString();
                }
                book.setAuthor(author);

                // Create a composite key for deduplication
                String compositeKey = title.toLowerCase() + "||" + author.toLowerCase();

                // Extract work key for additional metadata
                String key = doc.has("key") ? doc.get("key").asText() : null;
                String workKey = null;

                if (key != null) {
                    if (key.startsWith("/works/")) {
                        workKey = key;
                    } else if (doc.has("work_key")) {
                        workKey = doc.get("work_key").asText();
                    } else if (key.startsWith("/books/")) {
                        try {
                            workKey = extractWorkKeyFromBook(key);
                        } catch (Exception e) {
                            System.err.println("Error fetching book details: " + e.getMessage());
                        }
                    }
                }

                // Set default description
                book.setDescription("No description available");

                // Set cover URL
                if (doc.has("cover_i")) {
                    book.setCoverURL("https://covers.openlibrary.org/b/id/" + doc.get("cover_i").asText() + "-M.jpg");
                } else {
                    book.setCoverURL("/images/template.avif");
                }

                // Set publication year
                if (doc.has("first_publish_year")) {
                    book.setPublicationYear(doc.get("first_publish_year").asInt());
                }

                // Set ISBN
                String isbn;
                if (doc.has("isbn") && doc.get("isbn").isArray() && !doc.get("isbn").isEmpty()) {
                    isbn = normalizeIsbn(doc.get("isbn").get(0).asText());
                } else {
                    isbn = workKey != null ? fetchIsbnFromWork(workKey) : "0000000000000";
                }

                if (isbn.equals("0000000000000")) {
                    isbn = generateIsbnFromKey(workKey != null ? workKey : key);
                }

                book.setIsbn(isbn);

                // Set genres/subjects
                if (doc.has("subject") && doc.get("subject").isArray()) {
                    setGenresFromSubjects(doc.get("subject"), book);
                }

                // Try to get more details from workKey
                if (workKey != null) {
                    enhanceBookWithWorkDetails(book, workKey);
                }

                book.setApiId(generateApiIdFromKey(key));
                book.setLastSync(LocalDateTime.now());

                // Determine if this book should replace an existing one
                boolean shouldReplace = false;
                if (!uniqueBooks.containsKey(compositeKey)) {
                    shouldReplace = true;
                } else {
                    // Compare with existing book to see if this one is better
                    Book existingBook = uniqueBooks.get(compositeKey);
                    shouldReplace = isBookBetterQuality(book, existingBook);
                }

                if (shouldReplace) {
                    uniqueBooks.put(compositeKey, book);
                }

                resultBooks.add(book);
            }
        }

        // Sort results by quality score and then by exact match or closest match
        resultBooks.sort((b1, b2) -> {
            // Calculate quality scores
            int score1 = calculateBookQualityScore(b1);
            int score2 = calculateBookQualityScore(b2);

            if (score1 != score2) {
                return score2 - score1; // Higher score first
            }

            // If quality scores are equal, prefer exact title matches
            String title1 = b1.getTitle().toLowerCase();
            String title2 = b2.getTitle().toLowerCase();

            if (title1.equals(title2)) {
                return 0;
            }

            String searchQuery = root.has("q") ? root.get("q").asText().toLowerCase() : "";

            // Prefer exact matches to query
            boolean exactMatch1 = title1.equals(searchQuery);
            boolean exactMatch2 = title2.equals(searchQuery);

            if (exactMatch1 && !exactMatch2) return -1;
            if (!exactMatch1 && exactMatch2) return 1;

            // Otherwise prefer titles that start with the query
            boolean startsWith1 = title1.startsWith(searchQuery);
            boolean startsWith2 = title2.startsWith(searchQuery);

            if (startsWith1 && !startsWith2) return -1;
            if (!startsWith1 && startsWith2) return 1;

            // Otherwise use natural ordering
            return title1.compareTo(title2);
        });

        // Return only books with descriptions first, then fall back to any books
        List<Book> booksWithDescriptions = resultBooks.stream()
                .filter(b -> !"No description available".equals(b.getDescription()))
                .toList();

        if (!booksWithDescriptions.isEmpty()) {
            return booksWithDescriptions.stream()
                    .limit(10)
                    .collect(Collectors.toList());
        }

        return resultBooks.stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    private int calculateBookQualityScore(Book book) {
        int score = 0;

        // Description is most important
        if (!"No description available".equals(book.getDescription())) {
            score += 10;
            // Bonus points for longer descriptions (likely more informative)
            score += Math.min(5, book.getDescription().length() / 100);
        }

        // Genres are next most important
        if (book.getGenre() != null && book.getGenre().length > 0) {
            score += 5 * book.getGenre().length; // More genres = better categorization
        }

        // Having a cover image
        if (book.getCoverURL() != null && !"/images/template.avif".equals(book.getCoverURL())) {
            score += 3;
        }

        // Having a publication year
        if (book.getPublicationYear() != null && book.getPublicationYear() > 0) {
            score += 2;
        }

        return score;
    }

    /**
     * Helper method to extract work key from a book key
     */
    private String extractWorkKeyFromBook(String bookKey) {
        String bookUrl = "https://openlibrary.org" + bookKey + ".json";
        ResponseEntity<Map<String, Object>> bookResponse = restTemplate.exchange(
                bookUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        Map<String, Object> bookDetails = bookResponse.getBody();
        if (bookDetails != null && bookDetails.containsKey("works")) {
            List<?> works = (List<?>) bookDetails.get("works");
            if (!works.isEmpty() && works.getFirst() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> firstWork = (Map<String, Object>) works.getFirst();
                if (firstWork.containsKey("key")) {
                    return (String) firstWork.get("key");
                }
            }
        }
        return null;
    }

    /**
     * Helper method to set genres from subject nodes
     */
    private void setGenresFromSubjects(JsonNode subjectNodes, Book book) {
        List<OpenLibraryApi.OpenLibrarySubjectDTO> subjects = new ArrayList<>();

        for (JsonNode subjectNode : subjectNodes) {
            OpenLibraryApi.OpenLibrarySubjectDTO subject = new OpenLibraryApi.OpenLibrarySubjectDTO();
            subject.setName(subjectNode.asText());
            subjects.add(subject);
        }

        book.setGenre(bookMapper.formatSubjects(subjects));
    }

    /**
     * Helper method to enhance book with work details
     */
    private void enhanceBookWithWorkDetails(Book book, String workKey) {
        try {
            Map<String, Object> workDetails = fetchWorkDetails(workKey);
            if (workDetails != null) {
                // Description
                if (workDetails.containsKey("description")) {
                    Object descObj = workDetails.get("description");
                    String description = getDescription(descObj);
                    if (!description.isEmpty()) {
                        book.setDescription(description);
                    }
                }

                // Subjects/genres if not already set
                if (workDetails.containsKey("subjects") &&
                        (book.getGenre() == null || book.getGenre().length == 0)) {
                    List<?> subjects = (List<?>) workDetails.get("subjects");
                    List<String> genres = getStrings(subjects);
                    if (!genres.isEmpty()) {
                        book.setGenre(genres.toArray(new String[0]));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching work details: " + e.getMessage());
        }
    }

    /**
     * Compares two book objects and determines which one has better quality data
     */
    private boolean isBookBetterQuality(Book newBook, Book existingBook) {
        int currentScore = 0;
        int newScore = 0;

        // Check description (most important)
        if (!"No description available".equals(existingBook.getDescription())) currentScore += 5;
        if (!"No description available".equals(newBook.getDescription())) newScore += 5;

        // Check genres
        if (existingBook.getGenre() != null && existingBook.getGenre().length > 0) currentScore += 3;
        if (newBook.getGenre() != null && newBook.getGenre().length > 0) newScore += 3;

        // Check for publication year
        if (existingBook.getPublicationYear() != null && existingBook.getPublicationYear() > 0) currentScore += 2;
        if (newBook.getPublicationYear() != null && newBook.getPublicationYear() > 0) newScore += 2;

        // Check cover
        if (!"/images/template.avif".equals(existingBook.getCoverURL())) currentScore += 1;
        if (!"/images/template.avif".equals(newBook.getCoverURL())) newScore += 1;

        return newScore > currentScore;
    }


    public Book fetchBookFromApi(String isbn) {
        try {
            String isbnUrl = String.format(
                    "https://openlibrary.org/api/books?bibkeys=ISBN:%s&format=json&jscmd=data",
                    isbn
            );

            ResponseEntity<Map<String, OpenLibraryApi>> response = restTemplate.exchange(
                    isbnUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            OpenLibraryApi bookData = Objects.requireNonNull(response.getBody()).get("ISBN:" + isbn);
            if (bookData == null) {
                return null;
            }
            Book book = bookMapper.convertToBook(bookData, isbn);
            book.setIsbn(isbn);

            String url = bookData.getUrl();
            if (url != null) {
                String[] split = url.split("/");
                String bookId = "";
                for (int i = 0; i < split.length; i++) {
                    if ("books".equals(split[i]) && i + 1 < split.length) {
                        bookId = split[i + 1];
                        break;
                    }
                }

                String bookDetailsUrl = "https://openlibrary.org/books/" + bookId + ".json";
                ResponseEntity<Map<String, Object>> bookDetailsResponse = restTemplate.exchange(
                        bookDetailsUrl,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {
                        }
                );

                Map<String, Object> bookDetails = bookDetailsResponse.getBody();
                if (bookDetails != null && bookDetails.containsKey("works")) {

                    Object worksObj = bookDetails.get("works");
                    if (worksObj instanceof List<?> worksList) {
                        if (!worksList.isEmpty() && worksList.getFirst() instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> first = (Map<String, Object>) worksList.getFirst();
                            Object key = first.get("key");
                            if (key instanceof String workKey) {
                                String workId = workKey.replace("/works/", "");
                                String workUrl = "https://openlibrary.org/works/" + workId + ".json";
                                ResponseEntity<Map<String, Object>> workDetailsResponse = restTemplate.exchange(
                                        workUrl,
                                        HttpMethod.GET,
                                        null,
                                        new ParameterizedTypeReference<>() {
                                        }
                                );

                                Map<String, Object> workDetails = workDetailsResponse.getBody();
                                if (workDetails != null && workDetails.containsKey("description")) {
                                    Object desObj = workDetails.get("description");
                                    String description = getDescription(desObj);
                                    book = bookMapper.updateBookWithDescription(book, description);
                                }

                                if (workDetails != null && workDetails.containsKey("subtitle")) {
                                    Object subObj = workDetails.get("subtitle");
                                    String subtitle = getSubtitle(subObj);
                                    book.setSubtitle(subtitle);
                                }
                            }
                        }
                    }
                }
            }
            if (book.getCoverURL() == null) {
                book.setCoverURL("/images/covertemplate.jpg");
            }
            if (book.getSubtitle() == null) {
                book.setSubtitle("");
            }
            book.setIsbn(isbn);

            return sanitizeBookFields(book);
        } catch (Exception e) {
            System.err.println("Error fetching book data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<OpenLibraryTrendingResponse.TrendingBook> fetchTrendingBooks(int limit) {
        String apiUrl = "https://openlibrary.org/trending/weekly.json?limit=" + limit;

        ResponseEntity<OpenLibraryTrendingResponse> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                null,
                OpenLibraryTrendingResponse.class
        );

        if (response.getBody() == null || response.getBody().getWorks() == null) {
            return List.of();
        }

        return response.getBody().getWorks();
    }

    public static String getDescription(Object descObj) {
        if (descObj instanceof String) {
            return (String) descObj;
        } else if (descObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> DesMap = (Map<String, Object>) descObj;
            Object ValObj = DesMap.get("value");
            return ValObj instanceof String ? (String) ValObj : "No description available";
        }
        return "No description available";
    }

    private static String getSubtitle(Object subtitleObj) {
        if (subtitleObj instanceof String) {
            return (String) subtitleObj;
        } else if (subtitleObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> SubMap = (Map<String, Object>) subtitleObj;
            Object ValObj = SubMap.get("value");
            return ValObj instanceof String ? (String) ValObj : null;
        }
        return null;
    }

    public String fetchDescriptionFromWorkId(String workId) {

        Map<String, Object> workDetails = fetchWorkDetails(workId);
        if (workDetails == null) {
            return null;
        }
        if (workDetails.containsKey("description")) {
            Object desObj = workDetails.get("description");
            return getDescription(desObj);
        }

        return "No description availible";

    }

    private String fetchAuthorName(String authorKey) {
        try {
            String authorUrl = "https://openlibrary.org" + authorKey + ".json";
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    authorUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            Map<String, Object> authorData = response.getBody();
            if (authorData != null && authorData.containsKey("name")) {
                return authorData.get("name").toString();
            }
        } catch (Exception e) {
            System.err.println("Error fetching author name: " + e.getMessage());
        }
        return null;
    }

    private static List<String> getStrings(List<?> subjects) {
        if (subjects == null || subjects.isEmpty()) return List.of("Fiction");  // Default genre

        Set<String> commonGenres = Set.of(
                "fiction", "fantasy", "science fiction", "mystery", "thriller",
                "romance", "historical fiction", "young adult", "adventure",
                "horror", "biography", "non-fiction", "history", "science",
                "philosophy", "poetry", "drama", "comedy", "classic"
        );


        Set<String> specificTerms = Set.of(
                "character", "battle", "war", "ring", "magic", "wizard", "dragon", "dwarf", "elf",
                "goblin", "troll", "spider", "hobbit", "invisibility", "eagle", "thrush",
                "pictorial", "specimen", "translation", "movable", "graphic", "juvenile",
                "toy", "untranslated", "motion", "arkenstone", "five armies"
        );

        List<String> priorityGenres = new ArrayList<>();
        for (Object subject : subjects) {
            if (subject instanceof String) {
                String genre = ((String) subject).toLowerCase().trim();
                if (commonGenres.contains(genre)) {
                    priorityGenres.add(genre.substring(0, 1).toUpperCase() + genre.substring(1));
                }
            }
        }

        if (!priorityGenres.isEmpty()) {
            return priorityGenres.stream().limit(3).collect(Collectors.toList());
        }

        List<String> filterGenre = new ArrayList<>();
        for (Object subject : subjects) {
            if (subject instanceof String) {
                String genre = ((String) subject).trim();
                String lowerGenre = genre.toLowerCase();


                if (genre.length() <= 5) continue;
                if (specificTerms.stream().anyMatch(lowerGenre::contains)) continue;
                if (lowerGenre.contains("protected daisy")) continue;
                if (lowerGenre.contains("accessible book")) continue;
                if (lowerGenre.contains("large type books")) continue;
                if (lowerGenre.contains("library staff")) continue;
                if (lowerGenre.contains("texts")) continue;
                if (lowerGenre.contains("works")) continue;
                if (lowerGenre.contains("awards")) continue;
                if (lowerGenre.contains("fictitious character")) continue;

                filterGenre.add(genre);

                if (filterGenre.size() >= 3) break;
            }
        }

        if (!filterGenre.isEmpty()) {
            return filterGenre;
        }

        return List.of("Fiction");
    }

    private String normalizeIsbn(String isbn) {
        if (isbn == null) return "0000000000000";
        String cleaned = isbn.replaceAll("[^0-9X]", "");
        if (cleaned.length() > 13) {
            return cleaned.substring(0, 13);
        } else if (cleaned.length() < 13) {
            return cleaned + "0".repeat(13 - cleaned.length());
        }
        return cleaned;
    }

    private String generateIsbnFromKey(String key) {
        if (key == null) return "0000000000000";
        String cleanKey = key.replaceAll("[^a-zA-Z0-9]", "");
        int hashCode = Math.abs(cleanKey.hashCode());
        String isbn = String.format("%013d", hashCode % 10000000000000L);
        if (isbn.length() > 13) {
            return isbn.substring(0, 13);
        }
        return isbn;
    }

    private int generateApiIdFromKey(String key) {
        if (key == null) return new Random().nextInt(1000000);

        try {
            return Math.abs(key.hashCode() % 1000000000);
        } catch (Exception e) {
            return new Random().nextInt(1000000);
        }
    }

    private List<Book> processSearchRequest(String searchUrl) {
        Map<String, Object> result = performSearchRequest(searchUrl);
        if (result != null) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.valueToTree(result);
            List<Book> books = extractBooksFromSearchResult(jsonNode);
            return books.stream()
                    .map(this::sanitizeBookFields)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private String extractIsbnFromApiData(Map<String, Object> bookData) {
        if (bookData == null) return "0000000000000";
        if (bookData.containsKey("identifiers")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> identifiers = (Map<String, Object>) bookData.get("identifiers");

            if (identifiers.containsKey("isbn_13")) {
                @SuppressWarnings("unchecked")
                List<String> isbn13 = (List<String>) identifiers.get("isbn_13");
                if (isbn13 != null && !isbn13.isEmpty()) {
                    return normalizeIsbn(isbn13.getFirst());
                }
            }

            if (identifiers.containsKey("isbn_10")) {
                @SuppressWarnings("unchecked")
                List<String> isbn10 = (List<String>) identifiers.get("isbn_10");
                if (isbn10 != null && !isbn10.isEmpty()) {
                    return normalizeIsbn(isbn10.getFirst());
                }
            }
        }
        return generateIsbnFromKey(bookData.containsKey("key") ?
                (String)bookData.get("key") : null);
    }

    /**
     * Truncates a string to a maximum length safely
     *
     * @param value String to truncate
     * @return Truncated string or original if null or shorter
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * Sanitizes all string fields in a Book entity to ensure they don't exceed
     * the database column size limits
     *
     * @param book Book to sanitize
     * @return Sanitized book
     */
    private Book sanitizeBookFields(Book book) {
        if (book == null) return null;

        book.setTitle(truncate(book.getTitle(), 255));
        book.setSubtitle(truncate(book.getSubtitle(), 255));
        book.setAuthor(truncate(book.getAuthor(), 255));
        book.setDescription(truncate(book.getDescription(), 1000));
        book.setCoverURL(truncate(book.getCoverURL(), 255));
        book.setIsbn(truncate(book.getIsbn(), 13));

        if (book.getGenre() != null) {
            String[] sanitizedGenre = new String[book.getGenre().length];
            for (int i = 0; i < book.getGenre().length; i++) {
                sanitizedGenre[i] = truncate(book.getGenre()[i], 255);
            }
            book.setGenre(sanitizedGenre);
        }

        return book;
    }
}
