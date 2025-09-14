package co.com.crediya.webclient.config;

import lombok.Getter; import lombok.Setter;

@Getter
@Setter
public class AuthProperties {
    private String baseUrl;
    private String existsPath;
    private String emailInfoPath;
}
