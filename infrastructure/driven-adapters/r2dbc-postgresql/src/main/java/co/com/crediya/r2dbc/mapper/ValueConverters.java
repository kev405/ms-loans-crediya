package co.com.crediya.r2dbc.mapper;


import java.util.UUID;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class ValueConverters {
    @Named("uuidToString") public String uuidToString(UUID id) { return id == null ? null : id.toString(); }
    @Named("stringToUuid") public UUID stringToUuid(String id) { return id == null ? null : UUID.fromString(id); }
}
