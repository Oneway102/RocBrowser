package com.borqs.browser;

import org.chromium.content.browser.ContentView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;

interface ContentViewController {

    Context getContext();

    Activity getActivity();

    TabControl getTabControl();

    ContentViewFactory getContentViewFactory();

    void onSetContentView(Tab tab, ContentView view);

    void endActionMode();

    void attachSubWindow(Tab tab);

    void dismissSubWindow(Tab tab);

    void onPageStarted(Tab tab, ContentView view, Bitmap favicon);

    void onPageFinished(Tab tab);

    void onProgressChanged(Tab tab);

    void onReceivedTitle(Tab tab, final String title);

    void onUpdatedSecurityState(Tab tab);
    
    
    boolean createTabWitNativeContents(String url, Tab parent, boolean setActive,
            boolean useCurrent, int nativeContentsPtr);
    
    void bookmarkedStatusHasChanged(Tab tab);
    
    void createSubWindow(Tab tab);
}