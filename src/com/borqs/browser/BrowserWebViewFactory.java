/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.borqs.browser;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebView;

import org.chromium.content.browser.ContentView;
import org.chromium.ui.WindowAndroid;

import android.util.Log;

/**
 * Web view factory class for creating {@link BrowserWebView}'s.
 */
public class BrowserWebViewFactory implements ContentViewFactory{

    private final Context mContext;

    public BrowserWebViewFactory(Context context) {
        mContext = context;
    }

    protected ContentView instantiateWebView(int nativeWebContents, WindowAndroid windowAndroid, 
    		AttributeSet attrs, int defStyle,  	int personality) {
        return new BrowserWebView(mContext, nativeWebContents, windowAndroid, attrs, defStyle, personality);
    }

    
    public ContentView createSubContentView(boolean privateBrowsing) {
        return createContentView(privateBrowsing);
    }

    
    public ContentView createContentView(boolean privateBrowsing) {
    	Log.i("BrowserWebViewFactory", "createWebView");
        ContentView w = instantiateWebView(0, null, null, android.R.attr.webViewStyle, 0);
        initWebViewSettings(w);
        return w;
    }
	
    protected void initWebViewSettings(ContentView w) {
        w.setScrollbarFadingEnabled(true);
        w.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        //w.setMapTrackballToArrowKeys(false); // use trackball directly
        // Enable the built-in zoom
        w.getContentSettings().setBuiltInZoomControls(true);
        final PackageManager pm = mContext.getPackageManager();
        boolean supportsMultiTouch =
                pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH)
                || pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT);
        w.getContentSettings().setDisplayZoomControls(!supportsMultiTouch);

        // Add this WebView to the settings observer list and update the
        // settings
        final BrowserSettings s = BrowserSettings.getInstance();
        s.startManagingSettings(w.getContentSettings());

        // Remote Web Debugging is always enabled
        WebView.setWebContentsDebuggingEnabled(true);
    }

}
