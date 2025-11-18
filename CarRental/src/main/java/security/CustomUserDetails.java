package CarRental.example.security;

import CarRental.example.document.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleValue = user.getRole();
        if (roleValue == null || roleValue.isBlank()) {
            roleValue = "USER";
        }

        String[] roles = roleValue.split(",");

        List<String> normalized = Arrays.stream(roles)
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r.toUpperCase())
                .collect(Collectors.toList());

        if (normalized.isEmpty()) {
            normalized = List.of("ROLE_USER");
        }

        List<String> finalRoles = normalized;
        return finalRoles.stream().map(role -> (GrantedAuthority) () -> role).collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return user.isEnabled(); }

    public User getUser() {
        return this.user;
    }
}
