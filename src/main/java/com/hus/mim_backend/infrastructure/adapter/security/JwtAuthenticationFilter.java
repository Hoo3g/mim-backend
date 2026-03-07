package com.hus.mim_backend.infrastructure.adapter.security;

import com.hus.mim_backend.application.port.output.TokenProvider;
import com.hus.mim_backend.application.rbac.usecase.ManageRbacUseCase;
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
import java.util.Set;

/**
 * JWT Authentication Filter — extracts token from Authorization header,
 * validates it, and populates the SecurityContext with email + roles.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenProvider tokenProvider;
    private final ManageRbacUseCase manageRbacUseCase;

    public JwtAuthenticationFilter(TokenProvider tokenProvider, ManageRbacUseCase manageRbacUseCase) {
        this.tokenProvider = tokenProvider;
        this.manageRbacUseCase = manageRbacUseCase;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (tokenProvider.validateToken(token)) {
                String email = tokenProvider.getEmailFromToken(token);
                Set<String> roles = tokenProvider.getRolesFromToken(token);
                Set<String> permissions = Set.of();
                try {
                    Set<String> dbRoles = manageRbacUseCase.getRolesByEmail(email);
                    if (!dbRoles.isEmpty()) {
                        roles = dbRoles;
                    }
                    permissions = manageRbacUseCase.getEffectivePermissionsByEmail(email);
                } catch (RuntimeException ignored) {
                    // Fallback to token roles only when RBAC lookup fails.
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
        }

        filterChain.doFilter(request, response);
    }
}
