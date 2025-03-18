package com.br.betterreads.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;


/**
 * Represents a review submitted by a user for a book in the BetterReads application
 *
 * Each review is associated with a specific user and book, contains a rating,
 * review text, and an automatically generated creation timestamp
 */
@Entity
@Table(name = "review", schema = "betterreads")
public class Review {

    /**
     * Unique identifier for the review (Auto-generated)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    /**
     * The user who submitted the review
     *
     * This is a many-to-one relationship since many reviews can
     * be associated with one user
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    /**
     * The book that the review refers to
     *
     * This is a many-to-one relationship since many reviews can be written for one book
     */
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    @NotNull
    private Book book;

    /**
     * The rating given by the user for the book
     *
     * The value must be between 1 and 5
     */
    @NotNull
    @Min(value = 1, message = "Rating must be at least 1.")
    @Max(value = 5, message = "Rating must be at most 5.")
    @Column(name = "rating", nullable = false)
    private int rating;

    /**
     * The content of the users review
     *
     * The text must be between 1 and 1000 characters
     */
    @NotNull
    @Size(min = 1, max = 1000, message = "Review text must be between 1 and 1000 characters")
    @Column(name = "review_text", nullable = false)
    private String text;

    /**
     * The timestamp when the review was created
     *
     * This field is automatically set before persisting the review
     * and cannot be updated afterward
     */
    @NotNull
    @Column(name = "created_at",nullable = false, updatable = false)
    private LocalDateTime createdAt;


    //Constructors

    /**
     * Default constructor required by JPA
     */
    public Review(){}

    /**
     * Constructor to initialize a new Review object
     *
     * @param user The user who wrote the review
     * @param book The book being reviewed
     * @param rating The rating for the book(1-5)
     * @param text The content of the review
     */
    public Review(User user, Book book, int rating, String text) {
        this.user = user;
        this.book = book;
        this.rating = rating;
        this.text = text;
    }

    /**
     * Auto-assigns the createdAt timestamp before persisting
     * to ensure all new reviews have a valid creation date
     */
    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();   //Date is set automatically before persisting
    }

    //Getters and Setters

    /**
     * Retrieves the unique identifier for the review.
     *
     * @return The review's ID.
     */
     public Long getReviewId() {
        return reviewId;
    }

    /**
     * Retrieves the user who submitted the review.
     *
     * @return The user associated with the review.
     */
    public User getUser() {
        return user;
    }

    /**
     * Sets the user who submitted the review.
     *
     * @param user The user to assign to this review.
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Retrieves the book that the review is associated with.
     *
     * @return The book linked to this review.
     */
    public Book getBook() {
        return book;
    }

    /**
     * Sets the book that this review is related to.
     *
     * @param book The book to assign to this review.
     */
    public void setBook(Book book) {
        this.book = book;
    }

    /**
     * Retrieves the rating given by the user.
     *
     * @return The review's rating (between 1 and 5).
     */
    public int getRating() {
        return rating;
    }

    /**
     * Sets the rating for this review.
     *
     * @param rating The new rating to assign (must be between 1 and 5).
     */
    public void setRating(int rating) {
        this.rating = rating;
    }

    /**
     * Retrieves the text content of the review.
     *
     * @return The review text.
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text content for this review.
     *
     * @param text The new review content to assign.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Retrieves the creation timestamp for the review.
     *
     * This value is automatically assigned before persistence and
     * cannot be changed afterward.
     *
     * @return The timestamp when the review was created.
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }


}
