package com.volunnear.auth;

import com.volunnear.auth.dto.LoginRequestDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityFacade {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public void authenticateAndCreateSession(LoginRequestDto requestDto,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        UsernamePasswordAuthenticationToken unauthenticatedToken = new UsernamePasswordAuthenticationToken(
                requestDto.username(),
                requestDto.password()
        );

        Authentication authentication = authenticationManager.authenticate(unauthenticatedToken);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }
}
