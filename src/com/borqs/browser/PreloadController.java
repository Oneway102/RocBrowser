package com.borqs.browser;

import org.chromium.content.browser.ContentView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;


public class PreloadController implements ContentViewController {

    private static final boolean LOGD_ENABLED = false;
    private static final String LOGTAG = "PreloadController";

    private Context mContext;

    public PreloadController(Context ctx) {
        mContext = ctx.getApplicationContext();

    }

    @Override
    public Context getContext() {
        return mContext;
    }

    @Override
    public Activity getActivity() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TabControl getTabControl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ContentViewFactory getContentViewFactory() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onSetContentView(Tab tab, ContentView view) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void endActionMode() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void attachSubWindow(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void dismissSubWindow(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onPageStarted(Tab tab, ContentView view, Bitmap favicon) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onPageFinished(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onProgressChanged(Tab tab) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onReceivedTitle(Tab tab, String title) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onUpdatedSecurityState(Tab tab) {
        // TODO Auto-generated method stub
        
    }

	@Override
	public boolean createTabWitNativeContents(String url, Tab parent,
			boolean setActive, boolean useCurrent, int nativeContentsPtr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void bookmarkedStatusHasChanged(Tab tab) {
		// TODO Auto-generated method stub
		
	}

}