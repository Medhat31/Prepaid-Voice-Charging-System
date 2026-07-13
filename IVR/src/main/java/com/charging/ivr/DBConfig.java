package com.charging.ivr;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class DBConfig {

    private static final Properties properties = new Properties();

    static {

        try (InputStream input = DBConfig.class.getClassLoader().getResourceAsStream("db.properties")) {

            if (input == null) {
                System.err.println("Unable to load db.properties!");
            } else {
                properties.load(input);
                Class.forName("org.postgresql.Driver");
                System.out.println("Database properties loaded successfully");
            }

        } catch (Exception e) {
            System.err.println("Failed to load DB config");
            e.printStackTrace();
        }

    }
    
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                properties.getProperty("db.url"),
                properties.getProperty("db.user"),
                properties.getProperty("db.password")
        );
    }
    
     public static Double getBalance(String msisdn) {
        
        String query = "SELECT balance FROM user_balance WHERE msisdn = ?";
        
        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            
           stmt.setString(1, msisdn);
           
           try (ResultSet rs = stmt.executeQuery()) {
               if (rs.next()) {
                   return rs.getDouble("balance");
               }
           }
            
        } catch (SQLException e) {
            System.err.println("Database query failed:" + e.getMessage());
        }
        
        return null;  // Returns null if phone number doesn't exist in DB
    }
    


}
