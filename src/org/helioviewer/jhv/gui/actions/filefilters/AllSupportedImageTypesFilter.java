package org.helioviewer.jhv.gui.actions.filefilters;

/**
 * Implementation of ExtensionFileFilter for all supported image types.
 */
public class AllSupportedImageTypesFilter extends ExtensionFileFilter
{
    public AllSupportedImageTypesFilter()
    {
        extensions = new String[] { "jpg", "jpeg", "png", "fts", "fits", "jp2", "jpx" };
    }

    public String getDescription()
    {
        return "All supported files (\".jpg\", \".jpeg\", \".png\", \".fts\", \".fits\", \".jp2\", \".jpx\")";
    }
}