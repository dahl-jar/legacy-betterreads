package com.br.betterreads.controller;

import com.br.betterreads.model.User;
import com.br.betterreads.model.ValidationResult;
import com.br.betterreads.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/signup")
    public String showSignUpForm(Model model){
        model.addAttribute("user", new User());
        return "Signup";
    }

    @PostMapping("/signup")
    public String register(@Valid @ModelAttribute("user") User user,
                           Model model,
                           BindingResult bindingResult) {

        if(bindingResult.hasErrors()) return "Signup";

        ValidationResult result = userService.createAndSaveUser(user.getUsername(),
                user.getEmail(),
                user.getPassword(),
                user.getRepeatPassword());

        if(!result.valid()) {
            model.addAttribute("error", result.errorMessage());
            return "Signup";
        }

        return "redirect:/Login";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model){
        model.addAttribute("user", new User());
        return "Login";
    }

    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("user") User user,
                               Model model,
                               BindingResult bindingResult,
                               HttpSession session){

        if(bindingResult.hasErrors()){
            model.addAttribute("error", "Invalid input");
            return "/login";
        }

        ValidationResult result = userService.validateUser(user.getEmail(), user.getPassword(), session);

        if(!result.valid()){
            model.addAttribute("error", result.errorMessage());
            return "Login";
        }

        return "redirect:/Hovedside";

    }

    @GetMapping("/Hovedside")
    public String showMainPage(Model model, HttpSession session){
        User loggedInUser = userService.getLoggedInUser(session);

        if(loggedInUser == null){
            return "redirect:/Login";
        }
        model.addAttribute("username", loggedInUser.getUsername());

        return "/Hovedside";
    }

}
