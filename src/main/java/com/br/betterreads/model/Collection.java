package com.br.betterreads.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "collection", schema = "betterreads")
public class Collection {

    @Id
    @Column(name = "collection_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long collectionId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User userId;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book bookId;

    @Enumerated(EnumType.STRING)
    private CollectionStatus status;

    @NotNull
    private LocalDateTime added_at;

    @PrePersist
    void setDate() {
        setAdded_at(LocalDateTime.now());
    }

    // Constructors
    public Collection() {}

    public Collection(User user, Book books, CollectionStatus status) {
        this.userId = user;
        this.bookId = books;
        this.status = status;
    }

    // Getters and Setters
    public User getUser() {
        return userId;
    }

    public void setUser(User user) {
        this.userId = user;
    }

    public Book getBookId() {
        return bookId;
    }

    public void setBookId(Book bookId) {
        this.bookId = bookId;
    }

    public CollectionStatus getStatus() {
        return status;
    }

    public void setStatus(CollectionStatus status) {
        this.status = status;
    }

    public LocalDateTime getAdded_at() {
        return added_at;
    }

    public void setAdded_at(@NotNull LocalDateTime added_at) {
        this.added_at = added_at;
    }
}
