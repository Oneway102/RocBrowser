package com.borqs.browser.combo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;

import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import android.widget.Scroller;


public class ScrollLayout extends ViewGroup {

	private static final String TAG = "ScrollLayout";

	private Scroller mScroller;

	private VelocityTracker mVelocityTracker;

	private static final int SHOWING_VIEW = 1;

	private int mWidth;	

	private static final int TOUCH_STATE_REST = 0;
	private static final int TOUCH_STATE_SCROLLING = 1;
	private static final int SNAP_VELOCITY = 600;

	private int mTouchState = TOUCH_STATE_REST;
	private int mTouchSlop;
	private float mLastMotionX;

	private ComboViewActivityMine mContext;

	public ScrollLayout(Context context, AttributeSet attrs) {

		this(context, attrs, 0);

	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
	}
	public ScrollLayout(Context context, AttributeSet attrs, int defStyle) {

		super(context, attrs, defStyle);

		mContext = (ComboViewActivityMine) context;

		mScroller = new Scroller(context);

		mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

	}


	@Override
	protected void attachViewToParent(View child, int index, LayoutParams params) {
		super.attachViewToParent(child, index, params);
	}

	@Override
	public void addView(View child) {
		super.addView(child);
	}

	@Override
	public void requestChildFocus(View child, View focused) {
		Log.d("requestChildFocus", "child = " + child);
		super.requestChildFocus(child, focused);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int childLeft = 0;
		final int childCount = getChildCount();
		for (int i = 0; i < childCount; i++) {
			final View childView = getChildAt(i);
			if (childView.getVisibility() != View.GONE) {
				final int childWidth = childView.getMeasuredWidth();
				childView.layout(childLeft, 0,
						childLeft + childWidth, childView.getMeasuredHeight());
				childLeft += childWidth;
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		final int width = MeasureSpec.getSize(widthMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		Log.e(TAG, "onMeasure width = " + width);

		if (widthMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"ScrollLayout only canmCurScreen run at EXACTLY mode!");
		}

		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		if (heightMode != MeasureSpec.EXACTLY) {
			throw new IllegalStateException(
					"ScrollLayout only can run at EXACTLY mode!");
		}

		// The children are given the same width and height as the scrollLayout
		final int count = getChildCount();
		for (int i = 0; i < count; i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}

		// Log.e(TAG, "moving to screen "+mCurScreen);
		scrollTo(SHOWING_VIEW * width, 0);

	}

	/**
	 * 
	 * According to the position of current layout
	 * 
	 * scroll to the destination page.
	 */

	public void snapToDestination() {
		setMWidth();
		final int destScreen = (getScrollX() + mWidth / 2) / mWidth;
		snapToScreen(destScreen);
	}

	private void setMWidth() {
		if (mWidth == 0) {
			mWidth = getWidth();
		}
	}

	public void snapToScreen(int whichScreen) {

		// get the valid layout page
		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		setMWidth();

		int scrollX = getScrollX();
		int startWidth = whichScreen * mWidth;

		if (scrollX != startWidth) {

			int delta = 0;
			int startX = 0;

			if (whichScreen > SHOWING_VIEW) {
				setPre();
				delta = startWidth - scrollX;
				startX = mWidth - startWidth + scrollX;

			} else if (whichScreen < SHOWING_VIEW) {
				setNext();
				delta = -scrollX;
				startX = scrollX + mWidth;
			} else {
				startX = scrollX;
				delta = startWidth - scrollX;

			}

			mScroller.startScroll(startX, 0, delta, 0, Math.abs(delta) * 2);
			invalidate(); // Redraw the layout
		}

		startCurrentView();

	}

	private void startCurrentView() {
		String viewTag = (String) getChildAt(SHOWING_VIEW).getTag();
		Message message = new Message();
		if (TextUtils.equals(viewTag, ComboViewActivityMine.BOOKMARK_INTENT_TAG)) {
			mContext.mLocalActivityManager.startActivity(ComboViewActivityMine.BOOKMARK_INTENT_TAG, new Intent(mContext, BrowserBookmarksPage.class));
			message.what = ComboViewActivityMine.BOOKMARK_VIEW;
			// message.what = Bookmarks;
		} else if (TextUtils.equals(viewTag, ComboViewActivityMine.HISTORY_INTENT_TAG)) {
			mContext.mLocalActivityManager.startActivity(ComboViewActivityMine.HISTORY_INTENT_TAG, new Intent(mContext, BrowserHistoryPage.class));
			message.what = ComboViewActivityMine.HISTORY_VIEW;
		} else {
			mContext.mLocalActivityManager.startActivity(ComboViewActivityMine.SNAPSHOT_INTENT_TAG, new Intent(mContext, BrowserSnapshotPage.class));
			message.what = ComboViewActivityMine.SNAPSHOT_VIEW;
		}

		mContext.mHandler.sendMessage(message);
	}

	public int getCurrentViewIndex() {
		String viewTag = (String) getChildAt(SHOWING_VIEW).getTag();
		if (TextUtils.equals(viewTag, ComboViewActivityMine.BOOKMARK_INTENT_TAG)) {
			return ComboViewActivityMine.BOOKMARK_VIEW;
			// message.what = Bookmarks;
		} else if (TextUtils.equals(viewTag, ComboViewActivityMine.HISTORY_INTENT_TAG)) {
			return ComboViewActivityMine.HISTORY_VIEW;
		} else {
			return ComboViewActivityMine.SNAPSHOT_VIEW;
		}
		// return ComboViewActivityMine.BOOKMARK_VIEW;;
	}
	
	public void setToScreen(int whichScreen) {

		whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
		//get width
		setMWidth();
		scrollTo(whichScreen  * mWidth, 0);

		if (whichScreen > SHOWING_VIEW) {
			setPre();
		} else if (whichScreen < SHOWING_VIEW) {
			setNext();
		}
	}

	public View getCurScreen() {
		return this.getChildAt(SHOWING_VIEW);
	}

	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) {		
			scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
			postInvalidate();
		} 
	}

