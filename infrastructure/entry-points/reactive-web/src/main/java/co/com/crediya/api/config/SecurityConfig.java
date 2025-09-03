package co.com.crediya.api.config;

import reactor.core.publisher.Mono;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {
    @Bean
    SecurityWebFilterChain chain(ServerHttpSecurity http, ConverterJwtToAuth jwtConverter) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers(HttpMethod.POST, "/api/v1/loans").hasRole("CUSTOMER")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(j -> j.jwtAuthenticationConverter(jwtConverter)))
                .build();
    }

    @Bean
    ConverterJwtToAuth jwtAuthConverter() {
        return new ConverterJwtToAuth("roles", "ROLE_");
    }

    public static class ConverterJwtToAuth implements
            org.springframework.core.convert.converter.Converter<Jwt, Mono<AbstractAuthenticationToken>> {
        private final String authoritiesClaim;

        private final String prefix;

        public ConverterJwtToAuth(String authoritiesClaim, String prefix) {
            this.authoritiesClaim = authoritiesClaim;
            this.prefix = prefix;
        }

        @Override
        public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
            var granted = new JwtGrantedAuthoritiesConverter();
            granted.setAuthoritiesClaimName(authoritiesClaim);
            granted.setAuthorityPrefix(prefix);

            var delegate = new JwtAuthenticationConverter();
            delegate.setJwtGrantedAuthoritiesConverter(granted);

            return new ReactiveJwtAuthenticationConverterAdapter(
                    delegate).convert(jwt);
        }
    }
}

