package com.borqs.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chromium.content.browser.ContentSettings;
import org.chromium.content.browser.ContentView;
import org.chromium.ui.WindowAndroid;
import com.borqs.browser.IntentHandler.UrlData;
import com.borqs.browser.UI.ComboViews;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;


/**
 * Controller for browser
 */
public class Controller
        implements ContentViewController, UiController, ActivityController {

    private static final String LOGTAG = "Controller";

    // public message ids
    public final static int LOAD_URL = 1001;
    public final static int STOP_LOAD = 1002;

    // Message Ids
    private static final int FOCUS_NODE_HREF = 102;
    private static final int RELEASE_WAKELOCK = 107;

    static final int UPDATE_BOOKMARK_THUMBNAIL = 108;

    private static final int OPEN_BOOKMARKS = 201;

    private static final int EMPTY_MENU = -1;

    // activity requestCode
    final static int COMBO_VIEW = 1;
    public final static int PREFERENCES_PAGE = 3;
    final static int FILE_SELECTED = 4;
    final static int AUTOFILL_SETUP = 5;
    final static int VOICE_RESULT = 6;

    private final static int WAKELOCK_TIMEOUT = 5 * 60 * 1000; // 5 minutes

    private Activity mActivity;
    private UI mUi;
    private TabControl mTabControl;
    private BrowserSettings mSettings;
    private ContentViewFactory mFactory;

    private UrlHandler mUrlHandler;
//    private UploadHandler mUploadHandler;
    private IntentHandler mIntentHandler;

    private boolean mActivityPaused = true;
    private boolean mLoadStopped;

    private Handler mHandler;

    private boolean mMenuIsDown;

    // FIXME, temp address onPrepareMenu performance problem.
    // When we move everything out of view, we should rewrite this.
    private int mCurrentMenuState = 0;
    private int mMenuState = R.id.MAIN_MENU;
    private int mOldMenuState = EMPTY_MENU;
    private Menu mCachedMenu;

    // For select and find, we keep track of the ActionMode so that
    // finish() can be called as desired.
    private ActionMode mActionMode;

    private boolean mBlockEvents;
    
    private boolean mShouldShowErrorConsole;

    public Controller(Activity browser, WindowAndroid windowAndroid) {
        mActivity = browser;
        mSettings = BrowserSettings.getInstance();
        mTabControl = new TabControl(this, windowAndroid);
//        mSettings.setController(this);
        mUrlHandler = new UrlHandler(this);
        mIntentHandler = new IntentHandler(mActivity, this);
        mFactory = new MyContentViewFactory(browser);
        //mFactory = new BrowserWebViewFactory(browser);

        startHandler();
    }
    
    void setUi(UI ui) {
        mUi = ui;
    }

    int getMaxTabs() {
        //return mActivity.getResources().getInteger(R.integer.max_tabs);
        return 4;
    }

    boolean isInLoad() {
        final Tab tab = getCurrentTab();
        return (tab != null) && tab.inPageLoad();
    }

    private void startHandler() {
        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
// ww
                }
            }
        };

    }

    protected void closeEmptyTab() {
        Tab current = mTabControl.getCurrentTab();
        if (current != null
                /* ww && current.getWebView().copyBackForwardList().getSize() == 0 */) {
            closeCurrentTab();
        }
    }

    protected void reuseTab(Tab appTab, UrlData urlData) {
        // Dismiss the subwindow if applicable.
        dismissSubWindow(appTab);
        // Since we might kill the WebView, remove it from the
        // content view first.
        mUi.detachTab(appTab);
        // Recreate the main WebView after destroying the old one.
        mTabControl.recreateWebView(appTab);
        // TODO: analyze why the remove and add are necessary
        mUi.attachTab(appTab);
        if (mTabControl.getCurrentTab() != appTab) {
            switchToTab(appTab);
            loadUrlDataIn(appTab, urlData);
        } else {
            // If the tab was the current tab, we have to attach
            // it to the view system again.
            setActiveTab(appTab);
            loadUrlDataIn(appTab, urlData);
        }
    }

    // it is assumed that tabcontrol already knows about the tab
    protected void addTab(Tab tab) {
        mUi.addTab(tab);
    }

    protected void removeTab(Tab tab) {
        mUi.removeTab(tab);
        mTabControl.removeTab(tab);
//        mCrashRecoveryHandler.backupState();
    }

    /*
     * Update the favorites icon if the private browsing isn't enabled and the
     * icon is valid.
     */
    private void maybeUpdateFavicon(Tab tab, final String originalUrl,
            final String url, Bitmap favicon) {
        if (favicon == null) {
            return;
        }
        if (!tab.isPrivateBrowsingEnabled()) {
//  ww          Bookmarks.updateFavicon(mActivity
//                    .getContentResolver(), originalUrl, url, favicon);
        }
    }

    /**
     * Pause all WebView timers using the WebView of the given tab
     * @param tab
     * @return true if the timers are paused or tab is null
     */
    private boolean pauseWebViewTimers(Tab tab) {
        if (tab == null) {
            return true;
        } else if (!tab.inPageLoad()) {
//            CookieSyncManager.getInstance().stopSync();
//            WebViewTimersControl.getInstance().onBrowserActivityPause(getCurrentWebView());
            return true;
        }
        return false;
    }

    private void releaseWakeLock() {
        /* ww
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mHandler.removeMessages(RELEASE_WAKELOCK);
            mWakeLock.release();
        }*/
    }

    private Tab showPreloadedTab(final UrlData urlData) {
        if (!urlData.isPreloaded()) {
            return null;
        }
        final PreloadedTabControl tabControl = urlData.getPreloadedTab();
        final String sbQuery = urlData.getSearchBoxQueryToSubmit();
        if (sbQuery != null) {
            if (!tabControl.searchBoxSubmit(sbQuery, urlData.mUrl, urlData.mHeaders)) {
                // Could not submit query. Fallback to regular tab creation
                tabControl.destroy();
                return null;
            }
        }
        // check tab count and make room for new tab
        if (!mTabControl.canCreateNewTab()) {
            Tab leastUsed = mTabControl.getLeastUsedTab(getCurrentTab());
            if (leastUsed != null) {
                closeTab(leastUsed);
            }
        }
        Tab t = tabControl.getTab();
        t.refreshIdAfterPreload();
        mTabControl.addPreloadedTab(t);
        addTab(t);
        setActiveTab(t);
        return t;
    }
    
    protected boolean isActivityPaused() {
        return mActivityPaused;
    }

    public boolean isMenuDown() {
        return mMenuIsDown;
    }

    // key handling
    protected void onBackKey() {
        if (!mUi.onBackKey()) {
            ContentView subwindow = mTabControl.getCurrentSubWindow();
            if (subwindow != null) {
                if (subwindow.canGoBack()) {
                    subwindow.goBack();
                } else {
                    dismissSubWindow(mTabControl.getCurrentTab());
                }
            } else {
                goBackOnePageOrQuit();
            }
        }
    }

    protected boolean onMenuKey() {
        return mUi.onMenuKey();
    }

    void goBackOnePageOrQuit() {
        Tab current = mTabControl.getCurrentTab();
        if (current == null) {
            /*
             * Instead of finishing the activity, simply push this to the back
             * of the stack and let ActivityManager to choose the foreground
             * activity. As BrowserActivity is singleTask, it will be always the
             * root of the task. So we can use either true or false for
             * moveTaskToBack().
             */
            mActivity.moveTaskToBack(true);
            return;
        }
        if (current.canGoBack()) {
            current.goBack();
        } else {
            // Check to see if we are closing a window that was created by
            // another window. If so, we switch back to that window.
            Tab parent = current.getParent();
            if (parent != null) {
                switchToTab(parent);
                // Now we close the other tab
                closeTab(current);
            } else {
                if ((current.getAppId() != null) || current.closeOnBack()) {
                    closeCurrentTab(true);
                }
                /*
                 * Instead of finishing the activity, simply push this to the back
                 * of the stack and let ActivityManager to choose the foreground
                 * activity. As BrowserActivity is singleTask, it will be always the
                 * root of the task. So we can use either true or false for
                 * moveTaskToBack().
                 */
                mActivity.moveTaskToBack(true);
            }
        }
    }

    boolean didUserStopLoading() {
        return mLoadStopped;
    }

    @Override
    public UI getUi() {
        return mUi;
    }

    @Override
    public ContentView getCurrentWebView() {
        return mTabControl.getCurrentContentView();
    }

    @Override
    public ContentView getCurrentTopContentView() {
        return mTabControl.getCurrentTopContentView();
    }

    @Override
    public Tab getCurrentTab() {
        return mTabControl.getCurrentTab();
    }

    @Override
    public TabControl getTabControl() {
        return mTabControl;
    }

    @Override
    public BrowserSettings getSettings() {
        return mSettings;
    }

    @Override
    public void start(Intent intent) {
//        WebViewClassic.setShouldMonitorWebCoreThread();
        // mCrashRecoverHandler has any previously saved state.
//        mCrashRecoveryHandler.startRecovery(intent);
        this.doStart(intent);
    }

    private void doStart(Intent intent) {
        Bundle icicle = null;
        // Find out if we will restore any state and remember the tab.
        final long currentTabId =
                mTabControl.canRestoreState(icicle, false);
        boolean restoreIncognitoTabs = false;

        if (currentTabId == -1) {
//            BackgroundHandler.execute(new PruneThumbnails(mActivity, null));
            if (intent == null) {
                // This won't happen under common scenarios. The icicle is
                // not null, but there aren't any tabs to restore.
                openTabToHomePage();
            } else {
                final Bundle extra = intent.getExtras();
                // Create an initial tab.
                // If the intent is ACTION_VIEW and data is not null, the Browser is
                // invoked to view the content by another application. In this case,
                // the tab will be close when exit.
                UrlData urlData = IntentHandler.getUrlDataFromIntent(intent);
                Tab t = null;
                if (urlData.isEmpty()) {
                    t = openTabToHomePage();
                } else {
                    t = openTab(urlData);
                }
                if (t != null) {
                    t.setAppId(intent.getStringExtra(android.provider.Browser.EXTRA_APPLICATION_ID));
                }
                ContentView webView = t.getWebView();
                if (extra != null) {
                    int scale = extra.getInt(android.provider.Browser.INITIAL_ZOOM_LEVEL, 0);
                    if (scale > 0 && scale <= 1000) {
//ww                        webView.setInitialScale(scale);
                    }
                }
            }
            mUi.updateTabs(mTabControl.getTabs());
        } else {
            mTabControl.restoreState(icicle, currentTabId, restoreIncognitoTabs,
                    mUi.needsRestoreAllTabs());
            List<Tab> tabs = mTabControl.getTabs();
            ArrayList<Long> restoredTabs = new ArrayList<Long>(tabs.size());
            for (Tab t : tabs) {
                restoredTabs.add(t.getId());
            }
//            BackgroundHandler.execute(new PruneThumbnails(mActivity, restoredTabs));
            if (tabs.size() == 0) {
                openTabToHomePage();
            }
            mUi.updateTabs(tabs);
            // TabControl.restoreState() will create a new tab even if
            // restoring the state fails.
            setActiveTab(mTabControl.getCurrentTab());
            // Intent is non-null when framework thinks the browser should be
            // launching with a new intent (icicle is null).
            if (intent != null) {
                mIntentHandler.onNewIntent(intent);
            }
        }
        // Read JavaScript flags if it exists.
        /* ww
        String jsFlags = getSettings().getJsEngineFlags();
        if (jsFlags.trim().length() != 0) {
            WebViewClassic.fromWebView(getCurrentWebView()).setJsFlags(jsFlags);
        }*/
        if (intent != null
                && BrowserActivity.ACTION_SHOW_BOOKMARKS.equals(intent.getAction())) {
            bookmarksOrHistoryPicker(ComboViews.Bookmarks);
        }
    }
    
    boolean isMenuOrCtrlKey(int keyCode) {
        return (KeyEvent.KEYCODE_MENU == keyCode)
                || (KeyEvent.KEYCODE_CTRL_LEFT == keyCode)
                || (KeyEvent.KEYCODE_CTRL_RIGHT == keyCode);
    }

    /**
     * helper method for key handler
     * returns the current tab if it can't advance
     */
    private Tab getNextTab() {
        int pos = mTabControl.getCurrentPosition() + 1;
        if (pos >= mTabControl.getTabCount()) {
            pos = 0;
        }
        return mTabControl.getTab(pos);
    }

    /**
     * helper method for key handler
     * returns the current tab if it can't advance
     */
    private Tab getPrevTab() {
        int pos  = mTabControl.getCurrentPosition() - 1;
        if ( pos < 0) {
            pos = mTabControl.getTabCount() - 1;
        }
        return  mTabControl.getTab(pos);
    }

    protected void pageUp() {
//        getCurrentTopWebView().pageUp(false);
    }

    protected void pageDown() {
//        getCurrentTopWebView().pageDown(false);
    }

    /**
     * As the menu can be open when loading state changes
     * we must manually update the state of the stop/reload menu
     * item
     */
    private void updateInLoadMenuItems(Menu menu, Tab tab) {
        if (menu == null) {
            return;
        }
        MenuItem dest = menu.findItem(R.id.stop_reload_menu_id);
        MenuItem src = ((tab != null) && tab.inPageLoad()) ?
                menu.findItem(R.id.stop_menu_id):
                menu.findItem(R.id.reload_menu_id);
        if (src != null) {
            dest.setIcon(src.getIcon());
            dest.setTitle(src.getTitle());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void handleNewIntent(Intent intent) {
        if (!mUi.isWebShowing()) {
            mUi.showWeb(false);
        }
        mIntentHandler.onNewIntent(intent);
    }

    @Override
    public void onResume() {
        if (!mActivityPaused) {
            Log.e(LOGTAG, "BrowserActivity is already resumed.");
            return;
        }
        mSettings.setLastRunPaused(false);
        mActivityPaused = false;
        Tab current = mTabControl.getCurrentTab();
        if (current != null) {
            current.resume();
//            resumeWebViewTimers(current);
        }
        releaseWakeLock();

        mUi.onResume();
        /*
        mNetworkHandler.onResume();
        WebView.enablePlatformNotifications();
        NfcHandler.register(mActivity, this);
        if (mVoiceResult != null) {
            mUi.onVoiceResult(mVoiceResult);
            mVoiceResult = null;
        }
        */
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        if (mActivityPaused) {
            Log.e(LOGTAG, "BrowserActivity is already paused.");
            return;
        }
        mActivityPaused = true;
        Tab tab = mTabControl.getCurrentTab();
        if (tab != null) {
            tab.pause();
        }
        mUi.onPause();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void shareCurrentPage() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Context getContext() {
        return mActivity;
    }

    @Override
    public Activity getActivity() {
        return mActivity;
    }

    @Override
    public void onSetContentView(Tab tab, ContentView view) {
        mUi.onSetWebView(tab, view);
    }

    @Override
    public void endActionMode() {
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    public ContentViewFactory getContentViewFactory() {
        return mFactory;
    }

    @Override
    public void hideCustomView() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void attachSubWindow(Tab tab) {
        if (tab.getSubContentView() != null) {
            mUi.attachSubWindow(tab.getSubContentView());
            getCurrentTopContentView().requestFocus();
        }
    }

    @Override
    public void removeSubWindow(Tab t) {
        if (t.getSubWebView() != null) {
            mUi.removeSubWindow(t.getSubViewContainer());
        }
    }

    /*
     * True if a custom ActionMode (i.e. find or select) is in use.
     */
    @Override
    public boolean isInCustomActionMode() {
        return mActionMode != null;
    }

    @Override
    public void stopLoading() {
        mLoadStopped = true;
        Tab tab = mTabControl.getCurrentTab();
        ContentView w = getCurrentTopContentView();
        if (w != null) {
            w.stopLoading();
            mUi.onPageStopped(tab);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (null == getCurrentTopContentView()) {
            return false;
        }
        if (mMenuIsDown) {
            // The shortcut action consumes the MENU. Even if it is still down,
            // it won't trigger the next shortcut action. In the case of the
            // shortcut action triggering a new activity, like Bookmarks, we
            // won't get onKeyUp for MENU. So it is important to reset it here.
            mMenuIsDown = false;
        }
        if (mUi.onOptionsItemSelected(item)) {
            // ui callback handled it
            return true;
        }
        switch (item.getItemId()) {
            case R.id.bookmarks_menu_id:
                bookmarksOrHistoryPicker(ComboViews.Bookmarks);
                break;

            case R.id.history_menu_id:
                bookmarksOrHistoryPicker(ComboViews.History);
                break;

            case R.id.snapshots_menu_id:
                bookmarksOrHistoryPicker(ComboViews.Snapshots);
                break;

            case R.id.add_bookmark_menu_id:
                bookmarkCurrentPage();
                break;
                
            case R.id.preferences_menu_id:
            	Log.i("Controller", "R.id.preferences_menu_id");
                openPreferences();
                break;

            default:
                return false;
        }
        return true;
    }

    @Override
    public void openPreferences() {
        Intent intent = new Intent(mActivity, BrowserPreferencesPage.class);
        intent.putExtra(BrowserPreferencesPage.CURRENT_PAGE,
                getCurrentTopWebView().getUrl());
        mActivity.startActivityForResult(intent, PREFERENCES_PAGE);
    }

    @Override
    public void bookmarkCurrentPage() {
        Intent bookmarkIntent = createBookmarkCurrentPageIntent(false);
        if (bookmarkIntent != null) {
            mActivity.startActivity(bookmarkIntent);
        }
    }

    public Intent createBookmarkCurrentPageIntent(boolean editExisting) {
        ContentView w = getCurrentTopContentView();
        if (w == null) {
            return null;
        }
        Intent i = new Intent(mActivity,
                AddBookmarkPage.class);
        i.putExtra(BrowserContract.Bookmarks.URL, w.getUrl());
        i.putExtra(BrowserContract.Bookmarks.TITLE, w.getTitle());
        String touchIconUrl = w.getTouchIconUrl();
        if (touchIconUrl != null) {
            i.putExtra(AddBookmarkPage.TOUCH_ICON_URL, touchIconUrl);
        }

        i.putExtra(BrowserContract.Bookmarks.THUMBNAIL,
                createScreenshot(w, getDesiredThumbnailWidth(mActivity),
                getDesiredThumbnailHeight(mActivity)));
        i.putExtra(BrowserContract.Bookmarks.FAVICON, w.getFavicon());
        if (editExisting) {
            i.putExtra(AddBookmarkPage.CHECK_FOR_DUPE, true);
        }
        // Put the dialog at the upper right of the screen, covering the
        // star on the title bar.
        i.putExtra("gravity", Gravity.RIGHT | Gravity.TOP);
        return i;
    }
    
    static int getDesiredThumbnailWidth(Context context) {
        return context.getResources().getDimensionPixelOffset(
                R.dimen.bookmarkThumbnailWidth);
    }

    static int getDesiredThumbnailHeight(Context context) {
        return context.getResources().getDimensionPixelOffset(
                R.dimen.bookmarkThumbnailHeight);
    }
    private static Bitmap sThumbnailBitmap;
    static Bitmap createScreenshot(ContentView view, int width, int height) {
        if (view == null || view.getContentHeight() == 0
                || view.getContentWidth() == 0) {
            return null;
        }
        // We render to a bitmap 2x the desired size so that we can then
        // re-scale it with filtering since canvas.scale doesn't filter
        // This helps reduce aliasing at the cost of being slightly blurry
        final int filter_scale = 2;
        int scaledWidth = width * filter_scale;
        int scaledHeight = height * filter_scale;
        if (sThumbnailBitmap == null || sThumbnailBitmap.getWidth() != scaledWidth
                || sThumbnailBitmap.getHeight() != scaledHeight) {
            if (sThumbnailBitmap != null) {
                sThumbnailBitmap.recycle();
                sThumbnailBitmap = null;
            }
            sThumbnailBitmap =
                    Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.RGB_565);
        }
        Canvas canvas = new Canvas(sThumbnailBitmap);
        int contentWidth = view.getContentWidth();
        float overviewScale = scaledWidth / (view.getScale() * contentWidth);
/*
        if (view instanceof BrowserContentView) {
            int dy = -((BrowserContentView)view).getTitleHeight();
            canvas.translate(0, dy * overviewScale);
        }
*/
        canvas.scale(overviewScale, overviewScale);

        view.draw(canvas);
        Bitmap ret = Bitmap.createScaledBitmap(sThumbnailBitmap,
                width, height, true);
        canvas.setBitmap(null);
        return ret;
    }
    
    @Override
    public void setBlockEvents(boolean block) {
        mBlockEvents = block;
    }

    // open a non inconito tab with the given url data
    // and set as active tab
    public Tab openTab(UrlData urlData) {
        Tab tab = showPreloadedTab(urlData);
        if (tab == null) {
            tab = createNewTab(false, true, true);
            if ((tab != null) && !urlData.isEmpty()) {
                loadUrlDataIn(tab, urlData);
            }
        }
        return tab;
    }

    @Override
    public Tab openTab(String url, boolean incognito, boolean setActive,
            boolean useCurrent) {
        return openTab(url, incognito, setActive, useCurrent, null);
    }

    public Tab openTab(String url, boolean incognito, boolean setActive,
            boolean useCurrent, Tab parent) {
        Tab tab = createNewTab(incognito, setActive, useCurrent);
        if (tab != null) {
            if (parent != null && parent != tab) {
                parent.addChildTab(tab);
            }
            if (url != null) {
                loadUrl(tab, url);
            }
        }
        return tab;
    }

    // this method will attempt to create a new tab
    // incognito: private browsing tab
    // setActive: ste tab as current tab
    // useCurrent: if no new tab can be created, return current tab
    private Tab createNewTab(boolean incognito, boolean setActive,
            boolean useCurrent) {
        Tab tab = null;
        if (mTabControl.canCreateNewTab()) {
            tab = mTabControl.createNewTab(incognito);
            addTab(tab);
            if (setActive) {
                setActiveTab(tab);
            }
        } else {
            if (useCurrent) {
                tab = mTabControl.getCurrentTab();
                reuseTab(tab, null);
            } else {
                mUi.showMaxTabsWarning();
            }
        }
        return tab;
    }

    @Override
    public void setActiveTab(Tab tab) {
        // monkey protection against delayed start
        if (tab != null) {
            mTabControl.setCurrentTab(tab);
            // the tab is guaranteed to have a webview after setCurrentTab
            mUi.setActiveTab(tab);
        }
    }

    /**
     * @param tab the tab to switch to
     * @return boolean True if we successfully switched to a different tab.  If
     *                 the indexth tab is null, or if that tab is the same as
     *                 the current one, return false.
     */
    @Override
    public boolean switchToTab(Tab tab) {
        Tab currentTab = mTabControl.getCurrentTab();
        if (tab == null || tab == currentTab) {
            return false;
        }
        setActiveTab(tab);
        return true;
    }

    @Override
    public void closeCurrentTab() {
        closeCurrentTab(false);
    }

    protected void closeCurrentTab(boolean andQuit) {
        if (mTabControl.getTabCount() == 1) {
//            mCrashRecoveryHandler.clearState();
            mTabControl.removeTab(getCurrentTab());
            mActivity.finish();
            return;
        }
        final Tab current = mTabControl.getCurrentTab();
        final int pos = mTabControl.getCurrentPosition();
        Tab newTab = current.getParent();
        if (newTab == null) {
            newTab = mTabControl.getTab(pos + 1);
            if (newTab == null) {
                newTab = mTabControl.getTab(pos - 1);
            }
        }
        if (andQuit) {
            mTabControl.setCurrentTab(newTab);
            closeTab(current);
        } else if (switchToTab(newTab)) {
            // Close window
            closeTab(current);
        }
    }

    /**
     * Close the tab, remove its associated title bar, and adjust mTabControl's
     * current tab to a valid value.
     */
    @Override
    public void closeTab(Tab tab) {
        if (tab == mTabControl.getCurrentTab()) {
            closeCurrentTab();
        } else {
            removeTab(tab);
        }
    }

    /**
     * Load the URL into the given WebView and update the title bar
     * to reflect the new load.  Call this instead of WebView.loadUrl
     * directly.
     * @param view The WebView used to load url.
     * @param url The URL to load.
     */
    @Override
    public void loadUrl(Tab tab, String url) {
        loadUrl(tab, url, null);
    }

    protected void loadUrl(Tab tab, String url, Map<String, String> headers) {
        if (tab != null) {
            dismissSubWindow(tab);
            tab.loadUrl(url, headers);
            mUi.onProgressChanged(tab);
        }
    }

    /**
     * Load UrlData into a Tab and update the title bar to reflect the new
     * load.  Call this instead of UrlData.loadIn directly.
     * @param t The Tab used to load.
     * @param data The UrlData being loaded.
     */
    protected void loadUrlDataIn(Tab t, UrlData data) {
        if (data != null) {
            if (data.isPreloaded()) {
                // this isn't called for preloaded tabs
            } else {
                loadUrl(t, data.mUrl, data.mHeaders);
            }
        }
    }

    // Called when loading from context menu or LOAD_URL message
    protected void loadUrlFromContext(String url) {
        Tab tab = getCurrentTab();
        ContentView view = tab != null ? tab.getWebView() : null;
        // In case the user enters nothing.
        if (url != null && url.length() != 0 && tab != null && view != null) {
            url = UrlUtils.smartUrlFilter(url);
            // ww TODO:  shouldOverrideUrlLoading
            if (true) {
                loadUrl(tab, url);
            }
        }
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
        boolean canGoBack = false;
        boolean canGoForward = false;
        boolean isHome = false;
        boolean isDesktopUa = false;
        boolean isLive = false;
        if (tab != null) {
            canGoBack = tab.canGoBack();
            canGoForward = tab.canGoForward();
            isHome = mSettings.getHomePage().equals(tab.getUrl());
            isDesktopUa = mSettings.hasDesktopUseragent(tab.getWebView());
            isLive = !tab.isSnapshot();
        }
        final MenuItem back = menu.findItem(R.id.back_menu_id);
        back.setEnabled(canGoBack);

        final MenuItem home = menu.findItem(R.id.homepage_menu_id);
        home.setEnabled(!isHome);

        final MenuItem forward = menu.findItem(R.id.forward_menu_id);
        forward.setEnabled(canGoForward);

        final MenuItem source = menu.findItem(isInLoad() ? R.id.stop_menu_id
                : R.id.reload_menu_id);
        final MenuItem dest = menu.findItem(R.id.stop_reload_menu_id);
        if (source != null && dest != null) {
            dest.setTitle(source.getTitle());
            dest.setIcon(source.getIcon());
        }
        menu.setGroupVisible(R.id.NAV_MENU, isLive);

        // decide whether to show the share link option
        PackageManager pm = mActivity.getPackageManager();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        ResolveInfo ri = pm.resolveActivity(send,
                PackageManager.MATCH_DEFAULT_ONLY);
        menu.findItem(R.id.share_page_menu_id).setVisible(ri != null);

        boolean isNavDump = mSettings.enableNavDump();
        final MenuItem nav = menu.findItem(R.id.dump_nav_menu_id);
        nav.setVisible(isNavDump);
        nav.setEnabled(isNavDump);

        boolean showDebugSettings = mSettings.isDebugEnabled();
        final MenuItem uaSwitcher = menu.findItem(R.id.ua_desktop_menu_id);
        uaSwitcher.setChecked(isDesktopUa);
        menu.setGroupVisible(R.id.LIVE_MENU, isLive);
        menu.setGroupVisible(R.id.SNAPSHOT_MENU, !isLive);
        menu.setGroupVisible(R.id.COMBO_MENU, false);

        mUi.updateMenuState(tab, menu);
    }

    @Override
    public void showPageInfo() {
        //mPageDialogsHandler.showPageInfo(mTabControl.getCurrentTab(), false, null);
    }

    @Override
    public void dismissSubWindow(Tab tab) {
        removeSubWindow(tab);
        // dismiss the subwindow. This will destroy the WebView.
        tab.dismissSubWindow();
        ContentView wv = getCurrentTopContentView();
        if (wv != null) {
            wv.requestFocus();
        }
    }

    @Override
    public void onPageStarted(Tab tab, ContentView view, Bitmap favicon) {
        // We've started to load a new page. If there was a pending message
        // to save a screenshot then we will now take the new page and save
        // an incorrect screenshot. Therefore, remove any pending thumbnail
        // messages from the queue.
        mHandler.removeMessages(Controller.UPDATE_BOOKMARK_THUMBNAIL,
                tab);

        // reset sync timer to avoid sync starts during loading a page
//        CookieSyncManager.getInstance().resetSync();
/* ww
        if (!mNetworkHandler.isNetworkUp()) {
            view.setNetworkAvailable(false);
        }
*/
        // when BrowserActivity just starts, onPageStarted may be called before
        // onResume as it is triggered from onCreate. Call resumeWebViewTimers
        // to start the timer. As we won't switch tabs while an activity is in
        // pause state, we can ensure calling resume and pause in pair.
        if (mActivityPaused) {
//ww            resumeWebViewTimers(tab);
        }
        mLoadStopped = false;
        endActionMode();

        mUi.onTabDataChanged(tab);

        String url = tab.getUrl();
        // update the bookmark database for favicon
        maybeUpdateFavicon(tab, null, url, favicon);

        //Performance.tracePageStart(url);

        // Performance probe
        if (false) {
            //Performance.onPageStarted();
        }
    }

    @Override
    public void onPageFinished(Tab tab) {
//        mCrashRecoveryHandler.backupState();
        mUi.onTabDataChanged(tab);
        // pause the WebView timer and release the wake lock if it is finished
        // while BrowserActivity is in pause state.

        if (mActivityPaused && pauseWebViewTimers(tab)) {
            releaseWakeLock();
        }

        // Performance probe
        if (false) {
            ;//Performance.onPageFinished(tab.getUrl());
         }

        //Performance.tracePageFinished();        
    }

    @Override
    public void onProgressChanged(Tab tab) {
        int newProgress = tab.getLoadProgress();

        if (newProgress == 100) {
//            CookieSyncManager.getInstance().sync();
            // onProgressChanged() may continue to be called after the main
            // frame has finished loading, as any remaining sub frames continue
            // to load. We'll only get called once though with newProgress as
            // 100 when everything is loaded. (onPageFinished is called once
            // when the main frame completes loading regardless of the state of
            // any sub frames so calls to onProgressChanges may continue after
            // onPageFinished has executed)
            if (tab.inPageLoad()) {
                updateInLoadMenuItems(mCachedMenu, tab);
            } else if (mActivityPaused && pauseWebViewTimers(tab)) {
                // pause the WebView timer and release the wake lock if it is
                // finished while BrowserActivity is in pause state.
                releaseWakeLock();
            }
            if (!tab.isPrivateBrowsingEnabled()
                    && !TextUtils.isEmpty(tab.getUrl())
                    && !tab.isSnapshot()) {
                // Only update the bookmark screenshot if the user did not
                // cancel the load early and there is not already
                // a pending update for the tab.
                if (tab.shouldUpdateThumbnail() &&
                        (tab.inForeground() && !didUserStopLoading()
                        || !tab.inForeground())) {
                    if (!mHandler.hasMessages(UPDATE_BOOKMARK_THUMBNAIL, tab)) {
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(
                                UPDATE_BOOKMARK_THUMBNAIL, 0, 0, tab),
                                500);
                    }
                }
            }
        } else {
            if (!tab.inPageLoad()) {
                // onPageFinished may have already been called but a subframe is
                // still loading
                // updating the progress and
                // update the menu items.
                updateInLoadMenuItems(mCachedMenu, tab);
            }
        }
        mUi.onProgressChanged(tab);
    }

    @Override
    public void onReceivedTitle(Tab tab, String title) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void bookmarksOrHistoryPicker(ComboViews startView) {
        if (mTabControl.getCurrentContentView() == null) {
            return;
        }
        // clear action mode
        if (isInCustomActionMode()) {
            endActionMode();
        }
        Bundle extras = new Bundle();
        // Disable opening in a new window if we have maxed out the windows
        extras.putBoolean(BrowserBookmarksPage.EXTRA_DISABLE_WINDOW,
                !mTabControl.canCreateNewTab());
        mUi.showComboView(startView, extras);
    }

    @Override
    public Tab openTabToHomePage() {
        return openTab(mSettings.getHomePage(), false, true, false);
    }

    @Override
    public void onUpdatedSecurityState(Tab tab) {
        mUi.onTabDataChanged(tab);
    }

    /**
     * handle key events in browser
     *
     * @param keyCode
     * @param event
     * @return true if handled, false to pass to super
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean noModifiers = event.hasNoModifiers();
        // Even if MENU is already held down, we need to call to super to open
        // the IME on long press.
        if (!noModifiers && isMenuOrCtrlKey(keyCode)) {
            mMenuIsDown = true;
            return false;
        }

        ContentView webView = getCurrentTopContentView();
        Tab tab = getCurrentTab();
        if (webView == null || tab == null) return false;

        boolean ctrl = event.hasModifiers(KeyEvent.META_CTRL_ON);
        boolean shift = event.hasModifiers(KeyEvent.META_SHIFT_ON);

        switch(keyCode) {
            case KeyEvent.KEYCODE_TAB:
                if (event.isCtrlPressed()) {
                    if (event.isShiftPressed()) {
                        // prev tab
                        switchToTab(getPrevTab());
                    } else {
                        // next tab
                        switchToTab(getNextTab());
                    }
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_SPACE:
                // WebView/WebTextView handle the keys in the KeyDown. As
                // the Activity's shortcut keys are only handled when WebView
                // doesn't, have to do it in onKeyDown instead of onKeyUp.
                if (shift) {
                    pageUp();
                } else if (noModifiers) {
                    pageDown();
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (!noModifiers) break;
                event.startTracking();
                return true;
            case KeyEvent.KEYCODE_FORWARD:
                if (!noModifiers) break;
                tab.goForward();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (ctrl) {
                    tab.goBack();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (ctrl) {
                    tab.goForward();
                    return true;
                }
                break;
        }
        // it is a regular key and webview is not null
         return mUi.dispatchKey(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        switch(keyCode) {
        case KeyEvent.KEYCODE_BACK:
            if (mUi.isWebShowing()) {
                bookmarksOrHistoryPicker(ComboViews.History);
                return true;
            }
            break;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (isMenuOrCtrlKey(keyCode)) {
            mMenuIsDown = false;
            if (KeyEvent.KEYCODE_MENU == keyCode
                    && event.isTracking() && !event.isCanceled()) {
                return onMenuKey();
            }
        }
        if (!event.hasNoModifiers()) return false;
        switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (event.isTracking() && !event.isCanceled()) {
                    onBackKey();
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mMenuState == EMPTY_MENU) {
            return false;
        }
        MenuInflater inflater = mActivity.getMenuInflater();
        inflater.inflate(R.menu.browser, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        updateInLoadMenuItems(menu, getCurrentTab());
        // hold on to the menu reference here; it is used by the page callbacks
        // to update the menu based on loading state
        mCachedMenu = menu;
        // Note: setVisible will decide whether an item is visible; while
        // setEnabled() will decide whether an item is enabled, which also means
        // whether the matching shortcut key will function.
        switch (mMenuState) {
            case EMPTY_MENU:
                if (mCurrentMenuState != mMenuState) {
                    menu.setGroupVisible(R.id.MAIN_MENU, false);
                    menu.setGroupEnabled(R.id.MAIN_MENU, false);
                    menu.setGroupEnabled(R.id.MAIN_SHORTCUT_MENU, false);
                }
                break;
            default:
                if (mCurrentMenuState != mMenuState) {
                    menu.setGroupVisible(R.id.MAIN_MENU, true);
                    menu.setGroupEnabled(R.id.MAIN_MENU, true);
                    menu.setGroupEnabled(R.id.MAIN_SHORTCUT_MENU, true);
                }
                updateMenuState(getCurrentTab(), menu);
                break;
        }
        mCurrentMenuState = mMenuState;
        return mUi.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        return false;
    }


    public boolean createTabWitNativeContents(String url, Tab parent, boolean setActive,
            boolean useCurrent, int nativeContentsPtr) {
        return ((openTabWithNativeContentsPtr(url, (parent != null) && parent.isPrivateBrowsingEnabled(), 
        		setActive, false, parent, nativeContentsPtr)) != null);
    }
    public Tab openTabWithNativeContentsPtr(String url, boolean incognito, 
    		boolean setActive,
            boolean useCurrent, Tab parent,
            int nativeContentsPtr) {
        Tab tab = createNewTabWithNativeContents(incognito, setActive, 
        		useCurrent, nativeContentsPtr);
        if (tab != null) {
            if (parent != null && parent != tab && !parent.isSnapshot()) {
                parent.addChildTab(tab);
            }
            // do not loadUrl here, because url will be loaded in native, WebContentsImpl
            /*
            if (url != null) {
                loadUrl(tab, url);
            }
            */
        }
        return tab;
    }
    private Tab createNewTabWithNativeContents(boolean incognito, boolean setActive,
            boolean useCurrent, int nativeContentsPtr) {
        Tab tab = null;
        if (mTabControl.canCreateNewTab()) {
            tab = mTabControl.createTabWithNativeContents(nativeContentsPtr, incognito);
            addTab(tab);
            if (setActive) {
                setActiveTab(tab);
            }
        } else {
        	/*
            if (useCurrent) {
                tab = mTabControl.getCurrentTab();
                reuseTab(tab, null);
            } else {
                mUi.showMaxTabsWarning();
            }*/
        	mUi.showMaxTabsWarning();
        }
        return tab;
    }

	@Override
	public void bookmarkedStatusHasChanged(Tab tab) {
		// TODO Auto-generated method stub
        mUi.bookmarkedStatusHasChanged(tab);
	}
 
	// Helper method for getting the top window.
    @Override
    public ContentView getCurrentTopWebView() {
        return mTabControl.getCurrentTopWebView();
    }
    
    protected void setShouldShowErrorConsole(boolean show) {
        if (show == mShouldShowErrorConsole) {
            // Nothing to do.
            return;
        }
        mShouldShowErrorConsole = show;
        Tab t = mTabControl.getCurrentTab();
        if (t == null) {
            // There is no current tab so we cannot toggle the error console
            return;
        }
        mUi.setShouldShowErrorConsole(t, show);
    }
    
    @Override
    public void createSubWindow(Tab tab) {
        endActionMode();
        ContentView mainView = tab.getWebView();
        ContentView subView = mFactory.createContentView(true);
        mUi.createSubWindow(tab, subView);
    }
}