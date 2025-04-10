package com.br.betterreads.controller;

import com.br.betterreads.model.Book;
import com.br.betterreads.model.User;
import com.br.betterreads.model.ValidationResult;
import com.br.betterreads.model.collection.Collection;
import com.br.betterreads.model.collection.CollectionStatus;
import com.br.betterreads.repository.BookRepository;
import com.br.betterreads.repository.CollectionRepository;
import com.br.betterreads.service.CollectionService;
import com.br.betterreads.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.http.HttpResponse;
import java.util.List;

@Controller
public class CollectionController {

    private final BookRepository bookRepository;
    private final UserService userService;
    private final CollectionService collectionService;
    private final CollectionRepository collectionRepository;

    public CollectionController(BookRepository bookRepository, UserService userService, CollectionService collectionService, CollectionRepository collectionRepository) {
        this.bookRepository = bookRepository;
        this.userService = userService;
        this.collectionService = collectionService;
        this.collectionRepository = collectionRepository;
    }

    @PostMapping("/book/collection/add")
    public String addBookToCollection(@RequestParam String isbn,
                                      @RequestParam String status,
                                      HttpSession session,
                                      RedirectAttributes ra) {

        User user = userService.getLoggedInUser(session);
        Book book = bookRepository.findByIsbn(isbn).getFirst();

//        if (bookIsInCollection(book, user)) return;



        CollectionStatus cStatus = CollectionStatus.valueOf(status);
        ValidationResult result = collectionService.addToCollection(user, book, cStatus);
        if (!result.valid()) {
           ra.addFlashAttribute("error", result.errorMessage());
           return "redirect:/book?isbn=" + isbn;
        }
        return "redirect:/book?isbn=" + isbn;
    }

    @PostMapping("/book/collection/remove")
    public String removeBookFromCollection(@RequestParam String isbn, HttpSession session) {
        User user = userService.getLoggedInUser(session);
        Book book = bookRepository.findByIsbn(isbn).getFirst();

        collectionService.removeFromCollection(user, book);
        return "redirect:/book?isbn=" + isbn;
    }

    @GetMapping("/user/booklist") // Changed path
    public String showBooklist(Model model, HttpSession session)  {
        User user = userService.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);

        List<Collection> currentlyReading = collectionService.getCollection(user, CollectionStatus.CURRENTLY_READING);
        List<Collection> wantToRead = collectionService.getCollection(user, CollectionStatus.WANT_TO_READ);
        List<Collection> haveRead = collectionService.getCollection(user, CollectionStatus.HAVE_READ);

        model.addAttribute("currentlyReading", currentlyReading);
        model.addAttribute("wantToRead", wantToRead);
        model.addAttribute("haveRead", haveRead);

        return "brukerbokliste";
    }
}
