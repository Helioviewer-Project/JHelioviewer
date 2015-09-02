package org.helioviewer.jhv.viewmodel.jp2view.io.jpip;

import org.helioviewer.jhv.viewmodel.jp2view.io.http.HTTPRequest;

/**
 * A glorified HTTP request object.
 * 
 * @author caplins
 * @author Juan Pablo
 */
public class JPIPRequest extends HTTPRequest {
    /** The query in string form. */
    private String query = null;
    private Priority priority;
    
    private enum Priority{
    	HIGH, LOW;
    }
    
    /**
     * Default constructor.
     * 
     * @param _method
     * @throws ProtocolException
     */
    public JPIPRequest(Method _method) {
        super(_method);
    }

    /** Method does nothing. */
    public void setURI(String _uri) {
    }

    /** Method does nothing... returns null. */
    public String getURI() {
        return null;
    }

    /**
     * Gets the query string.
     * 
     * @return Query String
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the query string.
     * 
     * @param _query
     */
    public void setQuery(Object _query) {
        query = _query.toString();
    }
    
    public void setPriority(Priority priority){
    	this.priority = priority;
    }
    
    public Priority getPriority(){
    	return this.priority;
    }
}
