package com.br.betterreads.service;

import com.br.betterreads.model.*;
import com.br.betterreads.model.collection.Collection;
import com.br.betterreads.model.collection.CollectionStatus;
import com.br.betterreads.repository.CollectionRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CollectionService {

    private final CollectionRepository collectionRepo;

    public CollectionService(CollectionRepository collectionRepo) {
        this.collectionRepo = collectionRepo;
    }

    @Transactional
    public ValidationResult addToCollection(User user, Book book, CollectionStatus status) {
        Optional<Collection> existing = collectionRepo.findCollectionByUserAndBook(user, book);

        if (existing.isPresent()) {
            Collection collection = existing.get();
            collection.setStatus(status);
            collectionRepo.save(collection);
            return ValidationResult.success();
        }

        Collection collection = new Collection(user, book, status);
        collectionRepo.save(collection);
        return ValidationResult.success();
    }

    @Transactional
    public ValidationResult removeFromCollection(User user, Book book) {
        Collection toDelete = collectionRepo.findCollectionByUserAndBook(user, book).orElse(null);
        if (toDelete == null) return ValidationResult.error("Collection not found");
        collectionRepo.delete(toDelete);
        return ValidationResult.success();
    }
}
