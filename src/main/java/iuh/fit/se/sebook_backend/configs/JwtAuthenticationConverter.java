package iuh.fit.se.sebook_backend.configs;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                defaultConverter.convert(jwt).stream(),
                extractAuthoritiesFromScope(jwt).stream()
        ).collect(Collectors.toList());

        return new JwtAuthenticationToken(jwt, authorities);
    }

    private Collection<GrantedAuthority> extractAuthoritiesFromScope(Jwt jwt) {
        Object scope = jwt.getClaim("scope");
        
        if (scope == null) {
            return Collections.emptyList();
        }

        // Xử lý scope có thể là string hoặc array
        if (scope instanceof String) {
            String scopeString = (String) scope;
            // Tách scope string thành các phần (space-separated)
            return Stream.of(scopeString.split("\\s+"))
                    .filter(s -> !s.isEmpty())
                    .map(s -> "SCOPE_" + s.toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else if (scope instanceof java.util.Collection) {
            @SuppressWarnings("unchecked")
            Collection<String> scopes = (Collection<String>) scope;
            return scopes.stream()
                    .filter(s -> s != null && !s.isEmpty())
                    .map(s -> "SCOPE_" + s.toUpperCase())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}

