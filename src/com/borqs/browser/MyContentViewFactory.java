package com.borqs.browser;

import org.chromium.chrome.browser.ContentViewUtil;
import org.chromium.content.browser.ContentView;
import org.chromium.ui.WindowAndroid;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.View;


/**
 * Web view factory class for creating {@link BrowserContentView}'s.
 */
public class MyContentViewFactory implements ContentViewFactory {

    private final Context mContext;

    public MyContentViewFactory(Context context) {
        mContext = context;
    }

    protected ContentView instantiateWebView(AttributeSet attrs, int defStyle,
            boolean privateBrowsing) {
//        return new BrowserContentView(mContext, attrs, defStyle, privateBrowsing);
        WindowAndroid windowAndroid = new WindowAndroid((Activity)mContext);
// ww        windowAndroid.restoreInstanceState(savedInstanceState);
        int nativeWebContents = ContentViewUtil.createNativeWebContents(false);
        //return new BrowserContentView(mContext, nativeWebContents, windowAndroid, attrs, defStyle, 0);
        return ContentView.newInstance(mContext, nativeWebContents, 
                windowAndroid, ContentView.PERSONALITY_CHROME);
    }

    @Override
    public ContentView createSubContentView(boolean privateBrowsing) {
        return createContentView(privateBrowsing);
    }

    @Override
    public ContentView createContentView(boolean privateBrowsing) {
        ContentView w = instantiateWebView(null, android.R.attr.webViewStyle, privateBrowsing);
        initWebViewSettings(w);
        return w;
    }

    protected void initWebViewSettings(ContentView w) {
        w.setScrollbarFadingEnabled(true);
        w.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
//        w.setMapTrackballToArrowKeys(false); // use trackball directly
        // Enable the built-in zoom
//        w.getSettings().setBuiltInZoomControls(true);
        final PackageManager pm = mContext.getPackageManager();
        boolean supportsMultiTouch =
                pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH)
                || pm.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT);
//        w.getSettings().setDisplayZoomControls(!supportsMultiTouch);

        // Add this WebView to the settings observer list and update the
        // settings
        final BrowserSettings s = BrowserSettings.getInstance();
//        s.startManagingSettings(w.getSettings());
    }

}