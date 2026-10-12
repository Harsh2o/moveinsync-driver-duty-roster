package com.moveinsync.roster.config;

import com.moveinsync.roster.model.entity.User;
import com.moveinsync.roster.model.enums.UserRole;
import com.moveinsync.roster.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Bootstraps an initial admin user for local development.
 * Only creates the admin if no users exist in the database.
 * In production, use environment variables or a secure secret-injection mechanism.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            User admin = new User("admin", passwordEncoder.encode("admin123"), "System Administrator", UserRole.ADMIN);
            userRepository.save(admin);
            log.info("Initial admin user created: username=admin");

            User approver = new User("approver", passwordEncoder.encode("approver123"), "Roster Approver", UserRole.APPROVER);
            userRepository.save(approver);
            log.info("Initial approver user created: username=approver");

            User employee = new User("employee", passwordEncoder.encode("employee123"), "Employee User", UserRole.EMPLOYEE);
            userRepository.save(employee);
            log.info("Initial employee user created: username=employee");
        }
    }
}
