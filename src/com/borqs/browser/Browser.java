package com.borqs.browser;

import java.util.concurrent.atomic.AtomicBoolean;

import org.chromium.base.PathUtils;
import org.chromium.content.browser.ResourceExtractor;

import android.app.Application;
import android.util.Log;

public class Browser extends Application { 

    private static final String[] MANDATORY_PAK_FILES = new String[] {
        "chrome.pak",
        "en-US.pak",
        "resources.pak",
        "chrome_100_percent.pak",
        /*"devtools_resources.pak"*/};

    private static final String PRIVATE_DATA_DIRECTORY_SUFFIX = "borqs_paks";

    private final static String LOGTAG = "browser";
    
    // Set to true to enable verbose logging.
    final static boolean LOGV_ENABLED = false;

    // Set to true to enable extra debug logging.
    final static boolean LOGD_ENABLED = true;

    private final AtomicBoolean mInForeground = new AtomicBoolean();

    @Override
    public void onCreate() {
        super.onCreate();

        if (LOGV_ENABLED)
            Log.v(LOGTAG, "Browser.onCreate: this=" + this);

        initializeApplicationParameters();

        // create CookieSyncManager with current Context
        //CookieSyncManager.createInstance(this);
        BrowserSettings.initialize(getApplicationContext());
        //Preloader.initialize(getApplicationContext());
    }

    public boolean isInForeground() {
        return mInForeground.get();
    }

    public void setInForeground(boolean fg) {
        mInForeground.set(fg);
    }

    private void initializeApplicationParameters() {
        ResourceExtractor.setMandatoryPaksToExtract(MANDATORY_PAK_FILES);
        PathUtils.setPrivateDataDirectorySuffix(PRIVATE_DATA_DIRECTORY_SUFFIX);
    }
}
