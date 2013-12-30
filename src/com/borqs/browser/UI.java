package com.borqs.browser;

import java.util.List;

import org.chromium.content.browser.ContentView;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient.CustomViewCallback;

/**
 * UI interface definitions
 */
public interface UI {

    public static enum ComboViews {
        History,
        Bookmarks,
        Snapshots,
    }

    public void onPause();

    public void onResume();

    public void onDestroy();

    public void onConfigurationChanged(Configuration config);

    public boolean onBackKey();

    public boolean onMenuKey();

    public boolean needsRestoreAllTabs();

    public void addTab(Tab tab);

    public void removeTab(Tab tab);

    public void setActiveTab(Tab tab);

    public void updateTabs(List<Tab> tabs);

    public void detachTab(Tab tab);

    public void attachTab(Tab tab);

    public void onSetWebView(Tab tab, ContentView view);

    public void createSubWindow(Tab tab, ContentView subWebView);

    public void attachSubWindow(View subContainer);

    public void removeSubWindow(View subContainer);

    public void showCustomView(View view, int requestedOrientation);

    public void onHideCustomView();

    public boolean isCustomViewShowing();

    // returns if the web page is clear of any overlays (not including sub windows)
    public boolean isWebShowing();

    public void showWeb(boolean animate);

    void editUrl(boolean clearInput, boolean forceIME);

    public void updateMenuState(Tab tab, Menu menu);

    public void onPageStopped(Tab tab);

    public void onProgressChanged(Tab tab);

//    Tab openTab(String url, boolean incognito, boolean setActive,
//            boolean useCurrent);

    void showMaxTabsWarning();

    public void onTabDataChanged(Tab tab);
}