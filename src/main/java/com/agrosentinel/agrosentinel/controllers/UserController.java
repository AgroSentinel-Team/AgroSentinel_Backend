package com.agrosentinel.agrosentinel.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.agrosentinel.agrosentinel.dtos.UserOptionResponse;
import com.agrosentinel.agrosentinel.services.UserDirectoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
public class UserController {
    private final UserDirectoryService userDirectoryService;

    @GetMapping
    public List<UserOptionResponse> findAll() {
        return userDirectoryService.findAllOptions();
    }
}