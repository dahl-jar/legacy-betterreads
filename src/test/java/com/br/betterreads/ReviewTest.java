package com.br.betterreads;

import com.br.betterreads.model.Book;
import com.br.betterreads.model.Review;
import com.br.betterreads.model.User;
import com.br.betterreads.model.ValidationResult;
import com.br.betterreads.repository.ReviewRepository;
import com.br.betterreads.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewTest {

    @Mock
    private ReviewRepository reviewRepository;

    private ReviewService reviewService;
    private User user;
    private Book book;
    private String reviewText;
    private int rating;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(reviewRepository);
        user = new User("testUser", "test@example.com", "password123");
        book = new Book();
        reviewText = "Good Book!";
        rating = 5;
    }

    @Test
    void createReview() {
        ValidationResult result = reviewService.createAndSaveReview(user, book, rating, reviewText);
        assertTrue(result.valid());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_NoUser() {
        ValidationResult result = reviewService.createAndSaveReview(null, book, rating, reviewText);
        assertFalse(result.valid());
        assertEquals("You must be logged in to make reviews", result.errorMessage());
    }

    @Test
    void createReview_NoBook() {
        ValidationResult result = reviewService.createAndSaveReview(user, null, rating, reviewText);
        assertFalse(result.valid());
        assertEquals("Book doesnt exist", result.errorMessage());
    }

    @Test
    void createReview_alreadyReviewed() {
        when(reviewRepository.getReviewByUserAndBook(user, book)).thenReturn(new Review(user, book, rating, reviewText));

        ValidationResult result = reviewService.createAndSaveReview(user, book, rating, reviewText);

        assertFalse(result.valid());
        assertEquals("You have already reviewed this book", result.errorMessage());
    }

    @Test
    void createReview_InvalidRating() {
        rating = -1;
        ValidationResult result = reviewService.createAndSaveReview(user, book, rating, reviewText);
        assertFalse(result.valid());
        assertEquals("Please rate the book", result.errorMessage());
    }

    @Test
    void createReview_InvalidReviewText() {
        ValidationResult result = reviewService.createAndSaveReview(user, book, rating, null);
        assertFalse(result.valid());
        assertEquals("Review text is empty", result.errorMessage());

        result = reviewService.createAndSaveReview(user, book, rating, "       ");
        assertFalse(result.valid());
        assertEquals("Review text is empty", result.errorMessage());
    }

    @Test
    void updateReview() {
        Review existingReview = new Review(user, book, rating, reviewText);
        when(reviewRepository.getReviewByUserAndBook(user, book)).thenReturn(existingReview);

        int newRating = 4;
        String newText = "Updated review text";

        ValidationResult result = reviewService.updateReview(existingReview, newRating, newText);
        System.out.println(result.errorMessage());
        assertTrue(result.valid());
        verify(reviewRepository).save(existingReview);
        assertEquals(newRating, existingReview.getRating());
        assertEquals(newText, existingReview.getText());
    }

    @Test
    void deleteReview() {

        Review savedReview = new Review(user, book, rating, reviewText);
        when(reviewRepository.getReviewByUserAndBook(user, book)).thenReturn(savedReview);

        reviewService.deleteReview(savedReview);
        verify(reviewRepository).delete(savedReview);
    }

}
