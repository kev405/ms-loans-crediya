package co.com.crediya.r2dbc.loan.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table("loan")
public class LoanEntity {

    @Id
    @Column("id")
    private UUID id;

    @Column("amount")
    private BigDecimal amount;

    @Column("term_months")
    private Integer termMonths;

    @Column("email")
    private String email;

    @Column("id_state_loan")
    private UUID stateLoanId;

    @Column("id_type_loan")
    private UUID typeLoanId;
}

