package it.dieti.dietiestatesbackend.application.auth;

import it.dieti.dietiestatesbackend.application.notification.NotificationService;
import it.dieti.dietiestatesbackend.config.SecurityPasswordProperties;
import it.dieti.dietiestatesbackend.domain.auth.PasswordResetToken;
import it.dieti.dietiestatesbackend.domain.auth.PasswordResetTokenRepository;
import it.dieti.dietiestatesbackend.domain.user.User;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {
    private static final long RESET_TOKEN_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityPasswordProperties passwordProps;
    private final NotificationService notificationService;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                SecurityPasswordProperties passwordProps, NotificationService notificationService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordProps = passwordProps;
        this.notificationService = notificationService;
    }

    @Transactional
    public void requestPasswordResetByEmail(String email) {
        var normalized = email == null ? "" : email.trim();
        if (normalized.isBlank()) return; // Always 202 at API layer

        var userOpt = userRepository.findByEmail(normalized);
        if (userOpt.isEmpty()) return; // 202 at API layer; avoid enumeration
        requestPasswordResetForUser(userOpt.get());
    }

    private void requestPasswordResetForUser(User user) {
        var now = OffsetDateTime.now();
        Optional<PasswordResetToken> existing = tokenRepository.findActiveByUser(user.id(), now);
        if (existing.isPresent()) {
            var t = existing.get();
            var refreshed = new PasswordResetToken(
                    t.id(), t.userId(), t.token(), now.plusMinutes(RESET_TOKEN_TTL_MINUTES), t.consumedAt(), t.createdAt(), now
            );
            tokenRepository.update(refreshed);
            notificationService.sendPasswordReset(user.email(), t.token());
            return;
        }
        String token = generateToken();
        var toInsert = new PasswordResetToken(
                null, user.id(), token, now.plusMinutes(RESET_TOKEN_TTL_MINUTES), null, now, now
        );
        tokenRepository.insert(toInsert);
        notificationService.sendPasswordReset(user.email(), token);
    }

    @Transactional
    public boolean confirmPasswordReset(String token, String newPassword) {
        if (token == null || token.isBlank() || newPassword == null || newPassword.isBlank()) {
            return false;
        }
        var now = OffsetDateTime.now();
        var opt = tokenRepository.findActiveByToken(token, now);
        if (opt.isEmpty()) return false;
        var t = opt.get();

        // Update user password
        var userOpt = userRepository.findById(t.userId());
        if (userOpt.isEmpty()) return false; // should not happen; token references missing user
        var u = userOpt.get();
        var updatedUser = new User(
                u.id(), u.displayName(), u.email(), u.firstAccess(), u.roleId(),
                passwordEncoder.encode(newPassword),
                passwordProps.getPasswordAlgo(),
                u.createdAt(),
                OffsetDateTime.now()
        );
        userRepository.update(updatedUser);

        var consumed = new PasswordResetToken(
                t.id(), t.userId(), t.token(), t.expiresAt(), now, t.createdAt(), now
        );
        tokenRepository.update(consumed);
        return true;
    }

    @Transactional
    public boolean changePasswordForUser(java.util.UUID userId, String oldPassword, String newPassword) {
        if (userId == null || oldPassword == null || oldPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            return false;
        }
        var userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return false;
        var u = userOpt.get();
        if (u.passwordHash() == null || u.passwordHash().isBlank()) return false;
        if (!passwordEncoder.matches(oldPassword, u.passwordHash())) return false;

        var updatedUser = new User(
                u.id(), u.displayName(), u.email(), false, u.roleId(),
                passwordEncoder.encode(newPassword),
                passwordProps.getPasswordAlgo(),
                u.createdAt(),
                OffsetDateTime.now()
        );
        userRepository.update(updatedUser);
        return true;
    }

    private static String generateToken() {
        byte[] buf = new byte[32];
        new SecureRandom().nextBytes(buf);
        return HexFormat.of().withUpperCase().formatHex(buf);
    }
}
