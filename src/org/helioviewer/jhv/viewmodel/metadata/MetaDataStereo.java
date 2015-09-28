package org.helioviewer.jhv.viewmodel.metadata;

import java.awt.Rectangle;
import java.time.LocalDateTime;

import org.helioviewer.jhv.base.coordinates.HeliocentricCartesianCoordinate;
import org.helioviewer.jhv.base.coordinates.HeliographicCoordinate;
import org.helioviewer.jhv.base.math.Vector3d;
import org.helioviewer.jhv.layers.LUT.Lut;

class MetaDataStereo extends MetaData
{
	private final static Rectangle RESOLUTION = new Rectangle(2048, 2048);
	public MetaDataStereo(MetaDataContainer metaDataContainer)
	{
        super(metaDataContainer, RESOLUTION);

        observatory = metaDataContainer.get("OBSRVTRY");
        measurement = metaDataContainer.get("WAVELNTH");
        if (measurement == null) {
            measurement = "" + metaDataContainer.tryGetDouble("WAVELNTH");
        }
        if (!((observatory.equalsIgnoreCase("STEREO_A") || observatory.equalsIgnoreCase("STEREO_B") && detector.equalsIgnoreCase("EUVI")))){
        	throw new UnsuitableMetaDataException("invalid instrument");
        }

        fullName = detector + " " + measurement;
        
        switch (measurement) {
		case "171":
			defaultLUT = Lut.STEREO_EUVI_171;
			break;
		case "195":
			defaultLUT = Lut.STEREO_EUVI_195;
			break;
		case "284":
			defaultLUT = Lut.STEREO_EUVI_284;
			break;
		case "304":
			defaultLUT = Lut.STEREO_EUVI_304;
			break;
		default:
			break;
		}
        
        String observedDate = metaDataContainer.get("DATE_OBS");
        localDateTime = LocalDateTime.parse(observedDate, DATE_FORMAT);

        readPixelParameters(metaDataContainer);
        
        this.heeqX = metaDataContainer.tryGetDouble("HEQX_OBS");
        this.heeqY = metaDataContainer.tryGetDouble("HEQY_OBS");
        this.heeqZ = metaDataContainer.tryGetDouble("HEQZ_OBS");
        this.heeqAvailable = this.heeqX != 0.0 || this.heeqY != 0.0 || this.heeqZ != 0.0;

        this.heeX = metaDataContainer.tryGetDouble("HEEX_OBS");
        this.heeY = metaDataContainer.tryGetDouble("HEEY_OBS");
        this.heeZ = metaDataContainer.tryGetDouble("HEEZ_OBS");
        this.heeAvailable = this.heeX != 0.0 || this.heeY != 0.0 || this.heeZ != 0.0;

        this.crlt = metaDataContainer.tryGetDouble("CRLT_OBS");
        this.crln = metaDataContainer.tryGetDouble("CRLN_OBS");
        this.dobs = metaDataContainer.tryGetDouble("DSUN_OBS");
        this.carringtonAvailable = this.crlt != 0.0 || this.crln != 0.0;

        this.stonyhurstLatitude = metaDataContainer.tryGetDouble("HGLT_OBS");
        this.stonyhurstLongitude = metaDataContainer.tryGetDouble("HGLN_OBS");
        this.stonyhurstAvailable = this.stonyhurstLatitude != 0.0 || this.stonyhurstLongitude != 0.0;
        if (stonyhurstAvailable)
        {
        	HeliocentricCartesianCoordinate hcc = new HeliographicCoordinate(Math.toRadians(stonyhurstLongitude), Math.toRadians(stonyhurstLatitude)).toHeliocentricCartesianCoordinate();
        	this.orientation = new Vector3d(hcc.x, hcc.y, hcc.z);
        }
        this.calcDefaultRotation();
   }
}
