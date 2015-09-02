package org.helioviewer.jhv.base.downloadmanager;

import java.util.Hashtable;

import org.helioviewer.jhv.viewmodel.jp2view.io.jpip.JPIPConstants;

public class RequestQuery {
    private Hashtable<String, String> fields = new Hashtable<String, String>();

    public RequestQuery() {
        fields.put("len", Integer.toString(JPIPConstants.MIN_REQUEST_LEN));
    }
    
    public void setField(String _key, String _value) {
        fields.put(_key, _value);
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
