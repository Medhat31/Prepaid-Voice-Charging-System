package com.telecom.repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BalanceRepository implements IBalanceRepository {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public BalanceRepository(String dbUrl, String dbUser, String dbPassword) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL Driver not found on classpath!");
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    @Override
    public boolean userExists(String msisdn) {
        String sql = "SELECT EXISTS(SELECT 1 FROM Users WHERE msisdn = ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, msisdn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBoolean(1);
            }
        } catch (SQLException e) {
            System.err.println("DB Error: " + e.getMessage());
        }
        return false;
    }

    @Override
    public BigDecimal getBalance(String msisdn) {
        String sql = "SELECT balance FROM Users WHERE msisdn = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, msisdn);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("balance");
            }
        } catch (SQLException e) {
            System.err.println("DB Error: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public void deductBalance(String msisdn, BigDecimal amount) {
        String sql = "UPDATE Users SET balance = balance - ? WHERE msisdn = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBigDecimal(1, amount);
            stmt.setString(2, msisdn);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}