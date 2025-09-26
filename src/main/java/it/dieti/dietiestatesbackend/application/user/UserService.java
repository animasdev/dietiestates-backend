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
import java.util.UUID;

@Service
public class UserService {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final it.dieti.dietiestatesbackend.config.SecurityPasswordProperties passwordProps;

    private static final String DEFAULT_SUPERADMIN_EMAIL = "superadmin@dietiestates.local";
    private static final String DEFAULT_SUPERADMIN_NAME = "Super Admin";

    public UserService(RoleRepository roleRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       it.dieti.dietiestatesbackend.config.SecurityPasswordProperties passwordProps) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordProps = passwordProps;
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
                passwordProps.getPasswordAlgo(),
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


    public User findUserById(UUID id) {
        return userRepository.findById(id).orElseThrow();
    }

    public String getRoleCode(UUID roleId) {
        return roleRepository.findById(roleId).orElseThrow().code();
    }

    public Role getCodeFromRole(UUID roleId) {
        return roleRepository.findById(roleId).orElseThrow();
    }
}
