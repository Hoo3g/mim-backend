package com.hus.mim_backend.infrastructure.adapter.security;

import com.hus.mim_backend.application.port.output.UserRepository;
import com.hus.mim_backend.application.port.output.TokenProvider;
import com.hus.mim_backend.application.rbac.usecase.ManageRbacUseCase;
import com.hus.mim_backend.domain.auth.model.AccountStatus;
import com.hus.mim_backend.domain.auth.model.Email;
import com.hus.mim_backend.domain.auth.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * JWT Authentication Filter — extracts token from Authorization header,
 * validates it, and populates the SecurityContext with email + roles.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final ManageRbacUseCase manageRbacUseCase;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(TokenProvider tokenProvider, ManageRbacUseCase manageRbacUseCase, UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.manageRbacUseCase = manageRbacUseCase;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String token = null;

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else if (request.getRequestURI().contains("/notifications/stream")) {
            // SSE endpoints: EventSource API cannot set custom headers,
            // so accept JWT from query param as fallback.
            token = request.getParameter("token");
        }

        if (token != null && tokenProvider.validateToken(token)) {
                String email = tokenProvider.getEmailFromToken(token);
                User currentUser = null;
                try {
                    currentUser = userRepository.findByEmail(new Email(email)).orElse(null);
                } catch (RuntimeException ignored) {
                    currentUser = null;
                }

                if (currentUser == null || currentUser.getStatus() == AccountStatus.BLOCKED) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                Set<String> roles = currentUser.getRoles() == null || currentUser.getRoles().isEmpty()
                        ? tokenProvider.getRolesFromToken(token)
                        : currentUser.getRoles().stream()
                                .filter(role -> role != null && !role.isBlank())
                                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                Set<String> permissions = Set.of();
                try {
                    permissions = manageRbacUseCase.getEffectivePermissionsByUserId(currentUser.getId());
                } catch (RuntimeException ignored) {
                    // Fallback to token/user roles only when RBAC lookup fails.
                }

                var authorities = new LinkedHashSet<SimpleGrantedAuthority>();
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
                for (String permission : permissions) {
                    authorities.add(new SimpleGrantedAuthority("PERM_" + permission));
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email,
                        null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
