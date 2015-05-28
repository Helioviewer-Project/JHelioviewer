package org.helioviewer.jhv.viewmodel.view.jp2view.io.jpip;

import java.util.Hashtable;

/**
 * A class that helps build a JPIP query string.
 * 
 * @author caplins
 * @author Juan Pablo
 */
public class JPIPQuery implements Cloneable {
    /** The hashtable holding the jpip-request-fields */
    private Hashtable<String, String> fields = new Hashtable<String, String>();

    /** Default constructor. */
    public JPIPQuery() {
        fields.put("len", Integer.toString(JPIPConstants.MIN_REQUEST_LEN));
    }

    /**
     * This constructor allows to initialize the query by means of a string list
     * of pairs: key1, value1, key2, value2, ...
     * 
     * @param _values
     */
    public JPIPQuery(String... _values) {
        String key = null;
        boolean isKey = true;
        boolean hasLen = false;

        for (String val : _values) {
            if (isKey)
                key = val;
            else {
                fields.put(key, val);
                hasLen = key.equals("len");
            }

            isKey = !isKey;
        }

        if (!hasLen) {
            fields.put("len", Integer.toString(JPIPConstants.MIN_REQUEST_LEN));
        }
    }

    /**
     * Sets the specified field to the specified value.
     * 
     * @param _key
     * @param _value
     */
    public void setField(String _key, String _value) {
        fields.put(_key, _value);
    }

    /** Clones the query. */
    public JPIPQuery clone() {
        JPIPQuery ret = new JPIPQuery();
        ret.fields.putAll(this.fields);
        return ret;
    }

    /** Returns a String representing this query. */
    public String toString() {
        String ret = "";
        for (String field : fields.keySet())
            ret += field + "=" + fields.get(field) + "&";
        if (ret.length() > 0)
            ret = ret.substring(0, ret.length() - 1);
        return ret;
    }
}
