package org.helioviewer.jhv.viewmodel.changeevent;

import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.jp2view.JHVJP2View;

public class ReaderErrorReason implements ChangedReason {
    private JHVJP2View view;
    private Throwable exception;

    public ReaderErrorReason(JHVJP2View view, Throwable exception) {
        this.view = view;
        this.exception = exception;
    }

    public View getView() {
        return view;
    }

    public JHVJP2View getJHVJP2View() {
        return view;
    }

    public Throwable getException() {
        return exception;
    }

}
