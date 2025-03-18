package com.br.betterreads.controller;

import com.br.betterreads.repository.BookRepository;
import com.br.betterreads.service.BookService;
import org.springframework.stereotype.Controller;

@Controller
public class BookController {

    private BookRepository bookRepo;
    private BookService bookService;

}
