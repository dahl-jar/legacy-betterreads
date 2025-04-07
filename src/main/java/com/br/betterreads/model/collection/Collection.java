package com.br.betterreads.model.collection;


import com.br.betterreads.model.Book;
import com.br.betterreads.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "collections", schema = "betterreads")
public class Collection {

    @EmbeddedId
    private CollectionId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_Id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("bookId")
    @JoinColumn(name = "book_id")
    private Book book;

    @Enumerated(EnumType.STRING)
    @NotNull
    private CollectionStatus status;

    @NotNull
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void setDate() {
        setCreatedAt(LocalDateTime.now());
    }

    // Constructors
    public Collection() {}

    public Collection(User user, Book books, CollectionStatus status) {
        this.user = user;
        this.book = books;
        this.status = status;
        this.id = new CollectionId(user.getUserId(), book.getBookId());
    }

    // Getters and Setters


    public CollectionId getId() {
        return id;
    }

    public void setId(CollectionId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public CollectionStatus getStatus() {
        return status;
    }

    public void setStatus(CollectionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(@NotNull LocalDateTime added_at) {
        this.createdAt = added_at;
    }
}
