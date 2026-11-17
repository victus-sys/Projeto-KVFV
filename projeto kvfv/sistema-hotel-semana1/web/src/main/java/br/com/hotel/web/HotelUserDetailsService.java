package br.com.hotel.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.sql.DriverManager;

@Service
public class HotelUserDetailsService implements UserDetailsService {
    @Value("${hotel.database.url}") private String databaseUrl;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try (var c = DriverManager.getConnection(databaseUrl);
             var p = c.prepareStatement("SELECT login, senha, perfil FROM usuario WHERE login = ?")) {
            p.setString(1, username.trim().toLowerCase());
            try (var r = p.executeQuery()) {
                if (!r.next()) throw new UsernameNotFoundException("Conta não encontrada.");
                return User.withUsername(r.getString("login"))
                        .password(r.getString("senha"))
                        .roles(r.getString("perfil"))
                        .build();
            }
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new UsernameNotFoundException("Não foi possível consultar a conta.", e);
        }
    }
}
