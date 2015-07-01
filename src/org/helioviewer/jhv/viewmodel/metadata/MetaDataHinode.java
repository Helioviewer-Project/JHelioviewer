package org.helioviewer.jhv.viewmodel.metadata;

import java.awt.Rectangle;
import java.time.LocalDateTime;

public class MetaDataHinode extends MetaData{
	
	private final static Rectangle RESOLUTION = new Rectangle(4096, 4096);
	private final static double IMAGE_SCALE = 0.6;

  public MetaDataHinode(MetaDataContainer metaDataContainer){
        super(metaDataContainer, RESOLUTION);
        
        measurement = metaDataContainer.get("WAVELNTH");
        observatory = metaDataContainer.get("TELESCOP");
        if (!(instrument.equalsIgnoreCase("XRT"))){
        	throw new NonSuitableMetaDataException("invalid instrument: "+instrument);
        }

        this.metaDataContainer = metaDataContainer;

        this.instrument = "XRT";
        fullName = "XRT";
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
