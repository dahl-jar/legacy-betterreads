package com.br.betterreads.service;

import com.br.betterreads.model.User;
import com.br.betterreads.model.ValidationResult;
import com.br.betterreads.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ValidationResult createAndSaveUser(String username, String email, String password, String repeatPassword) {

        if(username == null || username.isBlank() ||
                email == null || email.isBlank() ||
                password == null || password.isBlank() ||
                repeatPassword == null || repeatPassword.isBlank()) {
            return ValidationResult.error("All fields are required");
        }

        username = username.trim();
        email = email.trim();


        if(userRepo.findByUsername(username).isPresent()) return ValidationResult.error("Username already exists");
        if(userRepo.findByEmail(email).isPresent()) return ValidationResult.error("Email already exists");
        if(!password.equals(repeatPassword)) return ValidationResult.error("Passwords do not match");

        String encodedPassword = passwordEncoder.encode(password);

        User newUser = new User(username, email, encodedPassword);
        newUser.setPassword(password);
        newUser.setRepeatPassword(repeatPassword);
        userRepo.save(newUser);
        return ValidationResult.success();
    }

    public ValidationResult validateUser(String email, String password, HttpSession session){

        if(email == null || email.isBlank()){
            return ValidationResult.error("Email cannot be empty");
        }

        if(password == null || password.isBlank()){
            return ValidationResult.error("Password cannot be empty");
        }

        Optional<User> userOptional = userRepo.findByEmail(email);

        if(userOptional.isEmpty()){
            return ValidationResult.error("No account with this email");
        }

        if(!passwordEncoder.matches(password, userOptional.get().getHashed_Password())){
            return ValidationResult.error("Incorrect password");
        }

        session.setAttribute("loggedInUser", userOptional.get());

        return ValidationResult.success();
    }

    public void logoutUser(HttpSession session){
            session.invalidate();
    }

    public User getLoggedInUser(HttpSession session){
        return (User) session.getAttribute("loggedInUser");
    }


}
