/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2010 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.wakatime.netbeans.plugin;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.netbeans.api.autoupdate.InstallSupport;
import org.netbeans.api.autoupdate.InstallSupport.Installer;
import org.netbeans.api.autoupdate.InstallSupport.Validator;
import org.netbeans.api.autoupdate.OperationContainer;
import org.netbeans.api.autoupdate.OperationContainer.OperationInfo;
import org.netbeans.api.autoupdate.OperationException;
import org.netbeans.api.autoupdate.OperationSupport.Restarter;
import org.netbeans.api.autoupdate.UpdateElement;
import org.netbeans.api.autoupdate.UpdateManager;
import org.netbeans.api.autoupdate.UpdateUnit;
import org.netbeans.api.autoupdate.UpdateUnitProvider;
import org.netbeans.api.autoupdate.UpdateUnitProviderFactory;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Cancellable;
import org.openide.util.NbBundle;

/**
 *
 * @author Jiri Rechtacek
 */
public final class UpdateHandler {

    public static boolean timeToCheck() {
        // every startup
        return true;
    }

    public static class UpdateHandlerException extends Exception {
        public UpdateHandlerException(String msg) {
            super(msg);
        }
        public UpdateHandlerException(String msg, Throwable th) {
            super(msg, th);
        }
    }

    public static void checkAndHandleUpdates() {

        WakaTime.info("Checking for updates to WakaTime plugin...");

        // refresh silent update center first
        refreshSilentUpdateProvider();

        Collection<UpdateElement> updates = findUpdates();
        Collection<UpdateElement> available = Collections.emptySet();
        if (installNewModules()) {
            available = findNewModules();
        }
        if (updates.isEmpty() && available.isEmpty()) {
            // none for install
            WakaTime.info("WakaTime plugin is up to date.");
            return;
        }

        WakaTime.info("Found new WakaTime plugin version, updating...");

        // create a container for install
        OperationContainer<InstallSupport> containerForInstall = feedContainer(available, false);
        if (containerForInstall != null) {
            try {
                handleInstall(containerForInstall);
                WakaTime.info("WakaTime plugin installation finished.");
            } catch (UpdateHandlerException ex) {
                WakaTime.error(ex.toString());

                // cancel progress bar
                InstallSupport support = containerForInstall.getSupport();
                try {
                    support.doCancel();
                } catch (OperationException ex1) {
                    WakaTime.error(ex1.toString());
                }

                return;
            }
        }

        // create a container for update
        OperationContainer<InstallSupport> containerForUpdate = feedContainer(updates, true);
        if (containerForUpdate != null) {
            try {
                handleInstall(containerForUpdate);
                WakaTime.info("WakaTime plugin update finished.");
            } catch (UpdateHandlerException ex) {
                WakaTime.error(ex.toString());

                // cancel progress bar
                InstallSupport support = containerForUpdate.getSupport();
                try {
                    support.doCancel();
                } catch (OperationException ex1) {
                    WakaTime.error(ex1.toString());
                }

                return;
            }
        }

    }

    public static boolean isLicenseApproved(String license) {
        // place your code there
        return true;
    }

    // package private methods
    static Collection<UpdateElement> findUpdates() {
        // check updates
        Collection<UpdateElement> elements4update = new HashSet<UpdateElement>();
        List<UpdateUnit> updateUnits = UpdateManager.getDefault().getUpdateUnits();
        for (UpdateUnit unit : updateUnits) {
            if (unit.getInstalled() != null) { // means the plugin already installed
                if (unit.getCodeName().equals(WakaTime.CODENAME)) { // this is our current plugin
                    if (!unit.getAvailableUpdates().isEmpty()) { // has updates
                        elements4update.add(unit.getAvailableUpdates().get(0)); // add plugin with highest version
                    }
                }
            }
        }
        return elements4update;
    }

    static void handleInstall(OperationContainer<InstallSupport> container) throws UpdateHandlerException {
        // check licenses
        if (!allLicensesApproved(container)) {
            // have a problem => cannot continue
            throw new UpdateHandlerException("Cannot continue because license approval is missing.");
        }
        
        InstallSupport support = container.getSupport();

        // download
        Validator v = null;
        try {
            v = doDownload(support);
        } catch (OperationException ex) {
            throw new UpdateHandlerException("A problem caught while downloading, cause: ", ex);
        } catch (NullPointerException ex) {
            throw new UpdateHandlerException("A problem caught while downloading, cause: ", ex);
        }
        if (v == null) {
            // have a problem => cannot continue
            throw new UpdateHandlerException("Missing Update Validator => cannot continue.");
        }

        // verify
        Installer i = null;
        try {
            i = doVerify(support, v);
        } catch (OperationException ex) {
            // caught a exception
            throw new UpdateHandlerException("A problem caught while verification of updates, cause: ", ex);
        }
        if (i == null) {
            // have a problem => cannot continue
            throw new UpdateHandlerException("Missing Update Installer => cannot continue.");
        }

        // install
        Restarter r = null;
        try {
            r = doInstall(support, i);
        } catch (OperationException ex) {
            // caught a exception
            throw new UpdateHandlerException("A problem caught while installation of updates, cause: ", ex);
        }

        // restart later
        support.doRestartLater(r);
    }

