package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.layers.LUT.Lut;

class MetaDataEIT extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(1024, 1024);
	public MetaDataEIT(MetaDataContainer metaDataContainer)
	{
        super(metaDataContainer, RESOLUTION, metaDataContainer.get("TELESCOP"), metaDataContainer.get("WAVELNTH"));
        
        if (!(instrument.equalsIgnoreCase("EIT")))
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);
                
        fullName = "EIT " + measurement;

        switch (measurement)
        {
			case "171":
				defaultLUT = Lut.SOHO_EIT_171;
				break;
			case "195":
				defaultLUT = Lut.SOHO_EIT_195;
				break;
			case "284":
				defaultLUT = Lut.SOHO_EIT_284;
				break;
			case "304":
				defaultLUT = Lut.SOHO_EIT_304;
				break;
			default:
				break;
		}
        
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
   }
}
