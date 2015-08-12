package org.helioviewer.jhv.viewmodel.metadata;

import java.awt.Rectangle;
import java.time.LocalDateTime;

import org.helioviewer.jhv.layers.filter.LUT.LUT_ENTRY;

public class MetaDataEIT extends MetaData{

	private final static Rectangle RESOLUTION = new Rectangle(1024, 1024);
	private final double IMAGE_SCALE = 2.63;

	public MetaDataEIT(MetaDataContainer metaDataContainer) {
        super(metaDataContainer, RESOLUTION);
        
        measurement = metaDataContainer.get("WAVELNTH");
        if (measurement == null) {
            measurement = "" + metaDataContainer.tryGetInt("WAVELNTH");
        }
        observatory = metaDataContainer.get("TELESCOP");
        if (!(instrument.equalsIgnoreCase("EIT"))){
        	throw new NonSuitableMetaDataException("invalid instrument: "+instrument);
        }
                
        this.metaDataContainer = metaDataContainer;
        
        fullName = "EIT " + measurement;

        switch (measurement) {
		case "171":
			defaultLUT = LUT_ENTRY.SOHO_EIT_171;
			break;
		case "195":
			defaultLUT = LUT_ENTRY.SOHO_EIT_195;
			break;
		case "284":
			defaultLUT = LUT_ENTRY.SOHO_EIT_284;
			break;
		case "304":
			defaultLUT = LUT_ENTRY.SOHO_EIT_304;
			break;
		default:
			break;
		}
        
        String observedDate = metaDataContainer.get("DATE_OBS");
        localDateTime = LocalDateTime.parse(observedDate, DATE_FORMAT);

        updatePixelParameters();
       
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
   }
}
