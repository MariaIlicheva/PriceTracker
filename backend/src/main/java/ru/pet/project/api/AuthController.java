package ru.pet.project.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.pet.project.api.dto.CsrfResponse;
import ru.pet.project.api.dto.RegisterRequest;
import ru.pet.project.api.dto.UserResponse;
import ru.pet.project.security.AuthenticatedUser;
import ru.pet.project.user.User;
import ru.pet.project.user.UserService;


//Регистрация, CSRF-токен и текущий пользователь.
//Вход (POST /api/v1/auth/login) и выход (POST /api/v1/auth/logout)

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/auth/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request.email(), request.password());
        return UserResponse.from(user);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return new UserResponse(user.getId(), user.getUsername());
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(@AuthenticationPrincipal AuthenticatedUser user,
                         Authentication authentication,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        userService.delete(user.getId());
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }
}