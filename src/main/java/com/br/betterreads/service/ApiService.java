package com.br.betterreads.service;

import com.br.betterreads.model.Book;
import com.br.betterreads.model.OpenLibraryTrendingResponse;
import com.br.betterreads.util.BookMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class ApiService {

    private final OpenLibraryClient openLibraryClient;
    private final BookProcessingService bookProcessingService;
    private final CacheService cacheService;
    private final RestTemplate restTemplate;

    public ApiService(OpenLibraryClient openLibraryClient,
                      BookProcessingService bookProcessingService,
                      CacheService cacheService, RestTemplate restTemplate) {
        this.openLibraryClient = openLibraryClient;
        this.bookProcessingService = bookProcessingService;
        this.cacheService = cacheService;
        this.restTemplate = restTemplate;
    }

    /**
     * Search for books by title, caching results for up to 30 minutes.
     */
    public List<Book> searchBookByTitle(String title) {
        String cacheKey = "title:" + title.toLowerCase();
        return cacheService.getCachedOrFetch(cacheKey, () ->
                bookProcessingService.processSearchResults(
                        openLibraryClient.searchBooks("title", title),
                        title // pass the query for possible exact-match logic
                )
        );
    }

    /**
     * Search for books by author, caching results for up to 30 minutes.
     */
    public List<Book> searchBookByAuthor(String author) {
        String cacheKey = "author:" + author.toLowerCase();
        return cacheService.getCachedOrFetch(cacheKey, () ->
                bookProcessingService.processSearchResults(
                        openLibraryClient.searchBooks("author", author),
                        author // pass the query for possible exact-match logic
                )
        );
    }

    /**
     * Fetch a single book by ISBN.
     * Retrieve raw data, pass it into BookProcessing for final structuring
     */
    public Book fetchBookFromApi(String isbn) {
        return bookProcessingService.processBookDetails(
                openLibraryClient.fetchBookData(isbn),
                isbn
        );
    }

    /**
     * Fetches trending books from OpenLibrary API
     * @return List of trending books
     */
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


    /**
     * Fetch the description for a given work ID (e.g. “/works/OL1234W”).
     */
    public String fetchDescriptionFromWorkId(String workId) {
        return bookProcessingService.extractWorkDescription(
                openLibraryClient.fetchWorkDetails(workId)
        );
    }

    List<Book> deduplicateBooks(List<Book> books) {
        Map<String, List<Book>> booksByTitleAuthor = books.stream()
                .collect(Collectors.groupingBy(book ->
                        (book.getTitle() != null ? book.getTitle().toLowerCase() : "") + "|" +
                                (book.getAuthor() != null ? book.getAuthor().toLowerCase() : "")
                ));

        return booksByTitleAuthor.values().stream()
                .map(this::selectBestEdition)
                .collect(Collectors.toList());
    }

    private Book selectBestEdition(List<Book> editions) {
        for (Book book : editions) {
            if (book.getGenre() != null && book.getGenre().length > 0 &&
                    book.getDescription() != null && !book.getDescription().isEmpty()) {
                return book;
            }
        }

        for (Book book : editions) {
            if (book.getDescription() != null && !book.getDescription().isEmpty()) {
                return book;
            }
        }

        for (Book book : editions) {
            if (book.getGenre() != null && book.getGenre().length > 0) {
                return book;
            }
        }
        return editions.getFirst();
    }


}
