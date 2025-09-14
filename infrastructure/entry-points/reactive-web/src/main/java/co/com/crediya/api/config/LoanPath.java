package co.com.crediya.api.config;

import lombok.Getter; import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "routes.paths")
public class LoanPath {
    private String loans;
    private String loansPageable;
    private String loansChangeStatus;
    private String loansById;
}
