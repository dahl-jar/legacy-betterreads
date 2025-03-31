package com.br.betterreads.service;

import com.br.betterreads.model.Book;
import com.br.betterreads.util.DescriptionHelper;
import com.br.betterreads.util.OpenLibraryUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class BookProcessingService {

    private final OpenLibraryClient openLibraryClient;
    private final IsbnService isbnService;
    private final AuthorService authorService;
    private final GenreService genreService;
    private final ObjectMapper objectMapper;

    public BookProcessingService(OpenLibraryClient openLibraryClient, IsbnService isbnService, GenreService genreService, AuthorService authorService) {
        this.openLibraryClient = openLibraryClient;
        this.isbnService = isbnService;
        this.authorService = authorService;
        this.genreService = genreService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Process search results from OpenLibrary, replicating original logic:
     *  - parse documents
     *  - deduplicate by title+author, pick best
     *  - sort by "quality" scoring (and optional exact‐match preference)
     *  - return top 10
     */
    public List<Book> processSearchResults(Map<String, Object> apiResponse, String searchQuery) {
        if (apiResponse == null) {
            return Collections.emptyList();
        }
        JsonNode root = objectMapper.valueToTree(apiResponse);
        JsonNode docs = (root != null) ? root.get("docs") : null;
        if (docs == null || !docs.isArray()) {
            return Collections.emptyList();
        }
        Map<String, Book> uniqueBooks = new ConcurrentHashMap<>();
        List<Book> rawBooks = StreamSupport.stream(docs.spliterator(), true)
                .map(this::processDocument)
                .filter(Objects::nonNull)
                .toList();
        for (Book book : rawBooks) {
            String compositeKey = (book.getTitle().toLowerCase() + "||" + book.getAuthor().toLowerCase());
            if (!uniqueBooks.containsKey(compositeKey)) {
                uniqueBooks.put(compositeKey, book);
            } else {
                Book existing = uniqueBooks.get(compositeKey);
                if (isBookBetterQuality(book, existing)) {
                    uniqueBooks.put(compositeKey, book);
                }
            }
        }
        List<Book> sorted = new ArrayList<>(uniqueBooks.values());
        sorted.sort((b1, b2) -> compareBookQuality(b1, b2, searchQuery));

        return sorted.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * Process a single doc into a Book, using all sub-services for data.
     */
    private Book processDocument(JsonNode doc) {
        Book book = new Book();
        book.setTitle(doc.path("title").asText("Unknown Title"));
        book.setDescription(DescriptionHelper.getDescription(doc.get("description")));
        book.setAuthor(authorService.extractAuthorName(doc));

        if (doc.has("cover_i")) {
            book.setCoverURL("https://covers.openlibrary.org/b/id/" + doc.get("cover_i").asText() + "-M.jpg");
        } else {
            book.setCoverURL("/images/template.avif");
        }
        book.setPublicationYear(doc.path("first_publish_year").asInt(0));


        String isbn = isbnService.extractIsbn(doc);
        book.setIsbn(isbn);

        if (doc.has("subject") && doc.get("subject").isArray()) {
            book.setGenre(genreService.determineGenres(doc.get("subject")));
        }

        String workKey = OpenLibraryUtil.extractWorkKey(doc);
        if (workKey != null) {
            enhanceWithWorkDetails(book, workKey);
        }


        book.setSubtitle("");

        String keyForApi = (workKey != null) ? workKey
                : (doc.has("key") ? doc.get("key").asText() : isbn);
        book.setApiId(generateApiIdFromKey(keyForApi));

        book.setLastSync(LocalDateTime.now());
        return sanitizeBook(book);
    }

    /**
     * “Process Book Details” for a single ISBN call.
     * This merges logic for cover fallback, subtitle fallback,
     * retrieving description from the associated /works/ key, etc.
     */
    public Book processBookDetails(Map<String, Object> bookApiResponse, String isbn) {
        if (bookApiResponse == null || !bookApiResponse.containsKey("ISBN:" + isbn)) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> rawData = (Map<String, Object>) bookApiResponse.get("ISBN:" + isbn);
        if (rawData == null) {
            return null;
        }

        Book book = new Book();
        book.setIsbn(isbn);
        Object cover = rawData.get("cover");
        if (cover instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> coverMap = (Map<String, String>) cover;

            String coverUrl = coverMap.getOrDefault("medium", coverMap.getOrDefault("large", "/images/template.avif"));
            book.setCoverURL(coverUrl);
        } else {
            book.setCoverURL("/images/template.avif");
        }

        Object titleObj = rawData.get("title");
        book.setTitle((titleObj instanceof String) ? (String) titleObj : "Unknown Title");

        Object authors = rawData.get("authors");
        if (authors instanceof List<?> authorsList) {
            String authorNames = authorsList.stream()
                    .map(a -> {
                        if (a instanceof Map) {
                            Object nameObj = ((Map<?, ?>) a).get("name");
                            return (nameObj instanceof String) ? (String) nameObj : null;
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            book.setAuthor(authorNames.isEmpty() ? "Unknown Author" : authorNames);
        } else {
            book.setAuthor("Unknown Author");
        }
        String url = (String) rawData.get("url");
        if (url != null) {
            String[] parts = url.split("/");
            String bookId = "";
            for (int i = 0; i < parts.length; i++) {
                if ("books".equals(parts[i]) && i + 1 < parts.length) {
                    bookId = parts[i + 1];
                    break;
                }
            }
            Map<String, Object> bookDetails = openLibraryClient.performRequest("https://openlibrary.org/books/" + bookId + ".json");
            if (bookDetails != null && bookDetails.containsKey("works")) {
                Object worksObj = bookDetails.get("works");
                if (worksObj instanceof List<?> worksList) {
                    if (!worksList.isEmpty()) {
                        Object firstWork = worksList.getFirst();
                        if (firstWork instanceof Map) {
                            String workKey = (String) ((Map<?, ?>) firstWork).get("key");
                            if (workKey != null) {
                                Map<String, Object> workDetails = openLibraryClient.fetchWorkDetails(workKey);
                                if (workDetails != null) {
                                    Object descObj = workDetails.get("description");
                                    String desc = DescriptionHelper.getDescription(descObj);
                                    if (!desc.equals("No description available")) {
                                        book.setDescription(desc);
                                    }
                                    Object subObj = workDetails.get("subtitle");
                                    String subtitle = DescriptionHelper.getSubtitle(subObj);
                                    book.setSubtitle(subtitle != null ? subtitle : "");
                                }
                            }
                        }
                    }
                }
            }
        }

        if (book.getDescription() == null) {
            book.setDescription("No description available");
        }

        if (book.getSubtitle() == null) {
            book.setSubtitle("");
        }

        book.setLastSync(LocalDateTime.now());
        book.setApiId(generateApiIdFromKey(isbn));
        return sanitizeBook(book);
    }

    /**
     * Enhances a Book with details from /works/<something> (description, subjects, etc.).
     */
    private void enhanceWithWorkDetails(Book book, String workKey) {
        if (workKey == null) return;

        // Normalize the work key to ensure it's in the correct format
        if (!workKey.startsWith("/works/")) {
            workKey = "/works/" + workKey.replaceAll("^/+|/+$", "")
                    .replace("/works/", "");
        }

        try {
            Map<String, Object> workDetails = openLibraryClient.fetchWorkDetails(workKey);
            if (workDetails == null) return;

            if (workDetails.containsKey("description")) {
                String possibleDesc = DescriptionHelper.getDescription(workDetails.get("description"));
                if (!"No description available".equals(possibleDesc)) {
                    book.setDescription(possibleDesc);
                }
            }

            if ((book.getGenre() == null || book.getGenre().length == 0) &&
                    workDetails.containsKey("subjects")) {

                Object subjectsObj = workDetails.get("subjects");
                if (subjectsObj instanceof List<?> subjects) {
                    String[] genres = genreService.determineGenres(subjects);
                    if (genres != null && genres.length > 0) {
                        book.setGenre(genres);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error enhancing book with work details for " + workKey + ": " + e.getMessage());
        }
    }

    /**
     * Compare two books by “quality”, plus handle any tie-breakers
     * or exact‐match preference for the search query.
     */
    private int compareBookQuality(Book b1, Book b2, String searchQuery) {
        // Highest “quality” first
        int score1 = calculateBookQualityScore(b1);
        int score2 = calculateBookQualityScore(b2);
        if (score1 != score2) {
            return score2 - score1;
        }
        // If same quality, we can optionally push exact or partial matches to the front
        String sQuery = (searchQuery == null) ? "" : searchQuery.trim().toLowerCase();
        String t1 = b1.getTitle().toLowerCase();
        String t2 = b2.getTitle().toLowerCase();

        // Exact match first
        boolean exact1 = t1.equals(sQuery);
        boolean exact2 = t2.equals(sQuery);
        if (exact1 && !exact2) return -1;
        if (exact2 && !exact1) return 1;

        // Then partial startsWith
        boolean starts1 = t1.startsWith(sQuery);
        boolean starts2 = t2.startsWith(sQuery);
        if (starts1 && !starts2) return -1;
        if (starts2 && !starts1) return 1;

        // fallback to alphabetical
        return t1.compareTo(t2);
    }

    /**
     * A simple heuristic to rate books for sorting.
     */
    private int calculateBookQualityScore(Book book) {
        int score = 0;
        // description presence
        if (book.getDescription() != null && !"No description available".equals(book.getDescription())) {
            score += 10;
            // add up to +5 more for longer descriptions (like original code did)
            score += Math.min(5, book.getDescription().length() / 100);
        }
        // multiple genres
        if (book.getGenre() != null && book.getGenre().length > 0) {
            score += 5 * book.getGenre().length;
        }
        // better cover
        if (book.getCoverURL() != null && !"/images/template.avif".equals(book.getCoverURL())) {
            score += 3;
        }
        // publication year
        if (book.getPublicationYear() != null && book.getPublicationYear() > 0) {
            score += 2;
        }
        return score;
    }

    /**
     * Decide if newBook is strictly better than existingBook by the same title/author.
     * This is used for merging duplicates (like the old code).
     */
    private boolean isBookBetterQuality(Book newBook, Book existingBook) {
        int existingScore = 0;
        int newScore = 0;

        if (!"No description available".equals(existingBook.getDescription())) existingScore += 5;
        if (!"No description available".equals(newBook.getDescription())) newScore += 5;

        if (existingBook.getGenre() != null && existingBook.getGenre().length > 0) existingScore += 3;
        if (newBook.getGenre() != null && newBook.getGenre().length > 0) newScore += 3;

        if (existingBook.getPublicationYear() != null && existingBook.getPublicationYear() > 0) existingScore += 2;
        if (newBook.getPublicationYear() != null && newBook.getPublicationYear() > 0) newScore += 2;

        if (!"/images/template.avif".equals(existingBook.getCoverURL())) existingScore += 1;
        if (!"/images/template.avif".equals(newBook.getCoverURL())) newScore += 1;

        return newScore > existingScore;
    }

    /**
     * Extract a “description” from a map of work details, if present.
     */
    public String extractWorkDescription(Map<String, Object> workDetails) {
        if (workDetails == null) return null;
        if (!workDetails.containsKey("description")) return "No description available";
        return DescriptionHelper.getDescription(workDetails.get("description"));
    }

    /**
     * Create an integer ID from a string key, used to fill the Book's “apiId.”
     */
    private int generateApiIdFromKey(String key) {
        if (key == null) {
            return new Random().nextInt(1000000);
        }
        try {
            return Math.abs(key.hashCode() % 1000000000);
        } catch (Exception e) {
            return new Random().nextInt(1000000);
        }
    }

    /**
     * Trim fields if needed (avoid DB column overflows, etc.).
     */
    private Book sanitizeBook(Book book) {
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

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return (value.length() <= maxLength) ? value : value.substring(0, maxLength);
    }
}
