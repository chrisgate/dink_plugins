package com.chrisgate.dink_plugins;

public class ModelException extends Exception {

    private static final long serialVersionUID = 469698280001919043L;

    public ModelException() {
    }

    public ModelException(String detailMessage) {
        super(detailMessage);
    }

    public ModelException(Throwable throwable) {
        super(throwable);
    }

    public ModelException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

}
