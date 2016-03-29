package org.helioviewer.jhv.viewmodel.jp2view.io.http;

public class HTTPResponse extends HTTPMessage
{
    public final int status;
    public final String reason;

    public HTTPResponse(int _status, String _reason)
    {
        status = _status;
        reason = _reason;
    }
}
