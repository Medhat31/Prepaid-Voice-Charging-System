package com.charging.ivr;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.asteriskjava.fastagi.AgiChannel;
import org.asteriskjava.fastagi.AgiException;
import org.asteriskjava.fastagi.AgiRequest;
import org.asteriskjava.fastagi.AgiScript;
import org.asteriskjava.fastagi.BaseAgiScript;
import org.asteriskjava.fastagi.DefaultAgiServer;
import org.asteriskjava.fastagi.SimpleMappingStrategy;

public class IVR extends BaseAgiScript {

    public static void main(String[] args) throws IOException {

        DefaultAgiServer server = new DefaultAgiServer();

        Map<String, AgiScript> mappings = new HashMap<>();

        mappings.put("balanceCheck", new IVR());

        SimpleMappingStrategy mappingStrategy = new SimpleMappingStrategy();
        mappingStrategy.setMappings(mappings);

        server.setMappingStrategy(mappingStrategy);

        System.out.println("Server is running and listening on port 4573...");
        server.startup();
    }

    @Override
    public void service(AgiRequest request, AgiChannel channel) throws AgiException {
        String choice = "";
        try {
            answer();
            playBilingual(channel, "welcome");

            int attempts = 0;
            int maxAttempts = 3;
            char pressedKey = 0;

            // ==========================================
            //          MAIN LANGUAGE MENU LOOP
            // ==========================================
            while (attempts < maxAttempts) {
                attempts++;
                playBilingual(channel, "for");

                pressedKey = channel.waitForDigit(5000);

                if (pressedKey == '1' || pressedKey == '2') {
                    break;
                }

                if (pressedKey == 0) {
                    playBilingual(channel, "no-input");
                } else {
                    playBilingual(channel, "invalid-number");
                }
            }

            if (attempts >= maxAttempts && (pressedKey != '1' && pressedKey != '2')) {
                playBilingual(channel, "goodbye");
                hangup();
                return;
            }

            String langChoice = String.valueOf(pressedKey);

            if ("1".equals(langChoice)) {
                channel.setVariable("CHANNEL(language)", "ar");
                choice = "ar";
            } else {
                channel.setVariable("CHANNEL(language)", "en");
                choice = "en";
            }

            // ==========================================
            //       MSISDN ENTRY & VALIDATION LOOP
            // ==========================================
            boolean success = false;
            while (!success) {
                String msisdn = getData("enter-phone-" + choice, 20000, 11);
                if (msisdn != null && !msisdn.isEmpty()) {
                    System.out.println("User entered: " + msisdn);

                    Double balance = DBConfig.getBalance(msisdn);
                    if (balance != null) {
                        int pounds = balance.intValue();
                        int piasters = (int) Math.round((balance - pounds) * 100);
                           
                        streamFile("current-balance-" + choice);
                        if ("ar".equals(choice)) {
                            playArabicNumber(pounds);
                            streamFile("pounds-ar");

                            if (piasters > 0) {
                                streamFile("and-ar");
                                playArabicNumber(piasters);
                                streamFile("piasters-ar");
                            }
                        } else {
                            sayNumber(String.valueOf(pounds));
                            streamFile("pounds-en"); // Pounds

                            if (piasters > 0) {
                                streamFile("and-en"); // And
                                sayNumber(String.valueOf(piasters));
                                streamFile("piasters-en"); // Piasters
                            }
                        }

                        success = true;
                        continue;
                    }
                }

                // If we reach here, the MSISDN was empty or not found in DB
                streamFile("invalid-number-" + choice);

                // ==========================================
                //  RETRY MENU LOOP (1 to continue, 2 to end)
                // ==========================================
                int retryAttempts = 0;
                char retryKey = 0;
                boolean endCall = false;

                while (retryAttempts < maxAttempts) {
                    retryAttempts++;
                    streamFile("try-again-menu-" + choice);
                    retryKey = channel.waitForDigit(5000);

                    // In this case break the inner retry loop and restart MSISDN loop
                    if (retryKey == '1') {
                        break;
                    } else if (retryKey == '2') {
                        endCall = true;
                        break;
                    }

                    if (retryKey == 0) {
                        streamFile("no-input-" + choice);
                    } else {
                        streamFile("invalid-option-" + choice);
                    }

                }

                // Check if user chose to end the call OR failed the retry menu 3 times
                if (endCall || (retryAttempts >= maxAttempts && retryKey != '1')) {
                    // Break the outer MSISDN loop, moving to the 'finally' block
                    break;
                }

            }

        } catch (AgiException e) {
            System.err.println("AGI execution error: " + e.getMessage());
        } finally {
            if (!choice.isEmpty()) {
                streamFile("goodbye-" + choice);
            }
            hangup();
        }
    }

    private void playBilingual(AgiChannel channel, String audio) throws AgiException {
        channel.setVariable("CHANNEL(language)", "ar");
        streamFile(audio + "-ar");
        channel.setVariable("CHANNEL(language)", "en");
        streamFile(audio + "-en");
    }

    private void playArabicNumber(int number) throws AgiException {
        if (number >= 0 && number <= 20) {
            streamFile("digits/" + number);
        } else if (number > 20 && number < 100) {
            int units = number % 10;
            int tens = number - units;

            if (units == 0) {
                streamFile("digits/" + tens);
            } else {
                streamFile("digits/" + units);
                streamFile("digits/and");
                streamFile("digits/" + tens);
            }
        } else if (number >= 100 && number < 1000) {
            int hundreds = (number / 100) * 100;
            int remainder = number % 100;

            sayNumber(String.valueOf(hundreds));

            if (remainder > 0) {
                streamFile("digits/and");
                playArabicNumber(remainder);
            }
        } else {
            sayNumber(String.valueOf(number));
        }
    }

}
