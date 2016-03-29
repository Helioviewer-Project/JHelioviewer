package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

/**
 * Enum describing the databin class ID's. Methods exist for getting the
 * KakaduClassID and the StandardClassID. I have also included the string
 * representations of the databins as defined for cache model updates.
 */
public enum JPIPDatabinClass
{
	PRECINCT_DATABIN(0, JPIPConstants.PRECINCT_DATA_BIN_CLASS, "P"),
	TILE_HEADER_DATABIN(1, JPIPConstants.TILE_HEADER_DATA_BIN_CLASS, "H"),
	TILE_DATABIN(2, JPIPConstants.TILE_DATA_BIN_CLASS, "T"),
	MAIN_HEADER_DATABIN(3, JPIPConstants.MAIN_HEADER_DATA_BIN_CLASS, "Hm"),
	META_DATABIN(4, JPIPConstants.META_DATA_BIN_CLASS, "M");

	/** The classID as an integer as per the Kakadu library. */
	private int kakaduClassID;

	/** The classID as an integer as per the JPEG2000 Part-9 standard. */
	private int standardClassID;
	
	/**
	 * The classID as a string as per the JPEG2000 Part-9 standard. Used for
	 * cache model updates.
	 */
	private String jpipString;
	
	public static JPIPDatabinClass fromKduClassID(int _kakaduClassID)
	{
		for(JPIPDatabinClass c:values())
			if(c.kakaduClassID==_kakaduClassID)
				return c;
		
		throw new RuntimeException("Unknown kakadu class ID "+_kakaduClassID);
	}

	private JPIPDatabinClass(int _kakaduClassID, int _standardClassID, String _jpipString)
	{
		kakaduClassID = _kakaduClassID;
		standardClassID = _standardClassID;
		jpipString = _jpipString;
	}

	/** Returns the classID as an integer as per the Kakadu library. */
	public int getKakaduClassID()
	{
		return kakaduClassID;
	}

	/**
	 * Returns the classID as an integer as per the JPEG2000 Part-9 standard.
	 */
	public int getStandardClassID()
	{
		return standardClassID;
	}

	/**
	 * Returns the classID as a string as per the JPEG2000 Part-9 standard. Used
	 * for cache model updates.
	 */
	public String getJpipString()
	{
		return jpipString;
	}
}
