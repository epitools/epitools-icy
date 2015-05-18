package plugins.davhelle.cellgraph.io;

import icy.main.Icy;
import icy.system.thread.ThreadUtil;

import java.io.File;

import javax.swing.JFileChooser;

/**
 * Simple folder choice dialog to let user locate an export folder of choice.
 * 
 * based on icy.gui.dialog.SaveDialog by Stephane Dallongeville
 * distributed under GNU General Public License
 * 
 * @author Davide Heller
 */
public class SaveFolderDialog
{
    private static class SaveFolderRunner implements Runnable
    {
        private final String title;

        private JFileChooser dialog;
        String result;

        public SaveFolderRunner(String title)
        {
            super();
            this.title = title;
        }

        @Override
        public void run()
        {
            result = null;

            if (dialog == null)
                dialog = new JFileChooser();

            dialog.setDialogTitle(title);
            dialog.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            final int returnVal = dialog.showSaveDialog(null);

            if (returnVal != JFileChooser.APPROVE_OPTION)
                return;

            final File f = dialog.getSelectedFile();
            
            result = f.getAbsolutePath();
        }
    }

    /**
     * Displays a folder save dialog, using the specified default directory and file name and
     * extension
     */
    public static String chooseFolder(String objectName)
    {
        final SaveFolderRunner runner = new SaveFolderRunner("Choose folder to store " + objectName);

        // no result in headless
        if (Icy.getMainInterface().isHeadLess())
            return null;

        ThreadUtil.invokeNow(runner);

        return runner.result;
    }

}

