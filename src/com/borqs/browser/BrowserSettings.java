package com.borqs.browser;

import org.chromium.content.browser.ContentView;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class BrowserSettings implements OnSharedPreferenceChangeListener {

    private static BrowserSettings sInstance;

    private Context mContext;

    public static void initialize(final Context context) {
        sInstance = new BrowserSettings(context);
    }

    public static BrowserSettings getInstance() {
        return sInstance;
    }

    private BrowserSettings(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        // TODO Auto-generated method stub
        
    }

    public String getHomePage() {
        return "http://www.baidu.com";
    }
    
    public boolean hasDesktopUseragent(ContentView view) {
        return false;
    }
    
    public boolean enableNavDump() {
        return false;
    }
    
    public boolean isDebugEnabled() {
        return false;
    }
    
    boolean allowAppTabs() {
        return false;
    }
    
    boolean openInBackground() {
        return false;
    }
}