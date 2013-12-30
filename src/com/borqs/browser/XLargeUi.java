package com.borqs.browser;

import java.util.List;

import org.chromium.content.browser.ContentView;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.Configuration;
import android.os.Handler;
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
}