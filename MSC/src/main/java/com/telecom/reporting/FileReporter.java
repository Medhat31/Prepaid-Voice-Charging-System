package com.telecom.reporting;

import com.telecom.domain.CDR;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class FileReporter implements IReporter {
    private final String filePath = "/tmp/calls";

    @Override
    public void report(CDR cdr) {
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write(cdr.toString());
            writer.newLine(); 
        } catch (IOException e) {
            System.err.println("Failed to write CDR to file: " + e.getMessage());
        }
    }
}