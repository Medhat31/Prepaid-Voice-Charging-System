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

public class Main {

    public static void main(String[] args) {
        System.out.println("=======================================================");
        System.out.println("        Initializing Telecom Charging System...        ");
        System.out.println("=======================================================");

        String dbUrl = System.getenv("DB_URL");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD"); 
        System.out.println("URL : " + dbUrl + "User : " + dbUser + "Pass : " + dbPass);

        IBalanceRepository balanceRepo = new BalanceRepository(dbUrl, dbUser, dbPass);

        IRateProvider rateProvider = new RateProvider();
        ICharger charger = new Charger(rateProvider);

        ReportingService reportingService = new ReportingService();
        IReporter masterReporter = reportingService;

        IMSC msc = new MSC(balanceRepo, charger, masterReporter);

        ITCPServer tcpServer = new TCPServer(msc);
        IUDPServer udpServer = new UDPServer();

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