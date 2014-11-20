/* ==========================================================
File:        SaveListener.java
Description: Sends a heartbeat when a file is saved.
Maintainer:  WakaTime <support@wakatime.com>
License:     BSD, see LICENSE for more details.
Website:     https://wakatime.com/
===========================================================*/

package org.wakatime.netbeans.plugin;

import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.spi.editor.document.OnSaveTask;
import org.openide.filesystems.FileObject;

public class SaveListener implements OnSaveTask {
    private final Document document;

    private SaveListener(Document document) {
        this.document = document;
    }
    
    @Override
    public void performTask() {
        final FileObject file = this.getFile();
        final Project currentProject = this.getProject();
        final long currentTime = System.currentTimeMillis() / 1000;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (file != null) {
                    final String currentFile = file.getPath();
                    WakaTime.logFile(currentFile, currentProject, true);
                    WakaTime.lastFile = currentFile;
                    WakaTime.lastTime = currentTime;
                }
            }
        });
    }

    @Override
    public void runLocked(Runnable r) {
        r.run();
    }

    @Override
    public boolean cancel() {
        return true;
    }
    
    private FileObject getFile() {
        if (this.document == null)
            return null;
        Source source = Source.create(this.document);
        if (source == null)
            return null;
        FileObject fileObject = source.getFileObject();
        if (fileObject == null)
            return null;
        return fileObject;
    }
    
    private Project getProject() {
        if (this.document == null)
            return null;
        Source source = Source.create(this.document);
        if (source == null)
            return null;
        FileObject fileObject = source.getFileObject();
        if (fileObject == null)
            return null;
        return FileOwnerQuery.getOwner(fileObject);
    }
    
    @MimeRegistration(mimeType = "", service = OnSaveTask.Factory.class, position = 1500)
    public static final class FactoryImpl implements Factory {

        @Override
        public OnSaveTask createTask(Context context) {
            return new SaveListener(context.getDocument());
        }
    }
    
}
