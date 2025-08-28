package co.com.crediya.api.mapper.loan;

import org.mapstruct.*;
import co.com.crediya.api.dto.loan.*;
import co.com.crediya.model.loan.Loan;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface LoanDTOMapper {

    // DTO -> Domain
    @Mapping(target = "amount", expression = "java(new Money(req.amount()))")
    @Mapping(target = "termMonths", expression = "java(new TermMonths(req.termMonths()))")
    @Mapping(target = "email", expression = "java(new Email(req.email()))")
    @Mapping(target = "id", ignore = true)
    Loan toDomain(CreateLoanRequest req);

    // Domain -> DTO
    @Mapping(target = "amount", expression = "java(domain.amount().value())")
    @Mapping(target = "termMonths", expression = "java(domain.termMonths().value())")
    @Mapping(target = "email", expression = "java(domain.email().value())")
    LoanResponse toResponse(Loan domain);

}
