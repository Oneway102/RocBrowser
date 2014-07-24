// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.content.browser;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebSettings;

import org.chromium.base.CalledByNative;
import org.chromium.base.JNINamespace;
import org.chromium.base.ThreadUtils;

import java.util.concurrent.Callable;

import android.util.Log;

/**
 * Manages settings state for a ContentView. A ContentSettings instance is obtained
 * from ContentView.getContentSettings(). If ContentView is used in the
 * ContentView.PERSONALITY_VIEW role, all settings are read / write. If ContentView
 * is in the ContentView.PERSONALITY_CHROME role, setting can only be read.
 */
@JNINamespace("content")
public class ContentSettings {
    // This enum corresponds to WebSettings.LayoutAlgorithm. We use our own to be
    // able to extend it.
    public enum LayoutAlgorithm {
        NORMAL,
        SINGLE_COLUMN,
        NARROW_COLUMNS,
        TEXT_AUTOSIZING,
    }

    private static final String TAG = "ContentSettings";

    // This class must be created on the UI thread. Afterwards, it can be
    // used from any thread. Internally, the class uses a message queue
    // to call native code on the UI thread only.

    // The native side of this object. Ownership is retained native-side by the WebContents
    // instance that backs the associated ContentViewCore.
    private int mNativeContentSettings = 0;

    private ContentViewCore mContentViewCore;

    // Custom handler that queues messages to call native code on the UI thread.
    private final EventHandler mEventHandler;

    // Protects access to settings fields.
    private final Object mContentSettingsLock = new Object();
    
    // Lock to protect all settings.
    //private final Object mAwSettingsLock = new Object();

    private boolean mSupportZoom = true;
    private boolean mBuiltInZoomControls = false;
    private boolean mDisplayZoomControls = true;
    
    //private final Context mContext;
    private double mDIPScale;
    
    private LayoutAlgorithm mLayoutAlgorithm = LayoutAlgorithm.NARROW_COLUMNS;
    private int mTextSizePercent = 100;
    private String mStandardFontFamily = "sans-serif";
    private String mFixedFontFamily = "monospace";
    private String mSansSerifFontFamily = "sans-serif";
    private String mSerifFontFamily = "serif";
    private String mCursiveFontFamily = "cursive";
    private String mFantasyFontFamily = "fantasy";
    // TODO(mnaganov): Should be obtained from Android. Problem: it is hidden.
    private String mDefaultTextEncoding = "Latin-1";
    private String mUserAgent;
    private int mMinimumFontSize = 8;
    private int mMinimumLogicalFontSize = 8;
    private int mDefaultFontSize = 16;
    private int mDefaultFixedFontSize = 13;
    private boolean mLoadsImagesAutomatically = true;
    private boolean mImagesEnabled = true;
    private boolean mJavaScriptEnabled = false;
    private boolean mAllowUniversalAccessFromFileURLs = false;
    private boolean mAllowFileAccessFromFileURLs = false;
    private boolean mJavaScriptCanOpenWindowsAutomatically = false;
    private boolean mSupportMultipleWindows = false;
    private PluginState mPluginState = PluginState.OFF;
    private boolean mAppCacheEnabled = false;
    private boolean mDomStorageEnabled = false;
    private boolean mDatabaseEnabled = false;
    private boolean mUseWideViewport = false;
    private boolean mLoadWithOverviewMode = false;
    private boolean mMediaPlaybackRequiresUserGesture = true;
    private String mDefaultVideoPosterURL;
    private float mInitialPageScalePercent = 0;

    private final boolean mSupportDeprecatedTargetDensityDPI = true;

    // Not accessed by the native side.
    private boolean mBlockNetworkLoads;  // Default depends on permission of embedding APK.
    private boolean mAllowContentUrlAccess = true;
    private boolean mAllowFileUrlAccess = true;
    private int mCacheMode = WebSettings.LOAD_DEFAULT;
    private boolean mShouldFocusFirstNode = true;
    private boolean mGeolocationEnabled = true;
    static class LazyDefaultUserAgent{
        // Lazy Holder pattern
        private static final String sInstance = nativeGetDefaultUserAgent();
    }
    
