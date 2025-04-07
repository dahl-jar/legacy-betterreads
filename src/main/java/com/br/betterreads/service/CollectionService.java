package com.br.betterreads.service;

import com.br.betterreads.model.*;
import com.br.betterreads.model.collection.Collection;
import com.br.betterreads.model.collection.CollectionStatus;
import com.br.betterreads.repository.CollectionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
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
    public void removeFromCollection(User user, Book book) {
        Collection toDelete = collectionRepo.findCollectionByUserAndBook(user, book).orElse(null);
        if (toDelete == null) return;
        collectionRepo.delete(toDelete);
    }

    /**
     * Returns a list of Collection objects based on the given user and status. Returns null if the list doesnt exist or is empty
     * @param user User the collection belongs to
     * @param status Status of the collection searched for
     * @return {@link List<Collection>} of Collection or null
     */
    public List<Collection> getCollection(User user, CollectionStatus status) {
        List<Collection> collection = collectionRepo.findCollectionsByUserAndStatus(user, status);
        if (collection.isEmpty()) return null;
        return collection;
    }
}
