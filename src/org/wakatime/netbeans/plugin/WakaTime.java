/* ==========================================================
File:        WakaTime.java
Description: Runs after GUI is ready.
Maintainer:  WakaTime <support@wakatime.com>
License:     BSD, see LICENSE for more details.
Website:     https://wakatime.com/
===========================================================*/

package org.wakatime.netbeans.plugin;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.openide.*;
import org.openide.modules.ModuleInstall;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;

@OnShowing
public class WakaTime extends ModuleInstall implements Runnable {
    public static final String CODENAME = "org.wakatime.netbeans.plugin";
    public static final String IDE_NAME = "NetBeans";
    public static final String CONFIG = ".wakatime.cfg";
    public static final short FREQUENCY = 2; // minutes between pings
    public static final Logger log = Logger.getLogger("WakaTime");

    public static String VERSION = "Unknown";
    public static String IDE_VERSION = "Unknown";
    public static String lastFile = null;
    public static long lastTime = 0;
    public static Boolean DEBUG = false;
    public static CustomDocumentListener documentListener = null;

    @Override
    public void run() {
        WakaTime.VERSION = WakaTime.getPluginVersion();
        WakaTime.IDE_VERSION = System.getProperty("netbeans.buildnumber");
        WakaTime.log.log(Level.INFO, "Initializing WakaTime plugin v{0} (https://wakatime.com/)", WakaTime.VERSION);

        if (!Dependencies.isCLIInstalled()) {
            WakaTime.info("Downloading and installing wakatime-cli ...");
            Dependencies.installCLI();
            WakaTime.info("Finished downloading and installing wakatime-cli.");
        } else if (Dependencies.isCLIOld()) {
            WakaTime.info("Upgrading wakatime-cli ...");
            Dependencies.upgradeCLI();
            WakaTime.info("Finished upgrading wakatime-cli.");
        } else {
            WakaTime.info("wakatime-cli is up to date.");
        }

        if (!Dependencies.isPythonInstalled()) {
            
            WakaTime.info("Python not found, downloading python...");

            // download and install python
            Dependencies.installPython();

            if (Dependencies.isPythonInstalled()) {
                log.info("Finished installing python...");
            } else {
                WakaTime.error("WakaTime requires Python to be installed.");
                String msg = "WakaTime requires Python to be installed and in your system PATH.\nYou can install Python from https://www.python.org/downloads/\nAfter installing Python, restart your IDE.";
                WakaTime.errorDialog(msg);
            }
            
        }

        WakaTime.DEBUG = WakaTime.isDebugEnabled();
        if (WakaTime.DEBUG) {
            log.setLevel(Level.CONFIG);
            WakaTime.debug("Logging level set to DEBUG");
        }

        WakaTime.debug("Python location: " + Dependencies.getPythonLocation());
        WakaTime.debug("CLI location: " + Dependencies.getCLILocation());

        // prompt for apiKey if it does not already exist
        String apiKey = ApiKey.getApiKey();
        if (apiKey.equals("")) {
            apiKey = ApiKey.promptForApiKey();
            if (apiKey != null && !apiKey.equals(""))
                ApiKey.saveApiKey(apiKey);
        }
        WakaTime.debug("API Key: "+ApiKey.getApiKey());

        // Listen for changes to documents
        PropertyChangeListener l = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                JTextComponent jtc = EditorRegistry.lastFocusedComponent();
                if (jtc != null) {
                    Document d = jtc.getDocument();
                    CustomDocumentListener listener = new CustomDocumentListener(d);
                    d.addDocumentListener(listener);
                    listener.update();
                }
            }
        };

        EditorRegistry.addPropertyChangeListener(l);

        WakaTime.info("Finished initializing WakaTime plugin.");

        // install update checker when UI is ready (main window shown)
        WindowManager.getDefault().invokeWhenUIReady(new Runnable () {
            @Override
            public void run () {
                try {
                    UpdateHandler.checkAndHandleUpdates();
                } catch(NullPointerException e) {
                    WakaTime.error(e.toString());
                }
            }
        });
    }

    public static boolean enoughTimePassed(long currentTime) {
        return WakaTime.lastTime + FREQUENCY * 60 < currentTime;
    }

    public static void info(String msg) {
        log.log(Level.INFO, msg);
    }

    public static void warn(String msg) {
        log.log(Level.WARNING, msg);
    }

    public static void error(String msg) {
        log.log(Level.SEVERE, msg);
    }

    public static void errorDialog(String msg) {
        int msgType = NotifyDescriptor.ERROR_MESSAGE;
        NotifyDescriptor d = new NotifyDescriptor.Message(msg, msgType);
        DialogDisplayer.getDefault().notify(d);
    }

    public static void debug(String msg) {
        log.log(Level.CONFIG, msg);
    }

    public static Boolean isDebugEnabled() {
        Boolean debug = false;
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
                    if (parts.length == 2 && parts[0].trim().equals("debug") && parts[1].trim().toLowerCase().equals("true")) {
                        debug = true;
                    }
                    line = br.readLine();
                }
            } catch (IOException e) {
                WakaTime.error(e.toString());
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    WakaTime.error(e.toString());
                }
            }
        }
        return debug;
    }

    public static String getPluginVersion() {
        for (UpdateUnit updateUnit : UpdateManager.getDefault().getUpdateUnits()) {
            UpdateElement updateElement = updateUnit.getInstalled();
            if (updateElement != null)
                if (WakaTime.CODENAME.equals(updateElement.getCodeName()))
                    return updateElement.getSpecificationVersion();
        }
        return "Unknown";
    }

    public static void logFile(String file, Project currentProject, boolean isWrite) {
        ArrayList<String> cmds = new ArrayList<String>();
        cmds.add(Dependencies.getPythonLocation());
        cmds.add(Dependencies.getCLILocation());
        cmds.add("--key");
        cmds.add(ApiKey.getApiKey());
        cmds.add("--file");
        cmds.add(file);
        if (currentProject != null) {
            cmds.add("--project");
            cmds.add(ProjectUtils.getInformation(currentProject).getDisplayName());
        }
        cmds.add("--plugin");
        cmds.add(IDE_NAME+"/"+IDE_VERSION+" "+IDE_NAME.toLowerCase()+"-wakatime/"+WakaTime.VERSION);
        if (isWrite)
            cmds.add("--write");
        try {
            WakaTime.debug("Executing CLI: " + Arrays.toString(cmds.toArray()));
            Process proc = Runtime.getRuntime().exec(cmds.toArray(new String[cmds.size()]));
            if (WakaTime.DEBUG) {
                BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(proc.getInputStream()));
                BufferedReader stdError = new BufferedReader(new
                        InputStreamReader(proc.getErrorStream()));
                proc.waitFor();
                String s;
                while ((s = stdInput.readLine()) != null) {
                    WakaTime.debug(s);
                }
                while ((s = stdError.readLine()) != null) {
                    WakaTime.debug(s);
                }
                WakaTime.debug("Command finished with return value: "+proc.exitValue());
            }
        } catch (IOException e) {
            WakaTime.error(e.toString());
        } catch (InterruptedException e) {
            WakaTime.error(e.toString());
        }
    }
}
