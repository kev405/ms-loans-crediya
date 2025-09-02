package co.com.crediya.api.dto.loan;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateLoanRequest", description = "Payload para crear un préstamo")
public record CreateLoanRequest(
        @Schema(description = "Monto solicitado",
                example = "3000.00", minimum = "0", type = "number", format = "double")
        @NotNull  @DecimalMin(value = "0.00") BigDecimal amount,
        @Schema(description = "Plazo en meses",
                example = "12", minimum = "1")
        @NotNull  @Min(1) Integer termMonths,
        @Schema(description = "Email del cliente solicitante",
                example = "user@example.com", format = "email")
        @NotBlank @Email  String email,
        @Schema(description = "ID del tipo de préstamo",
                example = "6a50dabc-ec06-4972-8eb5-cb4dde555b4e", format = "uuid")
        @NotBlank String typeLoanId,
        @Schema(description = "Nombre del estado actual del préstamo", example = "PENDING_REVIEW")
        String stateLoanId
) {}
