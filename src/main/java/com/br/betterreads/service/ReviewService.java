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
        if(reviewRepo.getReviewByUserAndBook(review.getUser(), review.getBook()) != null) return new ValidationResult(false, "You have already reviewed this book");
        if(review.getRating() < 0) return new ValidationResult(false, "Please rate the book");
        if(review.getText() == null || review.getText().isBlank()) return new ValidationResult(false, "Review text is empty");
        return ValidationResult.success();
    }

    @Transactional
    public ValidationResult createAndSaveReview(User user, Book book, int rating, String text) {
        Review review = new Review(user, book, rating, text);
        ValidationResult result = validateReview(review);
        if(!result.valid()) return result;
        review.setText(text.trim());
        reviewRepo.save(review);
        return result;
    }

    @Transactional
    public ValidationResult updateReview(Review review, int newRating, String newText) {
        Review oldReview = reviewRepo.getReviewByUserAndBook(review.getUser(), review.getBook());

        oldReview.setRating(newRating);
        oldReview.setText(newText);

        reviewRepo.save(oldReview);

        return ValidationResult.success();
    }

    @Transactional
    public void deleteReview(Review review) {
        Review toDelete = reviewRepo.getReviewByUserAndBook(review.getUser(), review.getBook());
        if (toDelete == null) return;
        reviewRepo.delete(toDelete);
    }

}
