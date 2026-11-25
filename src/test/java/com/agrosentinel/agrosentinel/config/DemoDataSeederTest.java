package com.agrosentinel.agrosentinel.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DemoDataSeederTest {
    @Test
    void allowsLocalPostgresOnly() {
        assertDoesNotThrow(() ->
                DemoDataSeeder.requireLocalDatasource("jdbc:postgresql://localhost:5432/agrosentinel"));
        assertDoesNotThrow(() ->
                DemoDataSeeder.requireLocalDatasource("jdbc:postgresql://127.0.0.1/agrosentinel"));
        assertThrows(IllegalStateException.class, () ->
                DemoDataSeeder.requireLocalDatasource("jdbc:postgresql://agrosentinel.example.com/db"));
        assertThrows(IllegalStateException.class, () -> DemoDataSeeder.requireLocalDatasource(null));
    }

    @Test
    void allowsRemoteDatasourceOnlyWhenBothHostAndDatabaseMatch() {
        assertDoesNotThrow(() -> DemoDataSeeder.requireAllowedDatasource(
                "jdbc:postgresql://pooler.example.com:5432/demo",
                true,
                "pooler.example.com",
                "demo"));
        assertThrows(IllegalStateException.class, () -> DemoDataSeeder.requireAllowedDatasource(
                "jdbc:postgresql://pooler.example.com:5432/other",
                true,
                "pooler.example.com",
                "demo"));
        assertThrows(IllegalStateException.class, () -> DemoDataSeeder.requireAllowedDatasource(
                "jdbc:postgresql://pooler.example.com:5432/demo",
                false,
                "pooler.example.com",
                "demo"));
    }

    @Test
    void requiresStrongCredentialKeyOnlyForRemoteDatasource() {
        assertDoesNotThrow(() ->
                DemoDataSeeder.requireRemotePasswordKeyIfNeeded("jdbc:postgresql://localhost/demo", null));
        assertThrows(IllegalStateException.class, () ->
                DemoDataSeeder.requireRemotePasswordKeyIfNeeded("jdbc:postgresql://remote.example.com/demo", null));
        assertThrows(IllegalStateException.class, () ->
                DemoDataSeeder.requireRemotePasswordKeyIfNeeded(
                        "jdbc:postgresql://remote.example.com/demo", "short"));
        assertDoesNotThrow(() ->
                DemoDataSeeder.requireRemotePasswordKeyIfNeeded(
                        "jdbc:postgresql://remote.example.com/demo", "0123456789abcdef0123456789abcdef"));
    }

    @Test
    void derivesStableUniqueUrlSafePasswordsForRemoteAccounts() {
        String key = "0123456789abcdef0123456789abcdef";
        String adminPassword = DemoDataSeeder.derivePassword("admin1@gmail.com", key);

        assertTrue(adminPassword.matches("[A-Za-z0-9_-]{43}"));
        assertEquals(adminPassword, DemoDataSeeder.derivePassword("ADMIN1@GMAIL.COM", key));
        assertNotEquals(adminPassword, DemoDataSeeder.derivePassword("tecnico1@gmail.com", key));
    }
}
