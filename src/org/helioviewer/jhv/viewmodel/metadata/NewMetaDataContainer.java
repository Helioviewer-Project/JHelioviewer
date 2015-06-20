package org.helioviewer.jhv.viewmodel.metadata;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class NewMetaDataContainer{
	private Document document;
	
	public NewMetaDataContainer(Document document) {
		this.document = document;
	}
	
	public String get(String key) {
        String value = getValueFromXML(key, "fits");
        return value;
	}

	private String getValueFromXML(String key, String string) {
		NodeList current = document.getElementsByTagName("meta");
		NodeList nodes = ((Element) current.item(0)).getElementsByTagName(string);
		NodeList value = ((Element) nodes.item(0)).getElementsByTagName(key);
		Element line = (Element) value.item(0);
		if (line != null){
		Node child = line.getFirstChild();
		if (child instanceof CharacterData){
			CharacterData cd = (CharacterData) child;
			return cd.getData();
		}
		}
		return null;
	}

	public int tryGetInt(String key) {
		String string = get(key);
        if (string != null) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException while trying to parse value \"" + string + "\" of key " + key + " from meta data of");
                return 0;
            }
        }
        return 0;
    }

	public double tryGetDouble(String key) {

        String string = get(key);
        if (string != null) {
            try {
                return Double.parseDouble(string);
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException while trying to parse value \"" + string + "\" of key " + key + " from meta data of");
                return Double.NaN;
            }
        }
        return 0.0;
    }
	

	public int getPixelWidth() {
		// TODO Auto-generated method stub
		return 4096;
	}

	public int getPixelHeight() {
		// TODO Auto-generated method stub
		return 4096;
	}

}
