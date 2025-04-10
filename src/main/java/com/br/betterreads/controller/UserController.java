package com.br.betterreads.controller;

import com.br.betterreads.model.*;
import com.br.betterreads.repository.ReviewRepository;
import com.br.betterreads.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@Controller
public class UserController {


    private final UserService userService;
    private final BookService bookService;
    private final ApiService apiService;
    private final CollectionService collectionService;
    private final ReviewRepository reviewRepository;

    public UserController(UserService userService, BookService bookService, ApiService apiService, CollectionService collectionService, ReviewRepository reviewRepository) {
        this.userService = userService;
        this.bookService = bookService;
        this.apiService = apiService;
        this.collectionService = collectionService;
        this.reviewRepository = reviewRepository;
    }

    @GetMapping("/signup")
    public String showSignUpForm(Model model) {
        model.addAttribute("user", new User());
        return "Signup";
    }

    @PostMapping("/signup")
    public String register(@Valid @ModelAttribute("user") User user,
                           Model model) {

        ValidationResult result = userService.createAndSaveUser(user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRepeatPassword());

        if (!result.valid()) {
            model.addAttribute("error", result.errorMessage());
            return "Signup";
        }

        return "redirect:login";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        model.addAttribute("user", new User());
        return "Login";
    }

    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("user") User user,
                               BindingResult bindingResult,
                               HttpSession session,
                               Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Invalid input");
            return "Login";
        }

        ValidationResult result = userService.validateUser(user.getEmail(), user.getPassword(), session);

        if (!result.valid()) {
            model.addAttribute("error", result.errorMessage());
            return "Login";
        }

        String origin = (String) session.getAttribute("redirectUrl");
        if (origin != null) {
            session.removeAttribute("redirectUrl");
            return "redirect:" + origin;
        }

        return "redirect:/";

    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/")
    public String showMainPage(Model model, HttpSession session) {
        User loggedInUser = userService.getLoggedInUser(session);

        if (loggedInUser != null) {
            model.addAttribute("user", loggedInUser);
        }

        List<Book> trendingBooks = bookService.displayTrending(24);
        model.addAttribute("books", trendingBooks);
        return "BetterReads";
    }

    @GetMapping("/user")
    public String viewProfile(Model model, HttpSession session) {
        User loggedInUser = userService.getLoggedInUser(session);

        if (loggedInUser != null) {
            model.addAttribute("user", loggedInUser);
        }

        return "redirect:/user/profile";
    }

    /**
     * Handles GET-request to /userBooklist and displays the logged-in users book lsit
     *
     * Retrieves the logged-in user and adds it to the model
     * If no user is logged in, it redirects to
     *
     * @param model
     * @param session
     * @return
     */
    @GetMapping("/userBooklist")
    public String showBooklist(Model model, HttpSession session){
        User loggedInUser = userService.getLoggedInUser(session);

        if(loggedInUser == null){
            return "redirect:/login";
        }

        model.addAttribute("user", loggedInUser);

        return "brukerbokliste";
    }


    @GetMapping("/user/profile")
    public String showUserProfile(Model model, HttpSession session) {
        User user = userService.getLoggedInUser(session);
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user);

        List<Review> userReviews = reviewRepository.findByUser(user);
        model.addAttribute("userReviews", userReviews);
        assert userReviews != null;
        model.addAttribute("reviewCount", userReviews.size());

        return "Bruker";
    }

}
