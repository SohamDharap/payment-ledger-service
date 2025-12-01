package com.bank.DistributedPaymentLedger.WalletService.config;

import com.bank.DistributedPaymentLedger.WalletService.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private  JwtService jwtService;
    @Autowired
    private  UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 1. Check for token presence and format ("Bearer <token>")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract token (skip "Bearer ")
        jwt = authHeader.substring(7);
        username = jwtService.extractUsername(jwt);

        // 3. If user is found and not yet authenticated
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 4. Load UserDetails from DB (using the bean in ApplicationConfig)
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 5. Validate token signature and expiration
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 6. Create Authentication Token
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. Set authentication context (User is now logged in for this request)
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 8. Continue down the filter chain
        filterChain.doFilter(request, response);
    }
}
