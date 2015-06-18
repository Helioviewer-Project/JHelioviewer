package org.helioviewer.jhv.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class which provides functions for accessing and working with files.
 * 
 * @author Benjamin Wamsler
 * @author Andre Dau
 */
public class FileUtils {
    private static Map<String, String> registeredExecutables = new HashMap<String, String>();

    private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };

    /**
     * Invokes an executable whose path was registered before.
     * 
     * @param identifier
     *            Identifier under which the executable is registered
     * @param arguments
     *            Arguments which should be passed to the executable
     * @throws IOException
     */
    public static Process invokeExecutable(String identifier, List<String> arguments) throws IOException {
        String exec = registeredExecutables.get(identifier);
        if (exec == null) {
            throw new IllegalArgumentException("Executable " + identifier + " not registered!");
        }
        if (arguments != null) {
            arguments.add(0, exec);
            String logExec = "";
            for (String argument : arguments) {
                logExec += " \"" + argument + "\"";
            }
            System.out.println(">> FileUtils.invokeExecutable > Execute command: " + logExec);
            return Runtime.getRuntime().exec(arguments.toArray(new String[arguments.size()]));
        } else {
            System.out.println(">> FileUtils.invokeExecutable > Execute command: " + "\"" + exec + "\"");
            return Runtime.getRuntime().exec(exec);
        }
    }

    /**
     * Logs stdout and stderr of a process. It is necessary to read from the
     * input and the error stream of a process object. Otherwise the process
     * might block when the buffer is full.
     * 
     * @param process
     *            The process object
     * @param processName
     *            The name of the process (for logging pruposes)
     * @param logLevel
     *            The level with which to log the output
     * @param blockUntilFinished
     *            True, if the method should block until the process finished
     *            execution
     * @throws IOException
     */
    public static void logProcessOutput(final Process process, final String processName, boolean blockUntilFinished) throws IOException {
        final BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        final BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        Thread threadStdout = new Thread(new Runnable() {
            public void run() {
                try {
                    String line;
                    while ((line = stdout.readLine()) != null) {
                        System.out.println(processName + ": " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        stdout.close();
                    } catch (IOException e) {
                    }
                }
            }
        });
        Thread threadStderr = new Thread(new Runnable() {
            public void run() {
                try {
                    String line;
                    while ((line = stderr.readLine()) != null) {
                        System.err.println(processName + ": " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        stderr.close();
                    } catch (IOException e) {
                    }
                }
            }
        });

        threadStderr.setDaemon(true);
        threadStdout.setDaemon(true);
        
        threadStderr.start();
        threadStdout.start();
        if (blockUntilFinished) {
            try {
                threadStderr.join();
                threadStdout.join();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Registers the path of an executable. The executable can be later invoked
     * using its identifier
     * 
     * @param identifier
     *            Identifier under which the executable can be accessed
     * @param path
     *            Path to the executable
     */
    public static void registerExecutable(String identifier, String path) {
        boolean registered = false;

        try {
            System.out.println(">> FileUtils.registerExecutable(" + identifier + ", " + path + ") > Trying to use execFile.setExecutable from JDK 1.6+");
            File execFile = new File(path);
            registered = (Boolean) (execFile.getClass().getDeclaredMethod("setExecutable", new Class[] { boolean.class }).invoke(execFile, true));
            if (!registered) {
                System.err.println("FileUtils.registerExecutable(" + identifier + ", " + path + ") > Failed to make file executable. The executable might not work properly!");
            }
        } catch (Throwable t) {
            System.out.println(">> FileUtils.registerExecutable(" + identifier + ", " + path + ") > Failed using setExecutable method. Fall back to Java < 1.6 registerExecutable mode.");
            t.printStackTrace();
            registered = false;
        }

        if (!registered) {
            System.err.println(">> FileUtils.registerExecutable(" + identifier + ", " + path + ") > Error while registering executable '" + identifier + "' in '" + path + "'");
        } else {
            registeredExecutables.put(identifier, path);
        }
    }

    /**
     * Returns an input stream to a resource. This function can be used even if
     * the whole program and resources are within a JAR file.\n The path must
     * begin with a slash and contain all subfolders, e.g.:\n
     * /images/sample_image.png <br>
     * The class loader used is the same which was used to load FileUtils
     * 
     * @param resourcePath
     *            The path to the resource
     * @return An InputStream to the resource
     */
    public static InputStream getResourceInputStream(String resourcePath) {
        return FileUtils.class.getResourceAsStream(resourcePath);
    }

    /**
     * Returns an URL to a resource. This function can be used even if the whole
     * program and resources are within a JAR file.\n The path must begin with a
     * slash and contain all subfolders, e.g.:\n /images/sample_image.png <br>
     * The class loader used is the same which was used to load FileUtils .
     * 
     * @param resourcePath
     *            The path to the resource
     * @return An URL to the resource
     */
    public static URL getResourceUrl(String resourcePath) {
        return FileUtils.class.getResource(resourcePath);
    }
}
