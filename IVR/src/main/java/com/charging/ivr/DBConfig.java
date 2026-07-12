package com.charging.ivr;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConfig {

    private static final Properties properties = new Properties();

    static {

        try (InputStream input = DBConfig.class.getClassLoader().getResourceAsStream("db.properties")) {

            if (input == null) {
                System.err.println("Unable to load db.properties !");
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
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                properties.getProperty("db.url"),
                properties.getProperty("db.user"),
                properties.getProperty("db.password")
        );
    }

}
