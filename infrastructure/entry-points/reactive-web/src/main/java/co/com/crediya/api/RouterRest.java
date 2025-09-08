package co.com.crediya.api;

import co.com.crediya.api.config.LoanPath;
import co.com.crediya.api.dto.pageable.ManualReviewQuery;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Configuration
@RequiredArgsConstructor
public class RouterRest {

    private final LoanPath paths;

    @Bean
    @RouterOperations({
        // POST /api/v1/loans
        @RouterOperation(
                path = "/api/v1/loans",
                produces = MediaType.APPLICATION_JSON_VALUE,
                consumes = MediaType.APPLICATION_JSON_VALUE,
                method = RequestMethod.POST,
                beanClass = Handler.class,
                beanMethod = "createLoan",
                operation = @Operation(
                        operationId = "createLoan",
                        summary = "Crear préstamo",
                        tags = {"Loans"},
                        requestBody = @RequestBody(required = true,
                                content = @Content(
                                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                                        schema = @Schema(implementation = co.com.crediya.api.dto.loan.CreateLoanRequest.class)
                                )
                        ),
                        responses = {
                                @ApiResponse(
                                        responseCode = "201",
                                        description = "Préstamo creado",
                                        content = @Content(schema = @Schema(implementation = co.com.crediya.api.dto.loan.LoanResponse.class))
                                ),
                                @ApiResponse(responseCode = "400", description = "Petición inválida", content = @Content(mediaType = "application/problem+json")),
                                @ApiResponse(responseCode = "409", description = "Conflicto de negocio", content = @Content(mediaType = "application/problem+json")),
                                @ApiResponse(responseCode = "422", description = "Validación de negocio", content = @Content(mediaType = "application/problem+json")),
                                @ApiResponse(responseCode = "500", description = "Error del servidor", content = @Content(mediaType = "application/problem+json"))
                        }
                )
        ),

        // GET /api/v1/loans  (NDJSON)
        @RouterOperation(
                path = "/api/v1/loans",
                produces = MediaType.APPLICATION_NDJSON_VALUE,
                method = RequestMethod.GET,
                beanClass = Handler.class,
                beanMethod = "getAllLoans",
                operation = @Operation(
                        operationId = "getAllLoans",
                        summary = "Listar préstamos (stream NDJSON)",
                        tags = {"Loans"},
                        responses = {
                                @ApiResponse(
                                        responseCode = "200",
                                        description = "OK",
                                        content = @Content(
                                                mediaType = MediaType.APPLICATION_NDJSON_VALUE,
                                                array = @ArraySchema(schema = @Schema(implementation = co.com.crediya.api.dto.loan.LoanResponse.class))
                                        )
                                ),
                                @ApiResponse(responseCode = "500", description = "Error del servidor", content = @Content(mediaType = "application/problem+json"))
                        }
                )
        ),

        // GET /api/loans/{id}
        @RouterOperation(
                path = "/api/v1/loans/{id}",
                produces = MediaType.APPLICATION_JSON_VALUE,
                method = RequestMethod.GET,
                beanClass = Handler.class,
                beanMethod = "getLoanById",
                operation = @Operation(
                        operationId = "getLoanById",
                        summary = "Obtener préstamo por ID",
                        tags = {"Loans"},
                        parameters = {
                                @Parameter(name = "id", in = ParameterIn.PATH, required = true, description = "ID del préstamo", schema = @Schema(format = "uuid"))
                        },
                        responses = {
                                @ApiResponse(responseCode = "200", description = "Encontrado", content = @Content(schema = @Schema(implementation = co.com.crediya.api.dto.loan.LoanResponse.class))),
                                @ApiResponse(responseCode = "204", description = "Sin contenido"), // si por ahora devuelves 204
                                @ApiResponse(responseCode = "404", description = "No encontrado", content = @Content(mediaType = "application/problem+json")),
                                @ApiResponse(responseCode = "500", description = "Error del servidor", content = @Content(mediaType = "application/problem+json"))
                        }
                )
        )
    })
    @RouterOperation(
            path = "/api/v1/loans",
            method = RequestMethod.GET,
            beanClass = ManualReviewQuery.class,
            beanMethod = "list",
            operation = @Operation(
                    operationId = "listManualReview",
                    summary = "Listado paginado/filtrable de prestamos para revisión manual",
                    tags = {"Loans"},
                    security = { @SecurityRequirement(name = "bearerAuth") },
                    parameters = {
                            @Parameter(name="search", description="Filtra por email/nombre (contiene)"),
                            @Parameter(name="status", description="CSV de estados. Default: PENDING_REVIEW,REJECTED,MANUAL_REVIEW"),
                            @Parameter(name="typeLoanId", description="Filtro por tipo préstamo"),
                            @Parameter(name="minAmount"),
                            @Parameter(name="maxAmount"),
                            @Parameter(name="page", schema=@Schema(type="integer", defaultValue="0")),
                            @Parameter(name="size", schema=@Schema(type="integer", defaultValue="20"))
                    },
                    responses = {
                            @ApiResponse(responseCode="200", description="OK"),
                            @ApiResponse(responseCode="401", description="Unauthorized"),
                            @ApiResponse(responseCode="403", description="Forbidden")
                    }
            )
    )
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(POST(paths.getLoans()), handler::createLoan)
//                .andRoute(GET(paths.getLoans()), handler::getAllLoans)
                .andRoute(GET(paths.getLoans()), handler::list)
                .andRoute(GET(paths.getLoansById()), handler::getLoanById);
    }
}
