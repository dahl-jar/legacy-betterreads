package com.br.betterreads.repository;

import com.br.betterreads.model.User;
import com.br.betterreads.model.Book;
import com.br.betterreads.model.collection.Collection;
import com.br.betterreads.model.collection.CollectionId;
import com.br.betterreads.model.collection.CollectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollectionRepository extends JpaRepository<Collection, CollectionId> {
    Optional<Collection> findCollectionByUserAndStatus(User user, CollectionStatus status);
    Optional<Collection> findCollectionByUserAndBook(User user, Book book);

    List<Collection> findCollectionsByUserAndStatus(User user, CollectionStatus status);
}
