package ru.pet.project.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String email, String password) {
        String normalizedEmail = email.trim();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyUsedException(normalizedEmail);
        }
        try {
            return userRepository.saveAndFlush(new User(normalizedEmail, passwordEncoder.encode(password)));
        } catch (DataIntegrityViolationException e) {
            // Два одновременных запроса с одной почтой
            // проверку выше прошли оба, но второй остановил уникальный индекс users_email_lower_uq
            throw new EmailAlreadyUsedException(normalizedEmail);
        }
    }


     //Одна команда DELETE FROM users, всё остальное база удалит каскадом
    @Transactional
    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }
}