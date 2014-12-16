/* ==========================================================
File:        ApiKey.java
Description: Prompts user for api key if it does not exist.
Maintainer:  WakaTime <support@wakatime.com>
License:     BSD, see LICENSE for more details.
Website:     https://wakatime.com/
===========================================================*/

package org.wakatime.netbeans.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import org.openide.*;
import org.openide.util.NbPreferences;

public final class ApiKey {
    
    public static String getApiKey() {
        String apiKey = NbPreferences.forModule(WakaTime.class).get("API Key", "");
        if (apiKey.equals("")) {
            apiKey = getApiKeyFromConfigFile();
        }
        return apiKey;
    }
    
    public static String getApiKeyFromConfigFile() {
        String apiKey = "";
        File userHome = new File(System.getProperty("user.home"));
        File configFile = new File(userHome, WakaTime.CONFIG);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(configFile.getAbsolutePath()));
        } catch (FileNotFoundException e1) {}
        if (br != null) {
            try {
                String line = br.readLine();
                while (line != null) {
                    String[] parts = line.split("=");
                    if (parts.length == 2 && parts[0].trim().equals("api_key")) {
                        apiKey = parts[1].trim();
                    }
                    line = br.readLine();
                }
            } catch (IOException e) {
                WakaTime.error(e.toString());
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    WakaTime.error(e.toString());
                    e.printStackTrace();
                }
            }
        }
        return apiKey;
    }
    
    public static void saveApiKey(String apiKey) {
        NbPreferences.forModule(WakaTime.class).put("API Key", apiKey);
        saveApiKeyToConfigFile(apiKey);
    }
    
    public static void saveApiKeyToConfigFile(String apiKey) {
        File userHome = new File(System.getProperty("user.home"));
        File configFile = new File(userHome, WakaTime.CONFIG);
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        boolean found = false;
        try {
            br = new BufferedReader(new FileReader(configFile.getAbsolutePath()));
        } catch (FileNotFoundException e1) {
        }
        if (br != null) {
            try {
                String line = br.readLine();
                while (line != null) {
                    String[] parts = line.split("=");
                    if (parts.length == 2 && parts[0].trim().equals("api_key")) {
                        found = true;
                        sb.append("api_key = " + apiKey + "\n");
                    } else {
                        sb.append(line + "\n");
                    }
                    line = br.readLine();
                }
            } catch (IOException e) {
                WakaTime.error(e.toString());
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    WakaTime.error(e.toString());
                    e.printStackTrace();
                }
            }
        }
        if (!found) {
            sb = new StringBuilder();
            sb.append("[settings]\n");
            sb.append("api_key = " + apiKey + "\n");
            sb.append("debug = false\n");
        }
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(configFile.getAbsolutePath(), "UTF-8");
        } catch (FileNotFoundException e) {
            WakaTime.error(e.toString());
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            WakaTime.error(e.toString());
            e.printStackTrace();
        }
        if (writer != null) {
            writer.print(sb.toString());
            writer.close();
        }
    }
    
    public static String promptForApiKey() {
        String apiKey = getApiKey();
        NotifyDescriptor.InputLine question = new NotifyDescriptor.InputLine(
            "API Key:",
            "Enter your WakaTime API Key",
            NotifyDescriptor.OK_CANCEL_OPTION,
            NotifyDescriptor.QUESTION_MESSAGE
        );
        question.setInputText(apiKey);
 
        if (DialogDisplayer.getDefault().notify(question) == NotifyDescriptor.OK_OPTION) {
            apiKey = question.getInputText();
            if (ApiKey.isValidApiKey(apiKey)) {
                return apiKey;
            }
        }
        
        return null;
    }
    
    public static boolean isValidApiKey(String apiKey) {
        try {
            UUID.fromString(apiKey);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
