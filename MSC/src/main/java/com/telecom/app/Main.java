package com.telecom.app;

import com.telecom.charging.Charger;
import com.telecom.charging.ICharger;
import com.telecom.charging.IRateProvider;
import com.telecom.charging.RateProvider;
import com.telecom.network.ITCPServer;
import com.telecom.network.IUDPServer;
import com.telecom.network.TCPServer;
import com.telecom.network.UDPServer;
import com.telecom.reporting.IReporter;
import com.telecom.reporting.ReportingService;
import com.telecom.repository.BalanceRepository;
import com.telecom.repository.IBalanceRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("        Initializing Telecom Charging System...        ");
        System.out.println("=======================================================");

        String dbUrl = "";
        String dbUser = "";
        String dbPass = "";

        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                System.err.println("Unable to find database credentials file (db.properties)");
                return;
            }
            
            Properties prop = new Properties();
            prop.load(input);
            
            dbUrl = prop.getProperty("db.url");
            dbUser = prop.getProperty("db.user");
            dbPass = prop.getProperty("db.password");
        } catch (IOException ex) {
            System.err.println("An error occurred while reading the properties file.");
            ex.printStackTrace();
            return;
        }    

        IBalanceRepository balanceRepo = new BalanceRepository(dbUrl, dbUser, dbPass);

        IRateProvider rateProvider = new RateProvider();
        ICharger charger = new Charger(rateProvider);

        ReportingService reportingService = new ReportingService();
        IReporter masterReporter = reportingService;

        IMSC msc = new MSC(balanceRepo, charger, masterReporter);

        IUDPServer udpServer = new UDPServer();
        ITCPServer tcpServer = new TCPServer(msc, udpServer);

        int tcpPort = 59090;
        int udpPort = 59091;

        udpServer.startListening(udpPort);

        new Thread(() -> {
            tcpServer.listen(tcpPort);
        }, "TCP-Signaling-Thread").start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nIntercepted termination signal. Powering down cleanly...");
            tcpServer.stopServer();
            udpServer.stopListening();
            System.out.println("Core engines safely offline. Goodbye.");
        }));
    }
}
