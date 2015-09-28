package org.helioviewer.jhv.gui.actions.filefilters;

/**
 * Implementation of ExtensionFileFilter for JHV state files.
 */
public class JHVStateFilter extends ExtensionFileFilter
{
    public JHVStateFilter()
    {
        extensions = new String[] { "jhv" };
    }

    public String getDescription()
    {
        return "JHV State files (\".jhv\")";
    }
}