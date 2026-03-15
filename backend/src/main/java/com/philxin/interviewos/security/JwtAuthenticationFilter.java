package com.philxin.interviewos.security;

import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.repository.AppUserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 从 Authorization 头解析 JWT，并将用户上下文写入 SecurityContext。
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = JwtTokenService.TOKEN_TYPE + " ";

    private final JwtTokenService jwtTokenService;
    private final AppUserRepository appUserRepository;

    public JwtAuthenticationFilter(
        JwtTokenService jwtTokenService,
        AppUserRepository appUserRepository
    ) {
        this.jwtTokenService = jwtTokenService;
        this.appUserRepository = appUserRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            resolveAuthenticatedUser(request).ifPresent(authenticatedUser -> {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    authenticatedUser,
                    null,
                    List.of()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }

    private java.util.Optional<AuthenticatedUser> resolveAuthenticatedUser(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            return java.util.Optional.empty();
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return java.util.Optional.empty();
        }

        try {
            Long userId = jwtTokenService.parseUserId(token);
            return appUserRepository.findById(userId).map(AuthenticatedUser::fromEntity);
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("Ignore invalid JWT token: type={}", exception.getClass().getSimpleName());
            return java.util.Optional.empty();
        }
    }
}
