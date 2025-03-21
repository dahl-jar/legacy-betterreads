package com.br.betterreads;

import com.br.betterreads.model.Book;
import com.br.betterreads.service.ApiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ApiTest {

    @Autowired
    private ApiService apiService;

    @Test
    void testFetchBook() {
        Book book = apiService.fetchBookFromApi("9780812511819");
        assertNotNull(book.getTitle());
        assertEquals("Robert Jordan", book.getAuthor());
    }
}