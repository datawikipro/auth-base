package pro.datawiki.auth.base.controller;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import pro.datawiki.auth.base.dto.LoginRequestDto;
import pro.datawiki.auth.base.dto.LoginResponseDto;
import pro.datawiki.auth.base.dto.SessionResponseDto;
import pro.datawiki.auth.base.dto.SessionUserDto;
import pro.datawiki.auth.base.repository.UserRepository;
import pro.datawiki.auth.base.security.JwtTokenProvider;
import pro.datawiki.auth.base.service.AuthService;

import java.util.HashMap;
import java.util.Map;

/**
 * Core authentication controller providing /auth/login, logout, and session endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class BaseAuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public LoginResponseDto login(@RequestBody LoginRequestDto req, HttpServletResponse response) {
        SessionUserDto session = authService.authenticate(req.getUsername(), req.getPassword());
        if (session == null) {
            return LoginResponseDto.builder().success(false).error("Invalid username or password").build();
        }
        String token = authService.createToken(session);
        setTokenCookie(response, token, authService.getExpireMinutes());
        return LoginResponseDto.builder().success(true).user(session).token(token).authProvider("email").build();
    }

    @PostMapping("/logout")
    public Map<String, Boolean> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return Map.of("success", true);
    }

    @GetMapping("/session")
    public SessionResponseDto session(@RequestAttribute(required = false) String currentUsername) {
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).map(user -> {
            SessionUserDto session = authService.buildSession(user);
            return SessionResponseDto.builder().success(true).user(session).build();
        }).orElse(SessionResponseDto.builder().success(false).build());
    }

    @GetMapping("/verify-token")
    public Map<String, Object> verifyToken(@RequestParam String token) {
        Claims claims = jwtTokenProvider.parseToken(token);
        if (claims == null) {
            return Map.of("valid", false);
        }
        Map<String, Object> result = new HashMap<>(claims);
        result.put("valid", true);
        return result;
    }

    private void setTokenCookie(HttpServletResponse response, String token, long expireMinutes) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) (expireMinutes * 60));
        cookie.setPath("/");
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
