package co.com.crediya.api.dto.stateloan;

import jakarta.validation.constraints.NotBlank;

public record StateLoanRequest(
        @NotBlank String name,
        String description
) {}
