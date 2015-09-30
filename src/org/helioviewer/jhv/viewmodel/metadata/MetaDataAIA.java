package org.helioviewer.jhv.viewmodel.metadata;

import java.time.LocalDateTime;

import org.helioviewer.jhv.base.math.Vector2i;
import org.helioviewer.jhv.layers.LUT.Lut;

public class MetaDataAIA extends MetaData
{
	private final static Vector2i RESOLUTION = new Vector2i(4096, 4096);
	
	public MetaDataAIA(MetaDataContainer metaDataContainer)
	{
        super(metaDataContainer, RESOLUTION);
        
        measurement = metaDataContainer.get("WAVELNTH");
        observatory = metaDataContainer.get("TELESCOP");
        if (!(instrument.equalsIgnoreCase("AIA_1") || instrument.equalsIgnoreCase("AIA_2") || instrument.equalsIgnoreCase("AIA_3") || instrument.equalsIgnoreCase("AIA_4"))){
        	throw new UnsuitableMetaDataException("invalid instrument: "+instrument);
        }

        this.instrument = "AIA";
        fullName = "AIA " + measurement;
        
        switch (measurement)
        {
		case "131":
			defaultLUT = Lut.SDO_AIA_131;
			break;
		case "1600":
			defaultLUT = Lut.SDO_AIA_1600;
			break;
		case "1700":
			defaultLUT = Lut.SDO_AIA_1700;
			break;
		case "171":
			defaultLUT = Lut.SDO_AIA_171;
			break;
		case "193":
			defaultLUT = Lut.SDO_AIA_193;
			break;
		case "211":
			defaultLUT = Lut.SDO_AIA_211;
			break;
		case "304":
			defaultLUT = Lut.SDO_AIA_304;
			break;
		case "335":
			defaultLUT = Lut.SDO_AIA_335;
			break;
		case "4500":
			defaultLUT = Lut.SDO_AIA_4500;
			break;
		case "94":
			defaultLUT = Lut.SDO_AIA_94;
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
   }
}
