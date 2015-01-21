package org.helioviewer.jhv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JOptionPane;

import org.helioviewer.jhv.base.DownloadStream;
import org.helioviewer.jhv.base.Message;
import org.helioviewer.jhv.gui.dialogs.NewVersionDialog;

/**
 * Class to test in a new thread if there is a newer version of JHelioviewer
 * released and shows a message.
 * 
 * After construction the code is available in run(), ie as a Runnable object.
 * To start in parallel use check().
 * 
 * If verbose is false, ie. when called during startup, the property
 * update.check.next is used to suspend the checks: - If it is negative, the
 * update check is suspended forever - If it is 0, the update check is done - If
 * it is positive, it is decremented and then checked if 0
 * 
 * For further version this gives much room for improvement: - automated
 * download - ... ?
 * 
 * @author Helge Dietert
 */
public class UpdateChecker implements Runnable {
    /**
     * File address to check for updates
     */
    private final URL UPDATE_URL;
    /**
     * Determines whether to show a message box if already the latest version is
     * running and if a message box is shown in case of an error.
     * 
     * Also it determines whether the properties update.check.* are used to
     * suspend the checks.
     */
    private boolean verbose=false;

    private NewVersionDialog d = new NewVersionDialog(verbose);

    /**
     * Constructs a new update object which is not verbose
     * 
     * @throws MalformedURLException
     *             Error while parsing the internal update URL
     */
    public UpdateChecker() throws MalformedURLException {
        UPDATE_URL = new URL("http://jhelioviewer.org/updateJHV.txt");
        verbose = false;
    }

    /**
     * Checks for update in a new thread
     */
    public void check() {
        Thread t = new Thread(this, "JHV Update Checker");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Checks for update and show a dialog box
     */
    public void run() {
        if (!verbose) {
            try {
                int n = Integer.parseInt(Settings.getProperty("update.check.next"));
                if (n > 0) {
                    n -= 1;
                    Settings.setProperty("update.check.next", Integer.toString(n));
                }
                if (n != 0) {
                    System.out.println("Update check suspended for this startup");
                    return;
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid update setting");
                e.printStackTrace();
                Settings.setProperty("update.check.next", Integer.toString(0));
            }
        }
        System.out.println("Start checking for updates");
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new DownloadStream(UPDATE_URL, JHVGlobals.getStdConnectTimeout(), JHVGlobals.getStdReadTimeout()).getInput()));
            String[] versionParts = in.readLine().split("\\.");
            double version = -1;
            switch(versionParts.length)
            {
                case 1:
                    version = Double.parseDouble(versionParts[0]);
                    break;
                case 2:
                default:
                    version = Double.parseDouble(versionParts[0]+"."+versionParts[1]);
                    break;
            }
                
            if (version>JHVGlobals.VERSION) {
                String message = in.readLine();
                System.out.println("Found newer version " + version);
                d.init(version, message);
                d.showDialog();
                if (!verbose) {
                    Settings.setProperty("update.check.next", Integer.toString(d.getNextCheck()));
                }
            } else {
                System.out.println("Running the newest version of JHelioviewer");
                if (verbose)
                    JOptionPane.showMessageDialog(null, "You are running the latest JHelioviewer version (" + JHVGlobals.VERSION + ")");
            }
            in.close();
        } catch (Exception e) {
            System.err.println("Error retrieving update server");
            e.printStackTrace();
            if (verbose)
                Message.warn("Update check error", "While checking for a newer version got " + e.getLocalizedMessage());
        }
    }

    /**
     * Sets if there should pop up a output anyway. Otherwise only in case of an
     * update a message box is shown
     * 
     * @param verbose
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}