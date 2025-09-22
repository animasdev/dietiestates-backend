package it.dieti.dietiestatesbackend.bootstrap;

import it.dieti.dietiestatesbackend.application.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = "app.bootstrap.superadmin.enabled", havingValue = "true", matchIfMissing = true)
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserService userService;

    public AdminBootstrap(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String password = userService.createDefaultSuperAdminIfMissing();
        if (password != null) {
            // Intentionally log the one-time generated password for first access
            log.warn("Created default SUPERADMIN account. email={}, temporaryPassword={}",
                    "superadmin@dietiestates.local", password);
        } else {
            log.info("SUPERADMIN account already present; no bootstrap action performed.");
        }
    }
}
