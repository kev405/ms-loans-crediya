package co.com.crediya.api.dto.loan;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoanResponse", description = "Respuesta con los datos del préstamo")
public record LoanResponse(
        @Schema(description = "ID del préstamo", example = "a3f1d9a1-1e7e-4f88-b7b0-5e4a2a3f1a2b", format = "uuid")
        String id,
        @Schema(description = "Monto aprobado/solicitado", example = "3000.00", type = "number", format = "double")
        BigDecimal amount,
        @Schema(description = "Plazo en meses", example = "12")
        Integer termMonths,
        @Schema(description = "Email del cliente", example = "user@example.com", format = "email")
        String email,
        @Schema(description = "Nombre del estado actual del préstamo", example = "PENDING_REVIEW")
        String stateLoanId,
        @Schema(description = "ID del tipo de préstamo", example = "6a50dabc-ec06-4972-8eb5-cb4dde555b4e", format = "uuid")
        String typeLoanId
) {}
