package co.com.crediya.api.dto.typeloan;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

public record TypeLoanResponse(
        @Schema(description = "ID", example = "8358f15c-62a0-4b1e-bc58-6c9e7e3e6d2e", format = "uuid")
        String id,
        @Schema(description = "Nombre", example = "PERSONAL")
        String name,
        @Schema(description = "Monto mínimo", example = "1000.00")
        BigDecimal minimumAmount,
        @Schema(description = "Monto máximo", example = "5000.00")
        BigDecimal maximumAmount,
        @Schema(description = "Tasa de interés (0..1)", example = "0.02")
        BigDecimal annualInterestPercent,
        @Schema(description = "Validación automática al crear un préstamo con este tipo", example = "true")
        boolean automaticValidation
) {}
