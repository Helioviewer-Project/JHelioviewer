package org.helioviewer.viewmodel.metadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.helioviewer.base.logging.Log;
import org.helioviewer.base.math.Vector2dDouble;
import org.helioviewer.base.physics.Constants;
import org.helioviewer.viewmodel.view.fitsview.FITSImage;
import org.helioviewer.viewmodel.view.jp2view.JP2Image;
import org.helioviewer.viewmodel.view.jp2view.kakadu.JHV_KduException;

/**
 * Implementation of MetaData, extends HelioviewerMetaData.
 * 
 * <p>
 * This special implementation also provides informations about the occulting
 * disc. The {@link MetaDataConstructor} should only produce this
 * implementation, if there is there is actual information about the occulting
 * disc present, so it is possible to test this via instanceof.
 * 
 * @author Markus Langenberg
 * 
 */
public class HelioviewerOcculterMetaData extends HelioviewerMetaData implements OcculterMetaData {

    private double innerRadius;
    private double outerRadius;
    private double flatDistance;
    private double maskRotation;
    private Vector2dDouble occulterCenter;

    /**
     * Default constructor.
     * 
     * Tries to read all informations required.
     * 
     * @param m
     *            Meta data container serving as a base for the construction
     */
    public HelioviewerOcculterMetaData(MetaDataContainer m) {
        super(m);

        innerRadius = m.tryGetDouble("HV_ROCC_INNER") * Constants.SunRadius;
        outerRadius = m.tryGetDouble("HV_ROCC_OUTER") * Constants.SunRadius;

        if (innerRadius == 0.0 && getDetector() != null) {
            if (getDetector().equalsIgnoreCase("C2")) {
                innerRadius = 2.3 * Constants.SunRadius;
                outerRadius = 8.0 * Constants.SunRadius;
            } else if (getDetector().equalsIgnoreCase("C3")) {
                innerRadius = 4.4 * Constants.SunRadius;
                outerRadius = 31.5 * Constants.SunRadius;
            } else if (getObservatory().equalsIgnoreCase("STEREO_A") && getDetector().equalsIgnoreCase("COR1")) {
                innerRadius = 1.36 * Constants.SunRadius;
                outerRadius = 4.5 * Constants.SunRadius;
            } else if (getObservatory().equalsIgnoreCase("STEREO_A") && getDetector().equalsIgnoreCase("COR2")) {
                innerRadius = 2.4 * Constants.SunRadius;
                outerRadius = 15.6 * Constants.SunRadius;
            } else if (getObservatory().equalsIgnoreCase("STEREO_B") && getDetector().equalsIgnoreCase("COR1")) {
                innerRadius = 1.5 * Constants.SunRadius;
                outerRadius = 4.9 * Constants.SunRadius;
            } else if (getObservatory().equalsIgnoreCase("STEREO_B") && getDetector().equalsIgnoreCase("COR2")) {
                innerRadius = 3.25 * Constants.SunRadius;
                outerRadius = 17 * Constants.SunRadius;
            }
        }

        if (getDetector().equalsIgnoreCase("C2")) {
            flatDistance = 6.2 * Constants.SunRadius;
        } else if (getDetector().equalsIgnoreCase("C3")) {
            flatDistance = 38 * Constants.SunRadius;
        } else if (getObservatory().equalsIgnoreCase("STEREO_A") && getDetector().equalsIgnoreCase("COR1")) {
            flatDistance = 4.5 * Constants.SunRadius;
        } else if (getObservatory().equalsIgnoreCase("STEREO_A") && getDetector().equalsIgnoreCase("COR2")) {
            flatDistance = 15.75 * Constants.SunRadius;
        } else if (getObservatory().equalsIgnoreCase("STEREO_B") && getDetector().equalsIgnoreCase("COR1")) {
            flatDistance = 4.95 * Constants.SunRadius;
        } else if (getObservatory().equalsIgnoreCase("STEREO_B") && getDetector().equalsIgnoreCase("COR2")) {
            flatDistance = 18 * Constants.SunRadius;
        }

        maskRotation = Math.toRadians(m.tryGetDouble("CROTA"));
        
        double centerX = 0, centerY = 0;
        if (getDetector() != null && ( getDetector().equalsIgnoreCase("COR1") || getDetector().equalsIgnoreCase("COR2")) && m instanceof JP2Image) {
            JP2Image jp2image = (JP2Image) m;
            try {
                String crval1Original = jp2image.getValueFromXML("HV_CRVAL1_ORIGINAL", "helioviewer");
                String crval2Original = jp2image.getValueFromXML("HV_CRVAL2_ORIGINAL", "helioviewer");
                if (crval1Original != null && crval2Original != null) {
                    centerX = Double.parseDouble(crval1Original);
                    centerY = Double.parseDouble(crval2Original);
                } else {
                    String crvalComment = jp2image.getValueFromXML("HV_SECCHI_COMMENT_CRVAL", "helioviewer");
                    if(crvalComment == null) {
                        crvalComment = jp2image.getValueFromXML("HV_COMMENT", "helioviewer");
                    }
                    if(crvalComment != null) {
                        Pattern pattern = Pattern.compile(".*CRVAL1=([+-]?\\d+(.\\d+)?).*");
                        Matcher matcher = pattern.matcher(crvalComment);
                        if(matcher.matches()) {
                            centerX = Double.parseDouble(matcher.group(1));
                        }
                        pattern = Pattern.compile(".*CRVAL2=([+-]?\\d+(.\\d+)?).*");
                        matcher = pattern.matcher(crvalComment);
                        if(matcher.matches()) {
                            centerY = Double.parseDouble(matcher.group(1));
                        }
                    }
                }
            } catch (JHV_KduException e) {
                Log.error(">> HelioviewerOcculterMetaData > Error reading helioviewer meta data key HV_SECCHI_COMMENT_CRVAL", e);
            }
        } else if (m instanceof FITSImage && centerX == 0 && centerY == 0) {
            centerX = m.tryGetDouble("CRVAL1");
            centerY = m.tryGetDouble("CRVAL2");
        }
        
        //Convert arcsec to meters
        double cdelt1 = m.tryGetDouble("CDELT1");
        double cdelt2 = m.tryGetDouble("CDELT2");
        if( cdelt1 != 0 && cdelt2 != 0) {
            centerX = centerX / cdelt1;
            centerY = centerY / cdelt2;
        }
        // HACK - manual adjustment for occulter center
        if (getObservatory().equalsIgnoreCase("STEREO_A") && getDetector().equalsIgnoreCase("COR1")) {
            centerX += 1;
            centerY += 1;
        } else if (getObservatory().equalsIgnoreCase("STEREO_A") && getDetector().equalsIgnoreCase("COR2")) {
            centerX += 3;
            centerY += 6;
        } else if (getObservatory().equalsIgnoreCase("STEREO_B") && getDetector().equalsIgnoreCase("COR1")) {
            centerX += 1;
            centerY += 3;
        } else if (getObservatory().equalsIgnoreCase("STEREO_B") && getDetector().equalsIgnoreCase("COR2")) {
            centerX += 22;
            centerY -= 37;
        }
        occulterCenter = new Vector2dDouble(centerX * getUnitsPerPixel(), centerY * getUnitsPerPixel());
    }

    /**
     * {@inheritDoc}
     */
    public double getInnerPhysicalOcculterRadius() {
        return innerRadius;
    }

    /**
     * {@inheritDoc}
     */
    public double getOuterPhysicalOcculterRadius() {
        return outerRadius;
    }

    /**
     * {@inheritDoc}
     */
    public double getPhysicalFlatOcculterSize() {
        return flatDistance;
    }

    /**
     * {@inheritDoc}
     */
    public double getMaskRotation() {
        return maskRotation;
    }

    /**
     * {@inheritDoc}
     * 
     * In this case, also the mask rotation is checked.
     */
    public boolean checkForModifications() {
        boolean changed = super.checkForModifications();

        double currentMaskRotation = Math.toRadians(metaDataContainer.tryGetDouble("CROTA"));
        if (changed || Math.abs(maskRotation - currentMaskRotation) > Math.toRadians(1)) {
            maskRotation = currentMaskRotation;
            changed = true;
        }

        return changed;
    }

    /**
     * {@inheritDoc}
     */
    public Vector2dDouble getOcculterCenter() {
        return occulterCenter;
    }
}
