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
import org.openide.util.NbPreferences;
import org.openide.windows.OnShowing;
import org.openide.windows.WindowManager;
import static org.wakatime.netbeans.plugin.WakaTime.IDE_NAME;
import static org.wakatime.netbeans.plugin.WakaTime.IDE_VERSION;

@OnShowing
public class WakaTime extends ModuleInstall implements Runnable {
    public static final String CODENAME = "org.wakatime.netbeans.plugin";
    public static final String IDE_NAME = "NetBeans";
    public static final String CONFIG = ".wakatime.cfg";
    public static final short FREQUENCY = 2; // minutes between pings
    public static final Logger log = Logger.getLogger("WakaTime");

    public static String VERSION = "Unknown";
    public static String IDE_VERSION = "Unknown";
    public static Boolean DEBUG = false;
    public static CustomDocumentListener documentListener = null;
    
    public static Boolean READY = false;
    public static String lastFile = null;
    public static long lastTime = 0;

    @Override
    public void run() {
        WakaTime.VERSION = WakaTime.getPluginVersion();
        WakaTime.IDE_VERSION = System.getProperty("netbeans.buildnumber");
        WakaTime.log.log(Level.INFO, "Initializing WakaTime plugin v{0} (https://wakatime.com/)", WakaTime.VERSION);

        WakaTime.DEBUG = WakaTime.isDebugEnabled();
        if (WakaTime.DEBUG) {
            log.setLevel(Level.CONFIG);
            WakaTime.debug("Logging level set to DEBUG");
        }

        try {
            if (!Dependencies.isCLIInstalled()) {
                WakaTime.info("Downloading and installing wakatime-cli ...");
                Dependencies.installCLI();
                WakaTime.READY = true;
                WakaTime.info("Finished downloading and installing wakatime-cli.");
            } else if (Dependencies.isCLIOld()) {
                WakaTime.info("Upgrading wakatime-cli ...");
                Dependencies.upgradeCLI();
                WakaTime.READY = true;
                WakaTime.info("Finished upgrading wakatime-cli.");
            } else {
                WakaTime.READY = true;
                WakaTime.info("wakatime-cli is up to date.");
            }
        } catch (Exception e) {
            WakaTime.error(e.toString());
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
                return;
            }
            
        }

        WakaTime.debug("Python location: " + Dependencies.getPythonLocation());
        WakaTime.debug("CLI location: " + Dependencies.getCLILocation());

        // prompt for apiKey if it does not already exist
        String apiKey = getApiKey();
        if (apiKey.equals("")) {
            apiKey = ApiKey.promptForApiKey(apiKey);
            if (apiKey != null && !apiKey.equals("")) {
                ConfigFile.set("settings", "api_key", apiKey);
                NbPreferences.forModule(WakaTime.class).put("API Key", apiKey);
            }
        }
        WakaTime.debug("API Key: " + obfuscateKey(getApiKey()));

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
        String debug = NbPreferences.forModule(WakaTime.class).get("Debug", "");
        if (debug.equals("")) {
            debug = ConfigFile.get("settings", "debug");
            try {
                NbPreferences.forModule(WakaTime.class).put("Debug", debug);
            } catch (Exception e) {
                WakaTime.warn(e.toString());
            }
        }
        return debug.equals("true");
    }

    public static String getApiKey() {
        String apiKey = NbPreferences.forModule(WakaTime.class).get("API Key", "");
        if (apiKey == null || apiKey.equals("")) {
            apiKey = ConfigFile.get("settings", "api_key");
            try {
               NbPreferences.forModule(WakaTime.class).put("API Key", apiKey);
            } catch (Exception e) {
                WakaTime.warn(e.toString());
            }
        }
        if (apiKey == null)
            apiKey = "";
        return apiKey;
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
    
    public static void sendHeartbeat(String file, Project currentProject, boolean isWrite) {
        if (WakaTime.READY)
            sendHeartbeat(file, currentProject, isWrite, 0);
    }

    private static void sendHeartbeat(final String file, final Project currentProject, final boolean isWrite, final int tries) {
        final String[] cmds = buildCliCommand(file, currentProject, isWrite);
        WakaTime.debug("Executing CLI: " + Arrays.toString(obfuscateKey(cmds)));
        Runnable r = new Runnable() {
            public void run() {
                try {
                    Process proc = Runtime.getRuntime().exec(cmds);
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
                } catch (Exception e) {
                    if (tries < 3) {
                        WakaTime.debug(e.toString());
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e1) {
                            WakaTime.error(e1.toString());
                        }
                        sendHeartbeat(file, currentProject, isWrite, tries + 1);
                    } else {
                        WakaTime.error(e.toString());
                    }
                }
            }
        };
        new Thread(r).start();
    }

    public static String[] buildCliCommand(String file, Project currentProject, boolean isWrite) {
        ArrayList<String> cmds = new ArrayList<String>();
        cmds.add(Dependencies.getPythonLocation());
        cmds.add(Dependencies.getCLILocation());
        cmds.add("--key");
        cmds.add(getApiKey());
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
        return cmds.toArray(new String[cmds.size()]);
    }
    
    private static String obfuscateKey(String key) {
        String newKey = null;
        if (key != null) {
            newKey = key;
            if (key.length() > 4)
                newKey = "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXX" + key.substring(key.length() - 4);
        }
        return newKey;
    }

    private static String[] obfuscateKey(String[] cmds) {
        ArrayList<String> newCmds = new ArrayList<String>();
        String lastCmd = "";
        for (String cmd : cmds) {
            if (lastCmd == "--key")
                newCmds.add(obfuscateKey(cmd));
            else
                newCmds.add(cmd);
            lastCmd = cmd;
        }
        return newCmds.toArray(new String[newCmds.size()]);
    }
}