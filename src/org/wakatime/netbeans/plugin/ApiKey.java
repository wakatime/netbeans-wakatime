/* ==========================================================
File:        ApiKey.java
Description: Prompts user for api key if it does not exist.
Maintainer:  WakaTime <support@wakatime.com>
License:     BSD, see LICENSE for more details.
Website:     https://wakatime.com/
===========================================================*/

package org.wakatime.netbeans.plugin;

import java.util.UUID;
import org.openide.*;

public final class ApiKey {
    
    public static String promptForApiKey(String defaultKey) {
        NotifyDescriptor.InputLine question = new NotifyDescriptor.InputLine(
            "API Key:",
            "Enter your WakaTime API Key",
            NotifyDescriptor.OK_CANCEL_OPTION,
            NotifyDescriptor.QUESTION_MESSAGE
        );
        question.setInputText(defaultKey);
 
        if (DialogDisplayer.getDefault().notify(question) == NotifyDescriptor.OK_OPTION) {
            String apiKey = question.getInputText();
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
