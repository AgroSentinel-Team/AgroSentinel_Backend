package com.agrosentinel.agrosentinel.dtos;

import java.util.Set;

public record UserOptionResponse(Long id, String nombre, String rol, Set<String> roles) {
}