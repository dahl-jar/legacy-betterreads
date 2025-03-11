package com.br.betterreads.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "User")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "User_id")
    private int userId;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    @Column(name = "Username", nullable = false)
    private String username;

    @NotBlank(message = "Email is required")
    @Column(name = "Email")
    private String email;

    @NotNull
    @Column(name = "Salt_Password", nullable = false)
    private String saltPassword;

    @NotNull
    @Column(name = "Hashed_Password", nullable = false)
    private String hashedPassword;

    @NotNull
    @Column(name = "Created_at", nullable = false)
    private LocalDateTime createdAt;

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public @NotBlank(message = "Username is required") String getUsername() {
        return username;
    }

    public void setUsername(@NotBlank(message = "Username is required") String username) {
        this.username = username;
    }

    public @NotBlank(message = "Email is required") String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank(message = "Email is required") String email) {
        this.email = email;
    }

    public String getSalt_Password() {
        return saltPassword;
    }

    public void setSalt_Password(String saltPassword) {
        this.saltPassword = saltPassword;
    }

    public String getHashed_Password() {
        return hashedPassword;
    }

    public void setHashed_Password(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public LocalDateTime getCreated_at() {
        return createdAt;
    }

    public void setCreated_at(LocalDateTime created_at) {
        createdAt = created_at;
    }
}