    // Protects access to settings global fields.
    private static final Object sGlobalContentSettingsLock = new Object();
    // For compatibility with the legacy WebView, we can only enable AppCache when the path is
    // provided. However, we don't use the path, so we just check if we have received it from the
    // client.
    private static boolean sAppCachePathIsSet = false;

    // The native side of this object.
    //private int mNativeAwSettings = 0;

    // A flag to avoid sending superfluous synchronization messages.
    private boolean mIsUpdateWebkitPrefsMessagePending = false;

    private static final int MINIMUM_FONT_SIZE = 1;
    private static final int MAXIMUM_FONT_SIZE = 72;

    // Class to handle messages to be processed on the UI thread.
    private class EventHandler {
        // Message id for updating multi-touch zoom state in the view
        private static final int UPDATE_MULTI_TOUCH = 2;

        // Message id for updating Webkit preferences
        private static final int UPDATE_WEBKIT_PREFERENCES = 0;
        
        // Actual UI thread handler
        private Handler mHandler;

        EventHandler() {
            //if (mContentViewCore.isPersonalityView()) {
            	Log.i("ContentSettings", "EventHandler");
                mHandler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        switch (msg.what) {
                            case UPDATE_MULTI_TOUCH:
                                if (mContentViewCore.isAlive()) {
                                    mContentViewCore.updateMultiTouchZoomSupport();
                                }
                                break;
                            case UPDATE_WEBKIT_PREFERENCES:
                                synchronized (mContentSettingsLock) {
                                    updateWebkitPreferencesOnUiThread();
                                    mIsUpdateWebkitPrefsMessagePending = false;
                                    mContentSettingsLock.notifyAll();
                                }
                                break;
                        }
                    }
                };
            //}
        }

        private void sendUpdateMultiTouchMessageLocked() {
            assert Thread.holdsLock(mContentSettingsLock);
            if (mNativeContentSettings == 0) return;
            mHandler.sendMessage(Message.obtain(null, UPDATE_MULTI_TOUCH));
        }
        
        private void updateWebkitPreferencesLocked() {
            assert Thread.holdsLock(mContentSettingsLock);
        	//assert Thread.holdsLock(mContentSettingsLock);
            //if (mNativeAwSettings == 0) return;
            if (mNativeContentSettings == 0) return;
            if (Looper.myLooper() == mHandler.getLooper()) {
                updateWebkitPreferencesOnUiThread();
            } else {
                // We're being called on a background thread, so post a message.
                if (mIsUpdateWebkitPrefsMessagePending) {
                    return;
                }
                mIsUpdateWebkitPrefsMessagePending = true;
                mHandler.sendMessage(Message.obtain(null, UPDATE_WEBKIT_PREFERENCES));
                // We must block until the settings have been sync'd to native to
                // ensure that they have taken effect.
                try {
                    while (mIsUpdateWebkitPrefsMessagePending) {
                        mContentSettingsLock.wait();
                    }
                } catch (InterruptedException e) {}
            }
        }
    }

    /**
     * Package constructor to prevent clients from creating a new settings
     * instance. Must be called on the UI thread.
     */
    ContentSettings(ContentViewCore contentViewCore, int nativeContentView) {
        ThreadUtils.assertOnUiThread();
        mContentViewCore = contentViewCore;
        mNativeContentSettings = nativeInit(nativeContentView);
        assert mNativeContentSettings != 0;
        //assert mNativeAwSettings != 0;
        
        //if (isAccessFromFileURLsGrantedByDefault) {
            mAllowUniversalAccessFromFileURLs = true;
            mAllowFileAccessFromFileURLs = true;
        //}

        mEventHandler = new EventHandler();
        if (!mContentViewCore.isPersonalityView()) {
            mBuiltInZoomControls = true;
            mDisplayZoomControls = false;
        }
        
        mUserAgent = LazyDefaultUserAgent.sInstance;
        //nativeUpdateEverything(mNativeAwSettings);
        nativeUpdateEverything(mNativeContentSettings);
    }

    /**
     * Notification from the native side that it is being destroyed.
     * @param nativeContentSettings the native instance that is going away.
     */
    @CalledByNative
    private void onNativeContentSettingsDestroyed(int nativeContentSettings) {
        assert mNativeContentSettings == nativeContentSettings;
        mNativeContentSettings = 0;
    }

    /**
     * Sets whether the WebView should support zooming using its on-screen zoom
     * controls and gestures. The particular zoom mechanisms that should be used
     * can be set with {@link #setBuiltInZoomControls}. This setting does not
     * affect zooming performed using the {@link WebView#zoomIn()} and
     * {@link WebView#zoomOut()} methods. The default is true.
     *
     * @param support whether the WebView should support zoom
     */
    public void setSupportZoom(boolean support) {
        synchronized (mContentSettingsLock) {
            mSupportZoom = support;
            mEventHandler.sendUpdateMultiTouchMessageLocked();
        }
    }

    /**
     * Gets whether the WebView supports zoom.
     *
     * @return true if the WebView supports zoom
     * @see #setSupportZoom
     */
    public boolean supportZoom() {
        return mSupportZoom;
    }

   /**
     * Sets whether the WebView should use its built-in zoom mechanisms. The
     * built-in zoom mechanisms comprise on-screen zoom controls, which are
     * displayed over the WebView's content, and the use of a pinch gesture to
     * control zooming. Whether or not these on-screen controls are displayed
     * can be set with {@link #setDisplayZoomControls}. The default is false,
     * due to compatibility reasons.
     * <p>
     * The built-in mechanisms are the only currently supported zoom
     * mechanisms, so it is recommended that this setting is always enabled.
     * In other words, there is no point of calling this method other than
     * with the 'true' parameter.
     *
     * @param enabled whether the WebView should use its built-in zoom mechanisms
     */
     public void setBuiltInZoomControls(boolean enabled) {
        synchronized (mContentSettingsLock) {
            mBuiltInZoomControls = enabled;
            mEventHandler.sendUpdateMultiTouchMessageLocked();
        }
    }

    /**
     * Gets whether the zoom mechanisms built into WebView are being used.
     *
     * @return true if the zoom mechanisms built into WebView are being used
     * @see #setBuiltInZoomControls
     */
    public boolean getBuiltInZoomControls() {
        return mBuiltInZoomControls;
    }

    /**
     * Sets whether the WebView should display on-screen zoom controls when
     * using the built-in zoom mechanisms. See {@link #setBuiltInZoomControls}.
     * The default is true.
     *
     * @param enabled whether the WebView should display on-screen zoom controls
     */
    public void setDisplayZoomControls(boolean enabled) {
        synchronized (mContentSettingsLock) {
            mDisplayZoomControls = enabled;
            mEventHandler.sendUpdateMultiTouchMessageLocked();
        }
    }

    /**
     * Gets whether the WebView displays on-screen zoom controls when using
     * the built-in zoom mechanisms.
     *
     * @return true if the WebView displays on-screen zoom controls when using
     *         the built-in zoom mechanisms
     * @see #setDisplayZoomControls
     */
    public boolean getDisplayZoomControls() {
        return mDisplayZoomControls;
    }

    boolean supportsMultiTouchZoom() {
        return mSupportZoom && mBuiltInZoomControls;
    }

    boolean shouldDisplayZoomControls() {
        return supportsMultiTouchZoom() && mDisplayZoomControls;
    }

    /**
     * Return true if JavaScript is enabled.
     *
     * @return True if JavaScript is enabled.
     */
    public boolean getJavaScriptEnabled() {
        return ThreadUtils.runOnUiThreadBlockingNoException(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    if (mNativeContentSettings != 0) {
                        return nativeGetJavaScriptEnabled(mNativeContentSettings);
                    } else {
                        return false;
                    }
                }
            });
    }

    /**
     * Sets the settings in this object to those from another
     * ContentSettings.
     * Required by WebView when we swap a in a new ContentViewCore
     * to an existing AwContents (i.e. to support displaying popup
     * windows in an already created WebView)
     */
    public void initFrom(ContentSettings settings) {
        setSupportZoom(settings.supportZoom());
        setBuiltInZoomControls(settings.getBuiltInZoomControls());
        setDisplayZoomControls(settings.getDisplayZoomControls());
    }
    
    public void setDIPScale(double dipScale) {
        synchronized (mContentSettingsLock) {
            mDIPScale = dipScale;
        }
    }

    public void setWebContents(int nativeWebContents) {
        synchronized (mContentSettingsLock) {
            nativeSetWebContents(mNativeContentSettings, nativeWebContents);
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setBlockNetworkLoads}.
     */
    /*public void setBlockNetworkLoads(boolean flag) {
        synchronized (mAwSettingsLock) {
            if (!flag && mContext.checkPermission(
                    android.Manifest.permission.INTERNET,
                    Process.myPid(),
                    Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
                throw new SecurityException("Permission denied - " +
                        "application missing INTERNET permission");
            }
            mBlockNetworkLoads = flag;
        }
    }*/

    /**
     * See {@link android.webkit.WebSettings#getBlockNetworkLoads}.
     */
    public boolean getBlockNetworkLoads() {
        synchronized (mContentSettingsLock) {
            return mBlockNetworkLoads;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setAllowFileAccess}.
     */
    public void setAllowFileAccess(boolean allow) {
        synchronized (mContentSettingsLock) {
            if (mAllowFileUrlAccess != allow) {
                mAllowFileUrlAccess = allow;
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getAllowFileAccess}.
     */
    public boolean getAllowFileAccess() {
        synchronized (mContentSettingsLock) {
            return mAllowFileUrlAccess;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setAllowContentAccess}.
     */
    public void setAllowContentAccess(boolean allow) {
        synchronized (mContentSettingsLock) {
            if (mAllowContentUrlAccess != allow) {
                mAllowContentUrlAccess = allow;
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getAllowContentAccess}.
     */
    public boolean getAllowContentAccess() {
        synchronized (mContentSettingsLock) {
            return mAllowContentUrlAccess;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setCacheMode}.
     */
    public void setCacheMode(int mode) {
        synchronized (mContentSettingsLock) {
            if (mCacheMode != mode) {
                mCacheMode = mode;
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getCacheMode}.
     */
    public int getCacheMode() {
        synchronized (mContentSettingsLock) {
            return mCacheMode;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setNeedInitialFocus}.
     */
    public void setShouldFocusFirstNode(boolean flag) {
        synchronized (mContentSettingsLock) {
            mShouldFocusFirstNode = flag;
        }
    }

    @Deprecated
    public void setEnableFixedLayoutMode(final boolean enable) {
        // No-op. Will be removed.
    }

    /**
     * See {@link android.webkit.WebView#setInitialScale}.
     */
    /*public void setInitialPageScale(final float scaleInPercent) {
        synchronized (mContentSettingsLock) {
            if (mInitialPageScalePercent != scaleInPercent) {
                mInitialPageScalePercent = scaleInPercent;
                ThreadUtils.runOnUiThreadBlocking(new Runnable() {
                    @Override
                    public void run() {
                        if (mNativeContentSettings != 0) {
                            nativeUpdateInitialPageScale(mNativeContentSettings);
                        }
                    }
                });
            }
        }
    }*/

    /**
     * See {@link android.webkit.WebSettings#setNeedInitialFocus}.
     */
    public boolean shouldFocusFirstNode() {
        synchronized(mContentSettingsLock) {
            return mShouldFocusFirstNode;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setGeolocationEnabled}.
     */
    public void setGeolocationEnabled(boolean flag) {
        synchronized (mContentSettingsLock) {
            if (mGeolocationEnabled != flag) {
                mGeolocationEnabled = flag;
            }
        }
    }

    /**
     * @return Returns if geolocation is currently enabled.
     */
    boolean getGeolocationEnabled() {
        synchronized (mContentSettingsLock) {
            return mGeolocationEnabled;
        }
    }

    /**
     * @returns the default User-Agent used by each ContentViewCore instance, i.e. unless
     * overridden by {@link #setUserAgentString()}
     */
    public static String getDefaultUserAgent() {
        return LazyDefaultUserAgent.sInstance;
    }

    /**
     * See {@link android.webkit.WebSettings#setUserAgentString}.
     */
    public void setUserAgentString(String ua) {
        synchronized (mContentSettingsLock) {
            final String oldUserAgent = mUserAgent;
            if (ua == null || ua.length() == 0) {
                mUserAgent = LazyDefaultUserAgent.sInstance;
            } else {
                mUserAgent = ua;
            }
            if (!oldUserAgent.equals(mUserAgent)) {
                ThreadUtils.runOnUiThreadBlocking(new Runnable() {
                    @Override
                    public void run() {
                        if (mNativeContentSettings != 0) {
                            nativeUpdateUserAgent(mNativeContentSettings);
                        }
                    }
                });
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getUserAgentString}.
     */
    public String getUserAgentString() {
        synchronized (mContentSettingsLock) {
            return mUserAgent;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setLoadWithOverviewMode}.
     */
    /*public void setLoadWithOverviewMode(boolean overview) {
        synchronized (mContentSettingsLock) {
            if (mLoadWithOverviewMode != overview) {
                mLoadWithOverviewMode = overview;
                mEventHandler.updateWebkitPreferencesLocked();
                ThreadUtils.runOnUiThreadBlocking(new Runnable() {
                    @Override
                    public void run() {
                        if (mNativeContentSettings != 0) {
                            nativeResetScrollAndScaleState(mNativeContentSettings);
                        }
                    }
                });
            }
        }
    }*/

    /**
     * See {@link android.webkit.WebSettings#getLoadWithOverviewMode}.
     */
    public boolean getLoadWithOverviewMode() {
        synchronized (mContentSettingsLock) {
            return mLoadWithOverviewMode;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setTextZoom}.
     */
    public void setTextZoom(final int textZoom) {
        synchronized (mContentSettingsLock) {
            if (mTextSizePercent != textZoom) {
                mTextSizePercent = textZoom;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getTextZoom}.
     */
    public int getTextZoom() {
        synchronized (mContentSettingsLock) {
            return mTextSizePercent;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setStandardFontFamily}.
     */
    public void setStandardFontFamily(String font) {
        synchronized (mContentSettingsLock) {
            if (font != null && !mStandardFontFamily.equals(font)) {
                mStandardFontFamily = font;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getStandardFontFamily}.
     */
    public String getStandardFontFamily() {
        synchronized (mContentSettingsLock) {
            return mStandardFontFamily;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setFixedFontFamily}.
     */
    public void setFixedFontFamily(String font) {
        synchronized (mContentSettingsLock) {
            if (font != null && !mFixedFontFamily.equals(font)) {
                mFixedFontFamily = font;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getFixedFontFamily}.
     */
    public String getFixedFontFamily() {
        synchronized (mContentSettingsLock) {
            return mFixedFontFamily;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setSansSerifFontFamily}.
     */
    public void setSansSerifFontFamily(String font) {
        synchronized (mContentSettingsLock) {
            if (font != null && !mSansSerifFontFamily.equals(font)) {
                mSansSerifFontFamily = font;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getSansSerifFontFamily}.
     */
    public String getSansSerifFontFamily() {
        synchronized (mContentSettingsLock) {
            return mSansSerifFontFamily;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setSerifFontFamily}.
     */
    public void setSerifFontFamily(String font) {
        synchronized (mContentSettingsLock) {
            if (font != null && !mSerifFontFamily.equals(font)) {
                mSerifFontFamily = font;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getSerifFontFamily}.
     */
    public String getSerifFontFamily() {
        synchronized (mContentSettingsLock) {
            return mSerifFontFamily;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setCursiveFontFamily}.
     */
    public void setCursiveFontFamily(String font) {
        synchronized (mContentSettingsLock) {
            if (font != null && !mCursiveFontFamily.equals(font)) {
                mCursiveFontFamily = font;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getCursiveFontFamily}.
     */
    public String getCursiveFontFamily() {
        synchronized (mContentSettingsLock) {
            return mCursiveFontFamily;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setFantasyFontFamily}.
     */
    public void setFantasyFontFamily(String font) {
        synchronized (mContentSettingsLock) {
            if (font != null && !mFantasyFontFamily.equals(font)) {
                mFantasyFontFamily = font;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getFantasyFontFamily}.
     */
    public String getFantasyFontFamily() {
        synchronized (mContentSettingsLock) {
            return mFantasyFontFamily;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setMinimumFontSize}.
     */
    public void setMinimumFontSize(int size) {
        synchronized (mContentSettingsLock) {
            size = clipFontSize(size);
            if (mMinimumFontSize != size) {
                mMinimumFontSize = size;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getMinimumFontSize}.
     */
    public int getMinimumFontSize() {
        synchronized (mContentSettingsLock) {
            return mMinimumFontSize;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setMinimumLogicalFontSize}.
     */
    public void setMinimumLogicalFontSize(int size) {
        synchronized (mContentSettingsLock) {
            size = clipFontSize(size);
            if (mMinimumLogicalFontSize != size) {
                mMinimumLogicalFontSize = size;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getMinimumLogicalFontSize}.
     */
    public int getMinimumLogicalFontSize() {
        synchronized (mContentSettingsLock) {
            return mMinimumLogicalFontSize;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setDefaultFontSize}.
     */
    public void setDefaultFontSize(int size) {
        synchronized (mContentSettingsLock) {
            size = clipFontSize(size);
            if (mDefaultFontSize != size) {
                mDefaultFontSize = size;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getDefaultFontSize}.
     */
    public int getDefaultFontSize() {
        synchronized (mContentSettingsLock) {
            return mDefaultFontSize;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setDefaultFixedFontSize}.
     */
    public void setDefaultFixedFontSize(int size) {
        synchronized (mContentSettingsLock) {
            size = clipFontSize(size);
            if (mDefaultFixedFontSize != size) {
                mDefaultFixedFontSize = size;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getDefaultFixedFontSize}.
     */
    public int getDefaultFixedFontSize() {
        synchronized (mContentSettingsLock) {
            return mDefaultFixedFontSize;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setJavaScriptEnabled}.
     */
    public void setJavaScriptEnabled(boolean flag) {
    	Log.i("ContentSettings", "flag: " + flag);
        synchronized (mContentSettingsLock) {
            if (mJavaScriptEnabled != flag) {
                mJavaScriptEnabled = flag;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setAllowUniversalAccessFromFileURLs}.
     */
    public void setAllowUniversalAccessFromFileURLs(boolean flag) {
        synchronized (mContentSettingsLock) {
            if (mAllowUniversalAccessFromFileURLs != flag) {
                mAllowUniversalAccessFromFileURLs = flag;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setAllowFileAccessFromFileURLs}.
     */
    public void setAllowFileAccessFromFileURLs(boolean flag) {
        synchronized (mContentSettingsLock) {
            if (mAllowFileAccessFromFileURLs != flag) {
                mAllowFileAccessFromFileURLs = flag;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setLoadsImagesAutomatically}.
     */
    public void setLoadsImagesAutomatically(boolean flag) {
        synchronized (mContentSettingsLock) {
            if (mLoadsImagesAutomatically != flag) {
                mLoadsImagesAutomatically = flag;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getLoadsImagesAutomatically}.
     */
    public boolean getLoadsImagesAutomatically() {
        synchronized (mContentSettingsLock) {
            return mLoadsImagesAutomatically;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setImagesEnabled}.
     */
    public void setImagesEnabled(boolean flag) {
        synchronized (mContentSettingsLock) {
            if (mImagesEnabled != flag) {
                mImagesEnabled = flag;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getImagesEnabled}.
     */
    public boolean getImagesEnabled() {
        synchronized (mContentSettingsLock) {
            return mImagesEnabled;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getJavaScriptEnabled}.
     */
    /*public boolean getJavaScriptEnabled() {
        synchronized (mAwSettingsLock) {
            return mJavaScriptEnabled;
        }
    }*/

    /**
     * See {@link android.webkit.WebSettings#getAllowUniversalAccessFromFileURLs}.
     */
    public boolean getAllowUniversalAccessFromFileURLs() {
        synchronized (mContentSettingsLock) {
            return mAllowUniversalAccessFromFileURLs;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getAllowFileAccessFromFileURLs}.
     */
    public boolean getAllowFileAccessFromFileURLs() {
        synchronized (mContentSettingsLock) {
            return mAllowFileAccessFromFileURLs;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setPluginsEnabled}.
     */
    @Deprecated
    public void setPluginsEnabled(boolean flag) {
        setPluginState(flag ? PluginState.ON : PluginState.OFF);
    }

    /**
     * See {@link android.webkit.WebSettings#setPluginState}.
     */
    public void setPluginState(PluginState state) {
        synchronized (mContentSettingsLock) {
            if (mPluginState != state) {
                mPluginState = state;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getPluginsEnabled}.
     */
    @Deprecated
    public boolean getPluginsEnabled() {
        synchronized (mContentSettingsLock) {
            return mPluginState == PluginState.ON;
        }
    }

    /**
     * Return true if plugins are disabled.
     * @return True if plugins are disabled.
     * @hide
     */
    @CalledByNative
    private boolean getPluginsDisabled() {
        // This should only be called from UpdateWebkitPreferences, which is called
        // either from the constructor, or with mAwSettingsLock being held.
        return mPluginState == PluginState.OFF;
    }

    /**
     * See {@link android.webkit.WebSettings#getPluginState}.
     */
    public PluginState getPluginState() {
        synchronized (mContentSettingsLock) {
            return mPluginState;
        }
    }


    /**
     * See {@link android.webkit.WebSettings#setJavaScriptCanOpenWindowsAutomatically}.
     */
    public void setJavaScriptCanOpenWindowsAutomatically(boolean flag) {
        synchronized (mContentSettingsLock) {
            if (mJavaScriptCanOpenWindowsAutomatically != flag) {
                mJavaScriptCanOpenWindowsAutomatically = flag;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getJavaScriptCanOpenWindowsAutomatically}.
     */
    public boolean getJavaScriptCanOpenWindowsAutomatically() {
        synchronized (mContentSettingsLock) {
            return mJavaScriptCanOpenWindowsAutomatically;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setLayoutAlgorithm}.
     */
    public void setLayoutAlgorithm(LayoutAlgorithm l) {
        synchronized (mContentSettingsLock) {
            if (mLayoutAlgorithm != l) {
                mLayoutAlgorithm = l;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getLayoutAlgorithm}.
     */
    public LayoutAlgorithm getLayoutAlgorithm() {
        synchronized (mContentSettingsLock) {
            return mLayoutAlgorithm;
        }
    }

    /**
     * Gets whether Text Auto-sizing layout algorithm is enabled.
     *
     * @return true if Text Auto-sizing layout algorithm is enabled
     * @hide
     */
    @CalledByNative
    private boolean getTextAutosizingEnabled() {
        return mLayoutAlgorithm == LayoutAlgorithm.TEXT_AUTOSIZING;
    }

    /**
     * See {@link android.webkit.WebSettings#setSupportMultipleWindows}.
     */
    public void setSupportMultipleWindows(boolean support) {
        synchronized (mContentSettingsLock) {
            if (mSupportMultipleWindows != support) {
                mSupportMultipleWindows = support;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#supportMultipleWindows}.
     */
    public boolean supportMultipleWindows() {
        synchronized (mContentSettingsLock) {
            return mSupportMultipleWindows;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setUseWideViewPort}.
     */
    public void setUseWideViewPort(boolean use) {
        synchronized (mContentSettingsLock) {
            if (mUseWideViewport != use) {
                mUseWideViewport = use;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getUseWideViewPort}.
     */
    public boolean getUseWideViewPort() {
        synchronized (mContentSettingsLock) {
            return mUseWideViewport;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setAppCacheEnabled}.
     */
    public void setAppCacheEnabled(boolean flag) {
        synchronized (mContentSettingsLock) {
            if (mAppCacheEnabled != flag) {
                mAppCacheEnabled = flag;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setAppCachePath}.
     */
    public void setAppCachePath(String path) {
        boolean needToSync = false;
        synchronized (sGlobalContentSettingsLock) {
            // AppCachePath can only be set once.
            if (!sAppCachePathIsSet && path != null && !path.isEmpty()) {
                sAppCachePathIsSet = true;
                needToSync = true;
            }
        }
        // The obvious problem here is that other WebViews will not be updated,
        // until they execute synchronization from Java to the native side.
        // But this is the same behaviour as it was in the legacy WebView.
        if (needToSync) {
            synchronized (mContentSettingsLock) {
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * Gets whether Application Cache is enabled.
     *
     * @return true if Application Cache is enabled
     * @hide
     */
    @CalledByNative
    private boolean getAppCacheEnabled() {
        // This should only be called from UpdateWebkitPreferences, which is called
        // either from the constructor, or with mAwSettingsLock being held.
        if (!mAppCacheEnabled) {
            return false;
        }
        synchronized (sGlobalContentSettingsLock) {
            return sAppCachePathIsSet;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setDomStorageEnabled}.
     */
    public void setDomStorageEnabled(boolean flag) {
        synchronized (mContentSettingsLock) {
            if (mDomStorageEnabled != flag) {
                mDomStorageEnabled = flag;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getDomStorageEnabled}.
     */
    public boolean getDomStorageEnabled() {
       synchronized (mContentSettingsLock) {
           return mDomStorageEnabled;
       }
    }

    /**
     * See {@link android.webkit.WebSettings#setDatabaseEnabled}.
     */
    public void setDatabaseEnabled(boolean flag) {
        synchronized (mContentSettingsLock) {
            if (mDatabaseEnabled != flag) {
                mDatabaseEnabled = flag;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getDatabaseEnabled}.
     */
    public boolean getDatabaseEnabled() {
       synchronized (mContentSettingsLock) {
           return mDatabaseEnabled;
       }
    }

    /**
     * See {@link android.webkit.WebSettings#setDefaultTextEncodingName}.
     */
    public void setDefaultTextEncodingName(String encoding) {
        synchronized (mContentSettingsLock) {
            if (encoding != null && !mDefaultTextEncoding.equals(encoding)) {
                mDefaultTextEncoding = encoding;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getDefaultTextEncodingName}.
     */
    public String getDefaultTextEncodingName() {
        synchronized (mContentSettingsLock) {
            return mDefaultTextEncoding;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setMediaPlaybackRequiresUserGesture}.
     */
    public void setMediaPlaybackRequiresUserGesture(boolean require) {
        synchronized (mContentSettingsLock) {
            if (mMediaPlaybackRequiresUserGesture != require) {
                mMediaPlaybackRequiresUserGesture = require;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getMediaPlaybackRequiresUserGesture}.
     */
    public boolean getMediaPlaybackRequiresUserGesture() {
        synchronized (mContentSettingsLock) {
            return mMediaPlaybackRequiresUserGesture;
        }
    }

    /**
     * See {@link android.webkit.WebSettings#setDefaultVideoPosterURL}.
     */
    public void setDefaultVideoPosterURL(String url) {
        synchronized (mContentSettingsLock) {
            if (mDefaultVideoPosterURL != null && !mDefaultVideoPosterURL.equals(url) ||
                    mDefaultVideoPosterURL == null && url != null) {
                mDefaultVideoPosterURL = url;
                mEventHandler.updateWebkitPreferencesLocked();
            }
        }
    }

    /**
     * See {@link android.webkit.WebSettings#getDefaultVideoPosterURL}.
     */
    public String getDefaultVideoPosterURL() {
        synchronized (mContentSettingsLock) {
            return mDefaultVideoPosterURL;
        }
    }

    private int clipFontSize(int size) {
        if (size < MINIMUM_FONT_SIZE) {
            return MINIMUM_FONT_SIZE;
        } else if (size > MAXIMUM_FONT_SIZE) {
            return MAXIMUM_FONT_SIZE;
        }
        return size;
    }

    private void updateWebkitPreferencesOnUiThread() {
        if (mNativeContentSettings != 0) {
            ThreadUtils.assertOnUiThread();
            nativeUpdateWebkitPreferences(mNativeContentSettings);
        }
    }

    // Initialize the ContentSettings native side.
    private native int nativeInit(int contentViewPtr);

    private native boolean nativeGetJavaScriptEnabled(int nativeContentSettings);
    
    //private native void nativeResetScrollAndScaleState(int nativeContentSettings);

    private native void nativeSetWebContents(int nativeContentSettings, int nativeWebContents);

    private native void nativeUpdateEverything(int nativeContentSettings);

    //private native void nativeUpdateInitialPageScale(int nativeContentSettings);

    private native void nativeUpdateUserAgent(int nativeContentSettings);

    private native void nativeUpdateWebkitPreferences(int nativeContentSettings);

    private static native String nativeGetDefaultUserAgent();
}
