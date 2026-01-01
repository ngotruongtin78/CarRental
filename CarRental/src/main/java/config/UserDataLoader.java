package CarRental.example.config;

import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UserDataLoader implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserDataLoader(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Create default admin account if not exists
        if (userRepo.findByUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setFullName("Administrator");
            admin.setEnabled(true);
            admin.setVerified(true);
            userRepo.save(admin);
            System.out.println("Default admin account created - Username: admin");
        }

        // Create default staff account if not exists
        if (userRepo.findByUsername("staff") == null) {
            User staff = new User();
            staff.setUsername("staff");
            staff.setPassword(passwordEncoder.encode("staff123"));
            staff.setRole("STAFF");
            staff.setFullName("Staff Member");
            staff.setEnabled(true);
            staff.setVerified(true);
            userRepo.save(staff);
            System.out.println("Default staff account created - Username: staff");
        }
    }
}
