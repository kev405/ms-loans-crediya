package co.com.crediya.r2dbc.typeloan.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import co.com.crediya.model.typeloan.TypeLoan;
import co.com.crediya.r2dbc.mapper.ValueConverters;
import co.com.crediya.r2dbc.typeloan.entity.TypeLoanEntity;

@Mapper(componentModel = "spring", uses = ValueConverters.class)
public interface TypeLoanEntityMapper {

    // Domain -> Entity
    @Mapping(target = "minimumAmount",        expression = "java(domain.minimumAmount().value())")
    @Mapping(target = "maximumAmount",        expression = "java(domain.maximumAmount().value())")
    @Mapping(target = "annualInterestPercent",expression = "java(domain.annualInterestRate().annualPercent())")
    @Mapping(target = "automaticValidation",  source = "automaticValidation") // primitivo -> wrapper
    TypeLoanEntity toEntity(TypeLoan domain);

    // Entity -> Domain
    @Mapping(target = "minimumAmount",       expression = "java(new Money(entity.getMinimumAmount()))")
    @Mapping(target = "maximumAmount",       expression = "java(new Money(entity.getMaximumAmount()))")
    @Mapping(target = "annualInterestRate",  expression = "java(new InterestRate(entity.getAnnualInterestPercent()))")
    @Mapping(target = "automaticValidation", source = "automaticValidation") // wrapper -> primitivo (MapStruct maneja null=>false si necesitas, o ajusta)
    TypeLoan toDomain(TypeLoanEntity entity);
}