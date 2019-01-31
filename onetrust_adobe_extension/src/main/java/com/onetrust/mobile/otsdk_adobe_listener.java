package com.onetrust.mobile;

import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionListener;

public class otsdk_adobe_listener extends ExtensionListener {

    protected otsdk_adobe_listener(ExtensionApi extension, String type, String source) {
        super(extension, type, source);
    }

    @Override
    protected otsdk_adobe getParentExtension() {
        return (otsdk_adobe)super.getParentExtension();
    }

    @Override
    public void hear(Event event) {
        getParentExtension().handleAdobeEvent(event);
    }

}
