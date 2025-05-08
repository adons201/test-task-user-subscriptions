package ru.tms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record User(Long id, @NotBlank(message = "Field User.username cannot by blank") String username) {
}
