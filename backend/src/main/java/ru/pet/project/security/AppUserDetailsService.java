package ru.pet.project.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.pet.project.user.UserRepository;

//Spring Security вызывает этот класс при входе: «найди пользователя с такой почтой».
//Пароль Spring проверяет сам, сравнивая введённый пароль с bcrypt-хешем.

@Service
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailIgnoreCase(email.trim())
                .map(AuthenticatedUser::new)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));
    }
}