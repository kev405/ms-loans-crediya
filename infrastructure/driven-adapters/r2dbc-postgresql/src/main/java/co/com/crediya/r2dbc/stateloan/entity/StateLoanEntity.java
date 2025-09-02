package co.com.crediya.r2dbc.stateloan.entity;

import lombok.*;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table("loan_state")
public class StateLoanEntity {
    @Id
    @Column("id")
    private UUID id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;
}
