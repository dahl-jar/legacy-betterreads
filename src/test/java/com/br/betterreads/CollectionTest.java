package com.br.betterreads;

import com.br.betterreads.model.Book;
import com.br.betterreads.model.User;
import com.br.betterreads.model.ValidationResult;
import com.br.betterreads.model.collection.Collection;
import com.br.betterreads.model.collection.CollectionStatus;
import com.br.betterreads.repository.CollectionRepository;
import com.br.betterreads.service.CollectionService;
import com.br.betterreads.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CollectionTest {

    @Mock
    private CollectionRepository collectionRepository;

    private CollectionService collectionService;
    private User user;
    private Book book;

    @BeforeEach
    public void setUp() {
        collectionService = new CollectionService(collectionRepository);
        user = new User("testUser", "test@example.com", "password123");
        book = new Book();
    }

    @Test
    public void testAddBookToCollection() {
        ValidationResult result = collectionService.addToCollection(user, book, CollectionStatus.WANT_TO_READ);
        assertTrue(result.valid());
        verify(collectionRepository).save(any(Collection.class));
    }

    @Test
    public void testRemoveBookFromCollection() {
        Collection collection = new Collection(user, book, CollectionStatus.WANT_TO_READ);
        when(collectionRepository.findCollectionByUserAndBook(user, book)).thenReturn(Optional.of(collection));

        collectionService.removeFromCollection(user, book);
        verify(collectionRepository).delete(collection);
    }

    @Test
    public void testChangeStatus() {
        Collection collection = new Collection(user, book, CollectionStatus.WANT_TO_READ);
        when(collectionRepository.findCollectionByUserAndBook(user, book)).thenReturn(Optional.of(collection));

        collectionService.addToCollection(user, book, CollectionStatus.HAVE_READ);
        assertEquals(CollectionStatus.HAVE_READ, collection.getStatus());
    }

    @Test
    public void testTimestampNotChanged() {
        Collection collection = new Collection(user, book, CollectionStatus.WANT_TO_READ);
        collection.setCreatedAt(LocalDateTime.of(2025, 7, 4, 12, 0));
        when(collectionRepository.findCollectionByUserAndBook(user, book)).thenReturn(Optional.of(collection));

        collectionService.addToCollection(user, book, CollectionStatus.HAVE_READ);
        assertEquals(LocalDateTime.of(2025, 7, 4, 12, 0), collection.getCreatedAt());
    }

}
