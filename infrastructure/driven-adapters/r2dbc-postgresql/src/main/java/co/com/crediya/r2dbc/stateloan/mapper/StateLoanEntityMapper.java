package co.com.crediya.r2dbc.stateloan.mapper;

import co.com.crediya.model.stateloan.StateLoan;
import co.com.crediya.r2dbc.mapper.ValueConverters;
import co.com.crediya.r2dbc.stateloan.entity.StateLoanEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = ValueConverters.class)
public interface StateLoanEntityMapper {
    StateLoanEntity toEntity(StateLoan domain);
    StateLoan toDomain(StateLoanEntity entity);
}

