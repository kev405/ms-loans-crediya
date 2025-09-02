package co.com.crediya.api.dto.stateloan;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StateLoanResponse", description = "Respuesta de estado de préstamo")
public record StateLoanResponse(
        @Schema(description = "ID del estado", example = "02b7d8f0-1b4d-4a5b-9bb7-2e3a8d9c4f1e", format = "uuid")
        String id,
        @Schema(description = "Nombre del estado", example = "PENDING_REVIEW")
        String name,
        @Schema(description = "Descripción del estado", example = "El préstamo está pendiente de revisión")
        String description
) {}
