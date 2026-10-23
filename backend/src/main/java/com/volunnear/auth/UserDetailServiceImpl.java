package com.volunnear.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {
    private final AppUserRepository appUserRepository;
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        AppUser appUser;
        if (identifier.contains("@")) {
            appUser = appUserRepository.findAppUserByEmail(identifier)
                    .orElseThrow(getUsernameNotFoundExceptionSupplier("User with email ", identifier));
        } else {
            appUser = appUserRepository.findAppUserByUsername(identifier)
                    .orElseThrow(getUsernameNotFoundExceptionSupplier("User with username ", identifier));
        }
        return new CustomUserDetails(appUser);
    }

    private static Supplier<UsernameNotFoundException> getUsernameNotFoundExceptionSupplier(String x, String identifier) {
        return () -> new UsernameNotFoundException(x + identifier + " not found");
    }
}
