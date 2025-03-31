package com.br.betterreads.service;

import com.br.betterreads.model.Book;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class CacheService {

    private static final long DEFAULT_TTL = 1800000; // 30 minutes

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Get from cache if present and not expired; otherwise, fetch from supplier and cache it.
     */
    public List<Book> getCachedOrFetch(String key, Supplier<List<Book>> supplier) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return new ArrayList<>(entry.getBooks());
        }
        List<Book> result = supplier.get();
        cache.put(key, new CacheEntry(result));
        return result;
    }

    /**
     * Basic cache entry container
     */
    private static class CacheEntry {
        private final List<Book> books;
        private final long timestamp;

        CacheEntry(List<Book> books) {
            this.books = Collections.unmodifiableList(books);
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > DEFAULT_TTL;
        }

        public List<Book> getBooks() {
            return books;
        }
    }

}
