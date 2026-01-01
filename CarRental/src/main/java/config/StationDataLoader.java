package CarRental.example.config;

import CarRental.example.document.User;
import CarRental.example.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class StationDataLoader implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public StationDataLoader(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepo.findByUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");


            admin.setPassword(passwordEncoder.encode("admin"));

            admin.setFullName("System Administrator");
            admin.setRole("ADMIN");
            admin.setEnabled(true);
            admin.setVerified(true);

            userRepo.save(admin);
            System.out.println(">>> Tài khoản Admin đã được tạo thành công: admin / admin");
        }
    }
}