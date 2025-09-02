package co.com.crediya.r2dbc.loan.mapper;

import org.mapstruct.*;
import co.com.crediya.model.loan.Loan;
import co.com.crediya.r2dbc.loan.entity.LoanEntity;
import co.com.crediya.r2dbc.mapper.ValueConverters;

@Mapper(componentModel = "spring", uses = ValueConverters.class)
public interface LoanEntityMapper {
    @Mapping(target = "amount", expression = "java(domain.amount().value())")
    @Mapping(target = "termMonths", expression = "java(domain.termMonths().value())")
    @Mapping(target = "email", expression = "java(domain.email().value())")
    LoanEntity toEntity(Loan domain);

    @Mapping(target = "amount", expression = "java(new Money(entity.getAmount()))")
    @Mapping(target = "termMonths", expression = "java(new TermMonths(entity.getTermMonths()))")
    @Mapping(target = "email", expression = "java(new Email(entity.getEmail()))")
    Loan toDomain(LoanEntity entity);
}
