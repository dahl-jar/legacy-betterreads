package com.br.betterreads.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Entity
@Table(name = "collection")
@IdClass(Collection.class)
public class Collection {


    @Id
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @Id
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    @NotNull
    private Book book;

    @NotNull
    private Status status;

    @NotNull
    private LocalDateTime added_at;



    // Constructors
    public Collection() {}

    public Collection(User user, Book book, Status status, LocalDateTime added_at) {
        this.user = user;
        this.book = book;
        this.status = status;
        this.added_at = added_at;
    }


    // Getters and Setters
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getAdded_at() {
        return added_at;
    }

    public void setAddedAt(LocalDateTime added_at) {
        this.added_at = added_at;
    }

    public enum Status{
        READ,
        WANT_TO_READ,
        CURRENTLY_READING
    }

}
