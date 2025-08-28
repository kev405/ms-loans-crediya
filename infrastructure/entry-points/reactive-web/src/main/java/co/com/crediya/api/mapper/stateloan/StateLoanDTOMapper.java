package co.com.crediya.api.mapper.stateloan;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import co.com.crediya.api.dto.stateloan.StateLoanRequest;
import co.com.crediya.api.dto.stateloan.StateLoanResponse;
import co.com.crediya.model.stateloan.StateLoan;

@Mapper(componentModel = "spring")
public interface StateLoanDTOMapper {

    // Request -> Domain (id lo genera el sistema/DB)
    @Mapping(target = "id", ignore = true)
    StateLoan toDomain(StateLoanRequest request);

    // Domain -> Response
    StateLoanResponse toResponse(StateLoan domain);
}