package com.borqs.browser;

import java.util.List;

import org.chromium.content.browser.ContentView;

import com.borqs.browser.UI.ComboViews;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

/**
 * UI aspect of the controller
 */
public interface UiController {

    UI getUi();

    ContentView getCurrentWebView();

    ContentView getCurrentTopContentView();

    Tab getCurrentTab();

    TabControl getTabControl();

    BrowserSettings getSettings();

    void shareCurrentPage();

    void hideCustomView();

    void attachSubWindow(Tab tab);

    void removeSubWindow(Tab tab);

    boolean isInCustomActionMode();

    void endActionMode();

    void stopLoading();

    boolean onOptionsItemSelected(MenuItem item);

    void setBlockEvents(boolean block);

    Tab openTab(String url, boolean incognito, boolean setActive,
            boolean useCurrent);

    void setActiveTab(Tab tab);

    boolean switchToTab(Tab tab);

    void closeCurrentTab();

    void closeTab(Tab tab);

    void updateMenuState(Tab tab, Menu menu);

    void showPageInfo();

    Activity getActivity();

    void loadUrl(Tab tab, String url);

    void handleNewIntent(Intent intent);

    void bookmarksOrHistoryPicker(ComboViews startView);

    Tab openTabToHomePage();

    void bookmarkCurrentPage();
    
    void openPreferences();
    
    ContentView getCurrentTopWebView();
}