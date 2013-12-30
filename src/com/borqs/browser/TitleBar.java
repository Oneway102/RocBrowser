package com.borqs.browser;

import org.chromium.content.browser.ContentView;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

/**
 * Base class for a title bar used by the browser.
 */
public class TitleBar extends RelativeLayout {
    private static final int PROGRESS_MAX = 100;
    private static final float ANIM_TITLEBAR_DECELERATE = 2.5f;
Context mContext;
    private UiController mUiController;
    private BaseUi mBaseUi;
    private FrameLayout mContentView;
    private PageProgressView mProgress;

//    private AutologinBar mAutoLogin;
    private NavigationBarBase mNavBar;
    private boolean mUseQuickControls;
    private SnapshotBar mSnapshotBar;

    //state
    private boolean mShowing;
    private boolean mInLoad;
    private boolean mSkipTitleBarAnimations;
    private Animator mTitleBarAnimator;
    private boolean mIsFixedTitleBar;

    public TitleBar(Context context, UiController controller, BaseUi ui,
            FrameLayout contentView) {
        super(context, null);
        mContext = context;
        mUiController = controller;
        mBaseUi = ui;
        mContentView = contentView;
        initLayout(context);
        setFixedTitleBar();
    }

    private void initLayout(Context context) {
        LayoutInflater factory = LayoutInflater.from(context);
        factory.inflate(R.layout.title_bar, this);
        mProgress = (PageProgressView) findViewById(R.id.progress);
        mNavBar = (NavigationBarBase) findViewById(R.id.taburlbar);
        mNavBar.setTitleBar(this);
    }

    private void inflateAutoLoginBar() {
    }

    private void inflateSnapshotBar() {
        if (mSnapshotBar != null) {
            return;
        }

        ViewStub stub = (ViewStub) findViewById(R.id.snapshotbar_stub);
        mSnapshotBar = (SnapshotBar) stub.inflate();
        mSnapshotBar.setTitleBar(this);
    }

