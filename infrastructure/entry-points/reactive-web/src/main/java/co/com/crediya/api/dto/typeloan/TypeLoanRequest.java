package co.com.crediya.api.dto.typeloan;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

public record TypeLoanRequest(
        @Schema(description = "Nombre del tipo", example = "PERSONAL")
        @NotBlank String name,
        @Schema(description = "Monto mínimo permitido", example = "1000.00")
        @NotNull  @DecimalMin("0.00") BigDecimal minimumAmount,
        @Schema(description = "Monto máximo permitido", example = "5000.00")
        @NotNull  @DecimalMin("0.00") BigDecimal maximumAmount,
        @Schema(description = "Tasa de interés (0..1)", example = "0.02")
        @NotNull  @DecimalMin("0.00") BigDecimal annualInterestPercent,
        @Schema(description = "Validación automática al crear un préstamo con este tipo", example = "true")
        boolean automaticValidation
) {}
