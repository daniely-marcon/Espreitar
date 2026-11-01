package com.example.espreitar.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record LimiteDenuncias(@Min(1) @Max(100) int limiteDenuncias) {
}
