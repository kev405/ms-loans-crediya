package co.com.crediya.api.dto.stateloan;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StateLoanRequest", description = "Payload para crear/actualizar un estado de préstamo")
public record StateLoanRequest(
        @Schema(description = "Nombre del estado", example = "PENDING_REVIEW")
        @NotBlank String name,
        @Schema(description = "Descripción del estado", example = "El préstamo está pendiente de revisión")
        String description
) {}
