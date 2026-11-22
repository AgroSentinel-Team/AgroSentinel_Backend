package com.agrosentinel.agrosentinel.dtos;

import java.util.List;

public record SpecPresetResponse(String key, String name, String description, List<SpecFieldResponse> fields) {
}
