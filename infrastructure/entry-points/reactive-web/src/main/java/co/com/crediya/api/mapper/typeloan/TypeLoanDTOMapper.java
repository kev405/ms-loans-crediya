package co.com.crediya.api.mapper.typeloan;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import co.com.crediya.api.dto.typeloan.TypeLoanRequest;
import co.com.crediya.api.dto.typeloan.TypeLoanResponse;
import co.com.crediya.model.value.InterestRate;
import co.com.crediya.model.value.Money;
import co.com.crediya.model.typeloan.TypeLoan;

@Mapper(componentModel = "spring")
public interface TypeLoanDTOMapper {

    // Request -> Domain
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "minimumAmount",       expression = "java(new Money(request.minimumAmount()))")
    @Mapping(target = "maximumAmount",       expression = "java(new Money(request.maximumAmount()))")
    @Mapping(target = "annualInterestRate",  expression = "java(new InterestRate(request.annualInterestPercent()))")
    TypeLoan toDomain(TypeLoanRequest request);

    // Domain -> Response
    @Mapping(target = "minimumAmount",        expression = "java(domain.minimumAmount().value())")
    @Mapping(target = "maximumAmount",        expression = "java(domain.maximumAmount().value())")
    @Mapping(target = "annualInterestPercent",expression = "java(domain.annualInterestRate().annualPercent())")
    TypeLoanResponse toResponse(TypeLoan domain);
}
