package br.com.hotel.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    @Test void hashesAndVerifiesWithoutStoringPlainPassword(){
        String encoded=PasswordHasher.hash("senha-segura");
        assertNotEquals("senha-segura",encoded);
        assertTrue(PasswordHasher.matches("senha-segura",encoded));
        assertFalse(PasswordHasher.matches("outra-senha",encoded));
    }
}
