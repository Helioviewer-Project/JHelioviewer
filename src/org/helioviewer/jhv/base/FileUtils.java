package org.helioviewer.jhv.base;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class which provides functions for accessing and working with files.
 * 
 * @author Benjamin Wamsler
 * @author Andre Dau
 */
public class FileUtils
{
    private static Map<String, String> registeredExecutables = new HashMap<String, String>();

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