    static Collection<UpdateElement> findNewModules() {
        // check updates
        Collection<UpdateElement> elements4install = new HashSet<UpdateElement>();
        List<UpdateUnit> updateUnits = UpdateManager.getDefault().getUpdateUnits();
        for (UpdateUnit unit : updateUnits) {
            if (unit.getInstalled() == null) { // means the plugin is not installed yet
                if (unit.getCodeName().equals(WakaTime.CODENAME)) { // this is our current plugin
                    if (!unit.getAvailableUpdates().isEmpty()) { // is available
                        elements4install.add(unit.getAvailableUpdates().get(0)); // add plugin with highest version
                    }
                }
            }
        }
        return elements4install;
    }

    static void refreshSilentUpdateProvider() {
        UpdateUnitProvider silentUpdateProvider = getSilentUpdateProvider();
        if (silentUpdateProvider == null) {
            // have a problem => cannot continue
            WakaTime.info("Missing Silent Update Provider => cannot continue.");
            return ;
        }
        try {
            final String displayName = "Checking for updates to WakaTime plugin...";
            silentUpdateProvider.refresh(
                ProgressHandleFactory.createHandle(
                    displayName,
                    new Cancellable () {
                        @Override
                        public boolean cancel () {
                            return true;
                        }
                    }
                ),
                true
            );
        } catch (IOException ex) {
            // caught a exception
            WakaTime.error("A problem caught while refreshing Update Centers, cause: " + ex.toString());
        }
    }

    static UpdateUnitProvider getSilentUpdateProvider() {
        List<UpdateUnitProvider> providers = UpdateUnitProviderFactory.getDefault().getUpdateUnitProviders(true);
        String oldCodename = "org_wakatime_netbeans_plugin_update_center";
        for (UpdateUnitProvider p : providers) {
            if (p.getName().equals(oldCodename) || p.getName().equals(WakaTime.CODENAME)) { // this is our current plugin
                try {
                    final String displayName = "Checking for updates to WakaTime plugin...";
                    p.refresh(
                        ProgressHandleFactory.createHandle(
                            displayName,
                            new Cancellable () {
                                @Override
                                public boolean cancel () {
                                    return true;
                                }
                            }
                        ),
                        true
                    );
                } catch (IOException ex) {
                    // caught a exception
                    WakaTime.error("A problem caught while refreshing Update Centers, cause: " + ex.toString());
                }
                return p;
            }
        }
        return null;
    }

    static OperationContainer<InstallSupport> feedContainer(Collection<UpdateElement> updates, boolean update) {
        if (updates == null || updates.isEmpty()) {
            return null;
        }
        // create a container for update
        OperationContainer<InstallSupport> container;
        if (update) {
            container = OperationContainer.createForUpdate();
        } else {
            container = OperationContainer.createForInstall();
        }

        // loop all updates and add to container for update
        for (UpdateElement ue : updates) {
            if (container.canBeAdded(ue.getUpdateUnit(), ue) && ue.getCodeName().equals(WakaTime.CODENAME)) {
                WakaTime.info("Update to WakaTime plugin found: " + ue);
                OperationInfo<InstallSupport> operationInfo = container.add(ue);
                if (operationInfo == null) {
                    continue;
                }
                container.add(operationInfo.getRequiredElements());
                if (!operationInfo.getBrokenDependencies().isEmpty()) {
                    // have a problem => cannot continue
                    WakaTime.info("There are broken dependencies => cannot continue, broken deps: " + operationInfo.getBrokenDependencies());
                    return null;
                }
            }
        }
        return container;
    }

    static boolean allLicensesApproved(OperationContainer<InstallSupport> container) {
        if (!container.listInvalid().isEmpty()) {
            return false;
        }
        for (OperationInfo<InstallSupport> info : container.listAll()) {
            String license = info.getUpdateElement().getLicence();
            if (!isLicenseApproved(license)) {
                return false;
            }
        }
        return true;
    }

    static Validator doDownload(InstallSupport support) throws OperationException {
        final String displayName = "Downloading new version of WakaTime plugin...";
        ProgressHandle downloadHandle = ProgressHandleFactory.createHandle(
            displayName,
            new Cancellable () {
                @Override
                public boolean cancel () {
                    return true;
                }
            }
        );
        return support.doDownload(downloadHandle, true);
    }

    static Installer doVerify(InstallSupport support, Validator validator) throws OperationException {
        final String displayName = "Validating WakaTime plugin...";
        ProgressHandle validateHandle = ProgressHandleFactory.createHandle(
            displayName,
            new Cancellable () {
                @Override
                public boolean cancel () {
                    return true;
                }
            }
        );
        Installer installer = support.doValidate(validator, validateHandle);
        return installer;
    }

    static Restarter doInstall(InstallSupport support, Installer installer) throws OperationException {
        final String displayName = "Installing WakaTime plugin...";
        ProgressHandle installHandle = ProgressHandleFactory.createHandle(
            displayName,
            new Cancellable () {
                @Override
                public boolean cancel () {
                    return true;
                }
            }
        );
        return support.doInstall(installer, installHandle);
    }

    private static boolean installNewModules() {
        String s = NbBundle.getBundle("org.wakatime.netbeans.plugin.Bundle").getString("UpdateHandler.NewModules");
        return Boolean.parseBoolean(s);
    }
}
