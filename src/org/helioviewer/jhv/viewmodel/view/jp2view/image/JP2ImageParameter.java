package org.helioviewer.jhv.viewmodel.view.jp2view.image;

import org.helioviewer.jhv.viewmodel.view.jp2view.image.ResolutionSet.ResolutionLevel;

/**
 * A simple class that encapsulates the important parameters for decompressing
 * and downloading a JPEG2000 image. An immutable class so it is thread safe.
 * 
 * @author caplins
 * @author Benjamin Wamsler
 * 
 */
public class JP2ImageParameter {

    /** Essentially an immutable Rectangle */
    public final SubImage subImage;

    /** An object that contains the zoom/resolution information. */
    public final ResolutionLevel resolution;

    /** The number of quality layers (more is a better image). */
    public final int qualityLayers;

    /** Zero based frame number */
    public int compositionLayer;

    /** This constructor assigns all variables... throw NPE if any args are null */
    public JP2ImageParameter(SubImage _roi, ResolutionLevel _resolution, int _qualityLayers, int _compositionLayer) {
        if (_roi == null || _resolution == null)
            throw new NullPointerException();
        subImage = _roi;
        resolution = _resolution;
        qualityLayers = _qualityLayers;
        compositionLayer = _compositionLayer;
    }

    /** The toString method. */

    public String toString() {
        String ret = "ImageViewParams[";
        ret += " " + subImage.toString();
        ret += " " + resolution.toString();
        ret += " [NumQLayers=" + qualityLayers + "]";
        ret += " [CurrentLayerNum=" + compositionLayer + "]";
        ret += " ]";
        return ret;
    }

    /** The equals method. */

    public boolean equals(Object o) {
        if (!(o instanceof JP2ImageParameter)) {
            return false;
        }
        JP2ImageParameter params = (JP2ImageParameter) o;
        return subImage.equals(params.subImage) && resolution.equals(params.resolution) && qualityLayers == params.qualityLayers && compositionLayer == params.compositionLayer;
    }
}
