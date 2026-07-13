package com.charging.ivr;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DBConfig {

    private static final String CRM_BASE_URL = "http://localhost:8080/CRM/api/phonebook";

    public static Double getBalance(String msisdn) {
        try {
            URL url = new URL(CRM_BASE_URL + "/" + msisdn + "/balance");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse balance from JSON: {"msisdn":"...","balance":"..."}
                String json = response.toString();
                int start = json.indexOf("\"balance\":\"") + 11;
                int end = json.indexOf("\"", start);
                return Double.parseDouble(json.substring(start, end));
            }

            conn.disconnect();
        } catch (Exception e) {
            System.err.println("Failed to get balance from CRM API: " + e.getMessage());
        }

        return null;
    }

}
