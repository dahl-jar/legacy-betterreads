package com.br.betterreads.service;

import com.br.betterreads.model.*;
import com.br.betterreads.repository.CollectionRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

@Service
public class CollectionService {

    private final CollectionRepository collectionRepo;

    public CollectionService(CollectionRepository collectionRepo) {
        this.collectionRepo = collectionRepo;
    }

    @Transactional
    public ValidationResult addCollection(User user, Book book, CollectionStatus status) {
        Collection collection = new Collection(user, book, status);
        collectionRepo.save(collection);
        return ValidationResult.success();
    }

    @Transactional
    public ValidationResult addBookToCollection(Collection collection, Book book) {




        return ValidationResult.success();
    }

    public void initCollection(@Valid User user) {
        collectionRepo.save(new Collection(user, null, CollectionStatus.READ));
        collectionRepo.save(new Collection(user, null, CollectionStatus.WANT_TO_READ));
        collectionRepo.save(new Collection(user, null, CollectionStatus.CURRENTLY_READING));
    }
}
