package com.br.betterreads.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "User")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int userId;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username needs to be at least 3 characters long")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @Transient
    private String password;

    @Transient
    private String repeatPassword;

    @Column(name = "encoded_password", nullable = false)
    private String encodedPassword;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public User(String username, String email, String encodedPassword) {
        this.username = username;
        this.email = email;
        this.encodedPassword = encodedPassword;
        createdAt = LocalDateTime.now();
    }

    public User() {}

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public String getRepeatPassword() {
        return repeatPassword;
    }

    public String getHashed_Password() {
        return encodedPassword;
    }

    public void setHashed_Password(String hashedPassword) {
        this.encodedPassword = hashedPassword;
    }

    public LocalDateTime getCreated_at() {
        return createdAt;
    }
}
