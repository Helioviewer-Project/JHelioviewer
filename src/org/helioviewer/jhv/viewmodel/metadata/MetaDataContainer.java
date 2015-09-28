package org.helioviewer.jhv.viewmodel.metadata;

import org.helioviewer.jhv.Telemetry;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

//FIXME: get rid of this class. should probably move to factory or MetaData class
public class MetaDataContainer
{
	private final Document document;
	
	public MetaDataContainer(Document _document)
	{
		document = _document;
	}
	
	public String get(String key)
	{
        return getValueFromXML(key, "fits");
	}

	private String getValueFromXML(String key, String string)
	{
		NodeList current = document.getElementsByTagName("meta");
		NodeList nodes = ((Element) current.item(0)).getElementsByTagName(string);
		NodeList value = ((Element) nodes.item(0)).getElementsByTagName(key);
		Element line = (Element) value.item(0);
		if (line != null)
		{
			Node child = line.getFirstChild();
			if (child instanceof CharacterData)
				return ((CharacterData) child).getData();
		}
		return null;
	}

	public int tryGetInt(String key)
	{
		String string = get(key);
        if (string != null)
        {
            try
            {
                return Integer.parseInt(string);
            }
            catch (NumberFormatException e)
            {
                Telemetry.trackException(e);
                return 0;
            }
        }
        return 0;
    }

	public double tryGetDouble(String key)
	{
        String string = get(key);
        if (string == null)
        	return 0.0; //TODO: should this be NaN too?

        try
        {
            return Double.parseDouble(string);
        }
        catch (NumberFormatException e)
        {
        	Telemetry.trackException(e);
            return Double.NaN;
        }
    }
}
