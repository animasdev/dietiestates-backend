package it.dieti.dietiestatesbackend.application.user;

import it.dieti.dietiestatesbackend.domain.user.User;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import it.dieti.dietiestatesbackend.domain.user.role.Role;
import it.dieti.dietiestatesbackend.domain.user.role.RoleRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class UserService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String PASSWORD_ALGO = "bcrypt";
    private static final String DEFAULT_SUPERADMIN_EMAIL = "superadmin@dietiestates.local";
    private static final String DEFAULT_SUPERADMIN_NAME = "Super Admin";

    public UserService(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Creates the default SUPERADMIN if missing, generating a random password.
     * Returns the plaintext password if created, or null if already present.
     */
    @Transactional
    public String createDefaultSuperAdminIfMissing() {
        if (userRepository.existsByEmail(DEFAULT_SUPERADMIN_EMAIL)) {
            return null;
        }
        String generatedPassword = generateSecurePassword(24);
        String passwordHash = passwordEncoder.encode(generatedPassword);
        Role role = getSuperAdminRole();
        User user = new User(
                null,
                DEFAULT_SUPERADMIN_NAME,
                DEFAULT_SUPERADMIN_EMAIL,
                true,
                role.id(),
                passwordHash,
                PASSWORD_ALGO,
                null,
                null
        );
        userRepository.insert(user);
        return generatedPassword;
    }

    private Role getSuperAdminRole() {
        return roleRepository.findByCode("SUPERADMIN").orElseThrow();
    }

    private static String generateSecurePassword(int length) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789@$!%*?&";
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
