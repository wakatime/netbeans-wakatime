/* ==========================================================
File:        DocumentListener.java
Description: Sends a heartbeat when a file is changed.
Maintainer:  WakaTime <support@wakatime.com>
License:     BSD, see LICENSE for more details.
Website:     https://wakatime.com/
===========================================================*/

package org.wakatime.netbeans.plugin;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.api.Source;
import org.openide.filesystems.FileObject;

/**
 *
 * @author alanhamlett
 */
public class CustomDocumentListener implements DocumentListener {
    private final Document document;
    
    public CustomDocumentListener(Document d) {
        this.document = d;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        this.handleTyping();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        this.handleTyping();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        this.handleTyping();
    }
    
    public void update() {
        if (WakaTime.documentListener != null)
            WakaTime.documentListener.remove();
        WakaTime.documentListener = this;
    }
    
    public void remove() {
        this.document.removeDocumentListener(this);
    }
    
    public void handleTyping() {
        final FileObject file = this.getFile();
        final Project currentProject = this.getProject();
        final long currentTime = System.currentTimeMillis() / 1000;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (file != null) {
                    final String currentFile = file.getPath();
                    if ((!currentFile.equals(WakaTime.lastFile) || WakaTime.enoughTimePassed(currentTime))) {
                        WakaTime.logFile(currentFile, currentProject, false);
                        WakaTime.lastFile = currentFile;
                        WakaTime.lastTime = currentTime;
                    }
                }
            }
        });
    }
    private FileObject getFile() {
        Source source = Source.create(this.document);
        FileObject fileObject = source.getFileObject();
        return fileObject;
    }
    
    private Project getProject() {
        Source source = Source.create(document);
        FileObject fileObject = source.getFileObject();
        return FileOwnerQuery.getOwner(fileObject);
    }
    
}
