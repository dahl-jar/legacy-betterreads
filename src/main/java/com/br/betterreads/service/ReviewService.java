package com.br.betterreads.service;

import com.br.betterreads.model.Book;
import com.br.betterreads.model.Review;
import com.br.betterreads.model.User;
import com.br.betterreads.model.ValidationResult;
import com.br.betterreads.repository.ReviewRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing {@link Review}
 */
@Service
public class ReviewService {

    private final ReviewRepository reviewRepo;

    public ReviewService(ReviewRepository reviewRepo) {
        this.reviewRepo = reviewRepo;
    }

    private ValidationResult validateReview(Review review) {
        if (review == null) return new ValidationResult(false, "Review doesnt exist");
        if(review.getUser() == null) return new ValidationResult(false, "You must be logged in to make reviews");
        if(review.getBook() == null) return new ValidationResult(false, "Book doesnt exist");

        List<Review> existingReviews = reviewRepo.getReviewByUserAndBook(review.getUser(), review.getBook());
        if(!existingReviews.isEmpty()) return new ValidationResult(false, "You have already reviewed this book");

        if(review.getRating() < 0) return new ValidationResult(false, "Please rate the book");
        if(review.getText() == null || review.getText().isBlank()) return new ValidationResult(false, "Review text is empty");
        return ValidationResult.success();
    }

    /**
     * Creates a new review for a book
     * @param user reviewer
     * @param book book getting reviewed
     * @param rating reviewers rating 1 - 5
     * @param text review text
     * @return {@link ValidationResult}
     */
    @Transactional
    public ValidationResult createAndSaveReview(User user, Book book, int rating, String text) {
        Review review = new Review(user, book, rating, text);
        ValidationResult result = validateReview(review);
        if(!result.valid()) return result;
        review.setText(text.trim());
        reviewRepo.save(review);
        return result;
    }

    /**
     * Updates a review.
     * @param review review to update
     * @param newRating the new rating
     * @param newText the new text
     * @return {@link ValidationResult}
     */
    @Transactional
    public ValidationResult updateReview(Review review, int newRating, String newText) {
        List<Review> reviews = reviewRepo.getReviewByUserAndBook(review.getUser(), review.getBook());
        if (reviews.isEmpty()) {
            return new ValidationResult(false, "Review not found");
        }

        Review oldReview = reviews.get(0);
        oldReview.setRating(newRating);
        oldReview.setText(newText);
        reviewRepo.save(oldReview);

        return ValidationResult.success();
    }



    /**
     * Deletes a review
     * @param review review to delete
     */
    @Transactional
    public void deleteReview(Review review) {
        List<Review> reviews = reviewRepo.getReviewByUserAndBook(review.getUser(), review.getBook());
        if (reviews.isEmpty()) return;

        Review toDelete = reviews.getFirst();
        reviewRepo.delete(toDelete);
    }

    /**
     * Get the average rating on a book based on user reviews.
     * @param book Book to fetch reviews from
     * @return Average rating of the book
     */
    public double getAvgRating(Book book) {
        List<Review> reviews = reviewRepo.findByBook(book);
        if (reviews.isEmpty()) return 0;
        return reviews.stream().mapToDouble(Review::getRating).sum() / reviews.size();
    }

    /**
     * Get the amount of reviews for a given book
     * @param book Book to fetch reviews from
     * @return number of reviews
     */
    public int getAmountOfReviews(Book book) {
        List<Review> reviews = reviewRepo.findByBook(book);
        return reviews.size();
    }



}
