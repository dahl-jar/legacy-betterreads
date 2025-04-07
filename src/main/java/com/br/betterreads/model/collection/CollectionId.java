package com.br.betterreads.model.collection;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class for modeling the composite primary key for Collection
 * implements {@link java.io.Serializable} per @Embeddable requirements
 */
@Embeddable
public class CollectionId implements Serializable {

    @Column(name="user_id")
    private Long userId;

    @Column(name = "book_id")
    private Long bookId;

    public CollectionId() {}

    public CollectionId(Long userId, Long bookId) {
        this.userId = userId;
        this.bookId = bookId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getBookId() {
        return bookId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CollectionId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(bookId, that.bookId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, bookId);
    }
}
