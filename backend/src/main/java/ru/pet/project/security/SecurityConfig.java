package ru.pet.project.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import ru.pet.project.api.dto.ApiError;
import ru.pet.project.api.dto.UserResponse;

import java.io.IOException;

//Настройки безопасности
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final ObjectMapper objectMapper; // инструмент превращающий  Java-объекты в JSON

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF-защита включена. Токен лежит в cookie XSRF-TOKEN, фронтенд
                // копирует его в заголовок X-XSRF-TOKEN каждого POST/PUT/PATCH/DELETE.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())

                // Вход: POST /api/v1/auth/login с полями email и password (как HTML-форма).
                .formLogin(form -> form
                        .loginProcessingUrl("/api/v1/auth/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler((request, response, authentication) -> {
                            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
                            writeJson(response, HttpStatus.OK, new UserResponse(user.getId(), user.getUsername()));
                        })
                        // Одно общее сообщение: не подсказываем, что именно неверно — почта или пароль.
                        .failureHandler((request, response, exception) ->
                                writeJson(response, HttpStatus.UNAUTHORIZED, new ApiError("Неверная почта или пароль")))
                        .permitAll())

                // Выход: POST /api/v1/auth/logout → 204 без тела.
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))

                // Запрос без входа получает 401, а не перенаправление на страницу входа.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, Object body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}