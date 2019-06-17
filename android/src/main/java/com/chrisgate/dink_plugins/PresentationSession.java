package com.chrisgate.dink_plugins;
import java.math.BigInteger;
import java.security.SecureRandom;


import android.app.Activity;

/**
 * This class is the Java representation of a <code>PresentationSession</code> defined in the JavaScript Presentation API.
 *
 */
public class PresentationSession {
    public static String DISCONNECTED = "disconnected";
    public static String CONNECTED = "connected";
    private String id;
    private String url;
    private Activity activity;
    //private CallbackContext callbackContext;
    private String state;
    private SecondScreenPresentation presentation;

    /**
     *
     * @param activity the parent activity associated with this session
     * @param url the URL of the presenting page passed by calling <code>navigator.presentation.requestSession(url)</code>
     */
    public PresentationSession(Activity activity, String url) {
        this.id = new BigInteger(130, new SecureRandom()).toString(32);
        this.url = url;
        this.activity = activity;
//        this.callbackContext = callbackContext;
        this.state = DISCONNECTED;
    }

    /**
     * @return the parent activity
     */
    public Activity getActivity() {
        return activity;
    }

//    /**
//     * @return the {@link CallbackContext}
//     */
//    public CallbackContext getCallbackContext() {
//        return callbackContext;
//    }

    /**
     * @return the URL of the presenting page
     */
    public String getUrl() {
        return url;
    }

    /**
     * @return the session ID. It will be created randomly in the constructor
     */
    public String getId() {
        return id;
    }

    /**
     * @return the session state. currently two states are supported: <code>'connected'</code> and <code>'disconnected'</code>
     */
    public String getState() {
        return state;
    }

    /**
     * @param state set the state.
     *
     *  If state is changed, the <code>session.onstatechange</code> will be fired on both controlling and presenting page. If new state value 'disconnected', session will be destroyed and removed from the sessions list.
     */
    public void setState(String state) {
        String oldState = this.state;
        this.state = state;
        if (!state.equals(oldState)) {
//            CDVPresentationPlugin.sendSessionResult(PresentationSession.this, "onstatechange", getState());
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (getPresentation() != null) {
                        getPresentation().getWebView().loadUrl("javascript:NavigatorPresentationJavascriptInterface.onstatechange('"+getId()+"','"+getState()+"')");
                    }
                }
            });
            if (DISCONNECTED.equals(state) && getPresentation() != null) {
                setPresentation(null);
            }
        }
    }

    /**
     * @return the {@link SecondScreenPresentation} associated with this session to display the presenting page on it.
     */
    public SecondScreenPresentation getPresentation() {
        return presentation;
    }

    /**
     * @param presentation the {@link SecondScreenPresentation} associated with this session. State will change to <code>disconnected</code> if value of presentation is <code>null</code>
     */
    public void setPresentation(SecondScreenPresentation presentation) {
        if (presentation == null) {
            if (this.presentation != null) {
                this.presentation.setSession(null);
            }
            setState(DISCONNECTED);
        }
        else {
            presentation.setSession(this);
        }
        this.presentation = presentation;
    }

    /**
     *
     * @param toPresentation <code>true</code> to post message to presenting page, <code>false</code> to post message to controlling page
     * @param msg the message to post
     */
    public void postMessage(boolean toPresentation, final String msg) {
        if (CONNECTED.equals(getState())) {
            if (toPresentation) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getPresentation().getWebView().loadUrl("javascript:NavigatorPresentationJavascriptInterface.onmessage('"+getId()+"','"+msg+"')");
                    }
                });
            }
            else{
//                CDVPresentationPlugin.sendSessionResult(PresentationSession.this, "onmessage", msg);
            }
        }
    }
}