    @Override
    protected void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        setFixedTitleBar();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mIsFixedTitleBar) {
            int margin = getMeasuredHeight() - calculateEmbeddedHeight();
            mBaseUi.setContentViewMarginTop(-margin);
        } else {
            mBaseUi.setContentViewMarginTop(0);
        }
    }

    private void setFixedTitleBar() {
        boolean isFixed = !mUseQuickControls
                && !mContext.getResources().getBoolean(R.bool.hide_title);
        // If getParent() returns null, we are initializing
        ViewGroup parent = (ViewGroup)getParent();
        if (mIsFixedTitleBar == isFixed && parent != null) return;
        mIsFixedTitleBar = isFixed;
        setSkipTitleBarAnimations(true);
        show();
        setSkipTitleBarAnimations(false);
        if (parent != null) {
            parent.removeView(this);
        }
        if (mIsFixedTitleBar) {
            mBaseUi.addFixedTitleBar(this);
        } else {
            mContentView.addView(this, makeLayoutParams());
            mBaseUi.setContentViewMarginTop(0);
        }
    }

    public BaseUi getUi() {
        return mBaseUi;
    }

    public UiController getUiController() {
        return mUiController;
    }

    public void setUseQuickControls(boolean use) {
        mUseQuickControls = use;
        setFixedTitleBar();
        if (use) {
            this.setVisibility(View.GONE);
        } else {
            this.setVisibility(View.VISIBLE);
        }
    }

    void setShowProgressOnly(boolean progress) {
        if (progress && !wantsToBeVisible()) {
            mNavBar.setVisibility(View.GONE);
        } else {
            mNavBar.setVisibility(View.VISIBLE);
        }
    }

    void setSkipTitleBarAnimations(boolean skip) {
        mSkipTitleBarAnimations = skip;
    }

    void setupTitleBarAnimator(Animator animator) {
        Resources res = mContext.getResources();
        int duration = res.getInteger(R.integer.titlebar_animation_duration);
        animator.setInterpolator(new DecelerateInterpolator(
                ANIM_TITLEBAR_DECELERATE));
        animator.setDuration(duration);
    }

    void show() {
        cancelTitleBarAnimation(false);
        if (mUseQuickControls || mSkipTitleBarAnimations) {
            this.setVisibility(View.VISIBLE);
            this.setTranslationY(0);
        } else {
            int visibleHeight = getVisibleTitleHeight();
            float startPos = (-getEmbeddedHeight() + visibleHeight);
            if (getTranslationY() != 0) {
                startPos = Math.max(startPos, getTranslationY());
            }
            mTitleBarAnimator = ObjectAnimator.ofFloat(this,
                    "translationY",
                    startPos, 0);
            setupTitleBarAnimator(mTitleBarAnimator);
            mTitleBarAnimator.start();
        }
        mShowing = true;
    }

    void hide() {
        if (mUseQuickControls) {
            this.setVisibility(View.GONE);
        } else {
            if (mIsFixedTitleBar) return;
            if (!mSkipTitleBarAnimations) {
                cancelTitleBarAnimation(false);
                int visibleHeight = getVisibleTitleHeight();
                mTitleBarAnimator = ObjectAnimator.ofFloat(this,
                        "translationY", getTranslationY(),
                        (-getEmbeddedHeight() + visibleHeight));
                mTitleBarAnimator.addListener(mHideTileBarAnimatorListener);
                setupTitleBarAnimator(mTitleBarAnimator);
                mTitleBarAnimator.start();
            } else {
                onScrollChanged();
            }
        }
        mShowing = false;
    }

    boolean isShowing() {
        return mShowing;
    }

    void cancelTitleBarAnimation(boolean reset) {
        if (mTitleBarAnimator != null) {
            mTitleBarAnimator.cancel();
            mTitleBarAnimator = null;
        }
        if (reset) {
            setTranslationY(0);
        }
    }

    private AnimatorListener mHideTileBarAnimatorListener = new AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            // update position
            onScrollChanged();
        }

        @Override
        public void onAnimationCancel(Animator animation) {
        }
    };

    private int getVisibleTitleHeight() {
        Tab tab = mBaseUi.getActiveTab();
        ContentView webview = tab != null ? tab.getWebView() : null;
//ww        return webview != null ? ((BrowserContentView)webview).getVisibleTitleHeight() : 0;
        return webview != null ? 30 : 0;
    }

    /**
     * Update the progress, from 0 to 100.
     */
    public void setProgress(int newProgress) {
        if (newProgress >= PROGRESS_MAX) {
            mProgress.setProgress(PageProgressView.MAX_PROGRESS);
            mProgress.setVisibility(View.GONE);
            mInLoad = false;
            mNavBar.onProgressStopped();
            // check if needs to be hidden
            if (!isEditingUrl() && !wantsToBeVisible()) {
                if (mUseQuickControls) {
                    hide();
                } else {
                    mBaseUi.showTitleBarForDuration();
                }
            }
        } else {
            if (!mInLoad) {
                mProgress.setVisibility(View.VISIBLE);
                mInLoad = true;
                mNavBar.onProgressStarted();
            }
            mProgress.setProgress(newProgress * PageProgressView.MAX_PROGRESS
                    / PROGRESS_MAX);
            if (mUseQuickControls && !isEditingUrl()) {
                setShowProgressOnly(true);
            }
            if (!mShowing) {
                show();
            }
        }
    }

    public int getEmbeddedHeight() {
        if (mUseQuickControls || mIsFixedTitleBar) return 0;
        return calculateEmbeddedHeight();
    }

    private int calculateEmbeddedHeight() {
        int height = mNavBar.getHeight();
        return height;
    }

    public boolean wantsToBeVisible() {
        return (mSnapshotBar != null && mSnapshotBar.getVisibility() == View.VISIBLE
                    && mSnapshotBar.isAnimating());
    }

    public boolean isEditingUrl() {
        return mNavBar.isEditingUrl();
    }

    public ContentView getCurrentWebView() {
        Tab t = mBaseUi.getActiveTab();
        if (t != null) {
            return t.getWebView();
        } else {
            return null;
        }
    }

    public PageProgressView getProgressView() {
        return mProgress;
    }

    public NavigationBarBase getNavigationBar() {
        return mNavBar;
    }

    public boolean useQuickControls() {
        return mUseQuickControls;
    }

    public boolean isInLoad() {
        return mInLoad;
    }

    private ViewGroup.LayoutParams makeLayoutParams() {
        return new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
    }

    @Override
    public View focusSearch(View focused, int dir) {
        ContentView web = getCurrentWebView();
        if (FOCUS_DOWN == dir && hasFocus() && web != null
                && web.hasFocusable() && web.getParent() != null) {
            return web;
        }
        return super.focusSearch(focused, dir);
    }

    public void onTabDataChanged(Tab tab) {
        if (mSnapshotBar != null) {
            mSnapshotBar.onTabDataChanged(tab);
        }

        if (tab.isSnapshot()) {
            inflateSnapshotBar();
            mSnapshotBar.setVisibility(VISIBLE);
            mNavBar.setVisibility(GONE);
        } else {
            if (mSnapshotBar != null) {
                mSnapshotBar.setVisibility(GONE);
            }
            mNavBar.setVisibility(VISIBLE);
        }
    }

    public void onScrollChanged() {
        if (!mShowing && !mIsFixedTitleBar) {
            setTranslationY(getVisibleTitleHeight() - getEmbeddedHeight());
        }
    }

}