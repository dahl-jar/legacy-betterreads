package com.br.betterreads.controller;

import ch.qos.logback.core.model.Model;
import com.br.betterreads.model.Review;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ReviewController {

    @PostMapping("/book")
    public String newReview(@ModelAttribute("review") Review review, Model model) {
        return "Bok";
    }

}
