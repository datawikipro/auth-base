package pro.datawiki.auth.base.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.datawiki.auth.base.dto.HealthResponseDto;
import pro.datawiki.auth.base.service.UserService;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class BaseHealthController {

    private final UserService userService;

    @GetMapping({"/", ""})
    public HealthResponseDto health() {
        return HealthResponseDto.builder()
                .status("healthy").database("connected").version("1.0.0").build();
    }

    @GetMapping("/health")
    public HealthResponseDto healthCheck() {
        long count = userService.countUsers();
        return HealthResponseDto.builder()
                .status("healthy").database("connected: " + count + " users").version("1.0.0").build();
    }
}
