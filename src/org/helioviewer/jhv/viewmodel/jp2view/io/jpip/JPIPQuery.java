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
		fields.put("len", Integer.toString(JPIPConstants.MIN_REQUEST_LEN));
	}

	/**
	 * This constructor allows to initialize the query by means of a string list
	 * of pairs: key1, value1, key2, value2, ...
	 * 
	 * @param _values
	 */
	public JPIPQuery(String... _values)
	{
		for(int i=0;i<_values.length;i+=2)
			fields.put(_values[i], _values[i+1]);
		
		if (!fields.contains("len"))
			fields.put("len", Integer.toString(JPIPConstants.MIN_REQUEST_LEN));
	}

	public void setField(String _key, String _value)
	{
		fields.put(_key, _value);
	}

	public String toString()
	{
		StringBuilder ret = new StringBuilder();
		for (String field : fields.keySet())
		{
			if (ret.length() != 0)
				ret.append('&');
			ret.append(field + "=" + fields.get(field));
		}

		return ret.toString();
	}
}
