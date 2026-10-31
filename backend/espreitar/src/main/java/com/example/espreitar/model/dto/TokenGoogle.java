package com.example.espreitar.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenGoogle(@NotBlank @Size(max = 8192) String credential) {
}
