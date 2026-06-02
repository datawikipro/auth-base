package pro.datawiki.auth.base.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import pro.datawiki.auth.base.domain.User;
import pro.datawiki.auth.base.repository.UserRepository;

import java.io.IOException;
import java.util.List;

/**
 * Filter that authenticates requests containing the X-API-Key header.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-Key");

        if (StringUtils.hasText(apiKey)) {
            userRepository.findByApiKey(apiKey).ifPresent(user -> {
                List<SimpleGrantedAuthority> authorities = user.getRoleNames().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .toList();

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("Authenticated user {} via X-API-Key", user.getUsername());
            });
        }

        chain.doFilter(request, response);
    }
}
