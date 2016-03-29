package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

import java.util.Hashtable;

/**
 * A class that helps build a JPIP query string.
 */
public class JPIPQuery
{
	/** The hashtable holding the jpip-request-fields */
	private Hashtable<String, String> fields = new Hashtable<>();

	/** Default constructor. */
	public JPIPQuery()
	{
	}

	public void setField(String _key, String _value)
	{
		fields.put(_key, _value);
	}
	
	public void removeField(String _key)
	{
		fields.remove(_key);
	}

	public String toString()
	{
		StringBuilder ret = new StringBuilder();
		for (String field : fields.keySet())
		{
			if (ret.length() != 0)
				ret.append('&');
			ret.append(field).append("=").append(fields.get(field));
		}

		return ret.toString();
	}
}
