package br.com.hotel.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new HotelPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login", "/cadastro", "/css/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,"/hospedes/*/excluir").hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.DELETE,"/api/v1/hospedes/**").hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers("/hospedes/**").hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers(HttpMethod.POST,"/reservas/canceladas/excluir").hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers("/reservas/*/confirmar", "/reservas/*/checkin", "/reservas/*/checkout").hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers("/api/v1/minha-conta").hasRole("HOSPEDE")
                        .requestMatchers("/api/v1/hospedes/**").hasAnyRole("ADMIN", "RECEPCIONISTA")
                        .requestMatchers("/minha-conta/**").hasRole("HOSPEDE")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").successHandler((request, response, authentication) -> {
                    String perfil = request.getParameter("perfil");
                    String role = authentication.getAuthorities().stream().map(a -> a.getAuthority()).findFirst().orElse("");
                    boolean correto = ("admin".equals(perfil) && role.equals("ROLE_ADMIN"))
                            || ("funcionario".equals(perfil) && role.equals("ROLE_RECEPCIONISTA"))
                            || ("hospede".equals(perfil) && role.equals("ROLE_HOSPEDE"));
                    if (correto) response.sendRedirect("/");
                    else {
                        new SecurityContextLogoutHandler().logout(request, response, authentication);
                        response.sendRedirect("/login?perfil=" + (perfil == null ? "admin" : perfil) + "&tipoInvalido");
                    }
                }).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .build();
    }
}
