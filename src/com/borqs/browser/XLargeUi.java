package com.borqs.browser;

import java.util.List;

import org.chromium.content.browser.ContentView;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;

/**
 * Ui for xlarge screen sizes
 */
public class XLargeUi extends BaseUi {

    private ActionBar mActionBar;
    private Handler mHandler;

    /**
     * @param browser
     * @param controller
     */
    public XLargeUi(Activity browser, UiController controller) {
        super(browser, controller);
        mHandler = new Handler();
//        mNavBar = (NavigationBarTablet) mTitleBar.getNavigationBar();
//        mTabBar = new TabBar(mActivity, mUiController, this);
        mActionBar = mActivity.getActionBar();
        setupActionBar();
//        setUseQuickControls(BrowserSettings.getInstance().useQuickControls());
    }

    private void setupActionBar() {
        
    }

    private boolean isTypingKey(KeyEvent evt) {
        return evt.getUnicodeChar() > 0;
    }

    @Override
    public void onPause() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onResume() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean onBackKey() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean onMenuKey() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean needsRestoreAllTabs() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void addTab(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void removeTab(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setActiveTab(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void updateTabs(List<Tab> tabs) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void detachTab(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void attachTab(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onSetWebView(Tab tab, ContentView view) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean isWebShowing() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void updateMenuState(Tab tab, Menu menu) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onPageStopped(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onProgressChanged(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void showMaxTabsWarning() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onTabDataChanged(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean dispatchKey(int code, KeyEvent event) {
        if (mActiveTab != null) {
            ContentView web = mActiveTab.getWebView();
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (code) {
                    case KeyEvent.KEYCODE_TAB:
                    case KeyEvent.KEYCODE_DPAD_UP:
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if ((web != null) && web.hasFocus() && !mTitleBar.hasFocus()) {
                            editUrl(false, false);
                            return true;
                        }
                }
                boolean ctrl = event.hasModifiers(KeyEvent.META_CTRL_ON);
                if (!ctrl && isTypingKey(event) && !mTitleBar.isEditingUrl()) {
                    editUrl(true, false);
                    return mContentView.dispatchKeyEvent(event);
                }
            }
        }
        return false;
    }

	@Override
	public void bookmarkedStatusHasChanged(Tab tab) {
		// TODO Auto-generated method stub
		
	}
}