package com.marketplace.security.jwt;

import com.marketplace.user.entity.User;
import com.marketplace.user.entity.UserStatus;
import com.marketplace.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository  userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            boolean validToken = jwtTokenProvider.validateToken(token);
            if (validToken) {
                String username=jwtTokenProvider.extractUsername(token);
                User user = userRepository.findByUsername(username).orElse(null);
                if (user !=null && user.getStatus() == UserStatus.ACTIVE){
                    List<GrantedAuthority> authorities = user.getRoles()
                            .stream()
                            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(
                                    "ROLE_" + role.getName().name()
                            ))
                            .toList();
                    UsernamePasswordAuthenticationToken authentication=new UsernamePasswordAuthenticationToken(username, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);

    }
}
