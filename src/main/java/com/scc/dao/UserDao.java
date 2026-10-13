
package com.scc.dao;

import com.scc.model.User;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;

public class UserDao {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;

    public boolean register(
            String username,
            String password,
            String role
    ) {
        if (username == null || username.isBlank()
                || password == null || password.isEmpty()) {
            System.out.println("Username and password are required.");
            return false;
        }

        if (!role.equals("TEACHER") && !role.equals("STUDENT")) {
            System.out.println("Invalid role.");
            return false;
        }

        String sql = """
                INSERT INTO users (username, password, roles, status)
                VALUES (?, ?, ?, 'PENDING')
                """;

        String passwordHash = hashPassword(password);

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());
            statement.setString(2, passwordHash);
            statement.setString(3, role);

            statement.executeUpdate();

            System.out.println("Registration successful!");
            System.out.println("Account status: PENDING");
            return true;

        } catch (SQLException e) {
            if (e.getMessage() != null
                    && e.getMessage().contains("UNIQUE")) {
                System.out.println("Username already exists.");
            } else {
                System.out.println("Registration failed: "
                        + e.getMessage());
            }
            return false;
        }
    }

    public User login(String username, String password) {
        String sql = """
                SELECT id, username, password, roles, status
                FROM users
                WHERE username = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, username.trim());

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    String storedHash = result.getString("password");

                    if (verifyPassword(password, storedHash)) {
                        return new User(
                                result.getInt("id"),
                                result.getString("username"),
                                storedHash,
                                result.getString("roles"),
                                result.getString("status")
                        );
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Login failed: " + e.getMessage());
        }

        return null;
    }

    private String hashPassword(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);

        byte[] hash = deriveKey(password.toCharArray(), salt);

        return "PBKDF2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    private boolean verifyPassword(
            String password,
            String storedValue
    ) {
        try {
            String[] parts = storedValue.split("\\$");

            if (parts.length != 4 || !parts[0].equals("PBKDF2")) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);

            if (iterations < 100_000 || iterations > 1_000_000) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = deriveKey(password.toCharArray(), salt);

            try {
                return MessageDigest.isEqual(expectedHash, actualHash);
            } finally {
                Arrays.fill(actualHash, (byte) 0);
            }

        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] deriveKey(char[] password, byte[] salt) {
        PBEKeySpec spec =
                new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            return factory.generateSecret(spec).getEncoded();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Password hashing failed.", e
            );
        } finally {
            spec.clearPassword();
            Arrays.fill(password, '\0');
        }
    }
}