	private void setNext() {
		int count = this.getChildCount();
		View view = getChildAt(count - 1);
		removeViewAt(count - 1);
		addView(view, 0);
	}

	private void setPre() {
		int count = this.getChildCount();
		View view = getChildAt(0);
		removeViewAt(0);
		addView(view, count - 1);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}

		mVelocityTracker.addMovement(event);
		final int action = event.getAction();
		final float x = event.getX();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			Log.d(TAG, "event down!");
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}
			mLastMotionX = x;
			break;

		case MotionEvent.ACTION_MOVE:
			int deltaX = (int) (mLastMotionX - x);
			mLastMotionX = x;
			scrollBy(deltaX, 0);
			break;

		case MotionEvent.ACTION_UP:

			// if (mTouchState == TOUCH_STATE_SCROLLING) {
			final VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000);
			int velocityX = (int) velocityTracker.getXVelocity();

			Log.d(TAG, "velocityX:" + velocityX + "; event : up");
			if (velocityX > SNAP_VELOCITY && SHOWING_VIEW > 0) {
				// Fling enough to move left
				Log.d(TAG, "snap left");
				snapToScreen(SHOWING_VIEW - 1);
			} else if (velocityX < -SNAP_VELOCITY
					&& SHOWING_VIEW < getChildCount() - 1) {
				// Fling enough to move right
				Log.d(TAG, "snap right");
				snapToScreen(SHOWING_VIEW + 1);
			} else {
				snapToDestination();
			}

			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}

			// }

			mTouchState = TOUCH_STATE_REST;
			break;

		case MotionEvent.ACTION_CANCEL:
			mTouchState = TOUCH_STATE_REST;
			break;
		}

		return true;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		Log.d(TAG, "onInterceptTouchEvent-slop:" + mTouchSlop);

		final int action = ev.getAction();

		if ((action == MotionEvent.ACTION_MOVE) &&

				(mTouchState != TOUCH_STATE_REST)) {

			return true;

		}

		final float x = ev.getX();

		switch (action) {

		case MotionEvent.ACTION_MOVE:

			final int xDiff = (int) Math.abs(mLastMotionX - x);

			if (xDiff > mTouchSlop) {

				mTouchState = TOUCH_STATE_SCROLLING;

			}

			break;

		case MotionEvent.ACTION_DOWN:

			mLastMotionX = x;

			mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST
					: TOUCH_STATE_SCROLLING;

			break;

		case MotionEvent.ACTION_CANCEL:

		case MotionEvent.ACTION_UP:

			mTouchState = TOUCH_STATE_REST;

			break;

		}

		return mTouchState != TOUCH_STATE_REST;

	}

	@Override
	protected void onAttachedToWindow() {

		Log.d("Windows", "onAttachedToWindow -- >" + getChildAt(SHOWING_VIEW).toString());

		startCurrentView();
		super.onAttachedToWindow();
	}

	@Override
	public void dispatchWindowFocusChanged(boolean hasFocus) {

		Log.d("Windows", "dispatchWindowFocusChanged -- >" + getChildAt(SHOWING_VIEW).toString());
		super.dispatchWindowFocusChanged(hasFocus);
	}

	@Override
	public void dispatchWindowVisibilityChanged(int visibility) {
		Log.d("Windows", "dispatchWindowVisibilityChanged -- >" + getChildAt(SHOWING_VIEW).toString());

		super.dispatchWindowVisibilityChanged(visibility);
	}

	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {

		Log.d("Windows", "onWindowFocusChanged -- >" + getChildAt(SHOWING_VIEW).toString());

		super.onWindowFocusChanged(hasWindowFocus);
	}

	@Override
	protected void onWindowVisibilityChanged(int visibility) {

		Log.d("Windows", "onWindowVisibilityChanged -- >" + getChildAt(SHOWING_VIEW).toString());

		super.onWindowVisibilityChanged(visibility);
	}

	@Override
	protected void onDetachedFromWindow() {

		Log.d("Windows", "onDetachedFromWindow -- >" + getChildAt(SHOWING_VIEW).toString());

		super.onDetachedFromWindow();
	}

}
