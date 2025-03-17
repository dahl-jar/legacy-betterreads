package com.br.betterreads.controller;

import com.br.betterreads.model.User;
import com.br.betterreads.model.ValidationResult;
import com.br.betterreads.service.UserService;
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

}
