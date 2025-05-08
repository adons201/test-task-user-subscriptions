package ru.tms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record Subscription(Long id,
                           @NotBlank(message = "Field Subscription.name cannot by blank") String name,
                           @NotNull(message = "Field Subscription.user cannot by null") Long user) {
}
