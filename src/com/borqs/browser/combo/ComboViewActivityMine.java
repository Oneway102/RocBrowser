/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 *  modified by ChHYin.
 */

package com.borqs.browser.combo;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import com.borqs.browser.BrowserPreferencesPage;
import com.borqs.browser.CombinedBookmarksCallbacks;
import com.borqs.browser.Controller;
import com.borqs.browser.R;
import com.borqs.browser.UI.ComboViews;

import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnClickListener;

public class ComboViewActivityMine  extends ActivityGroup implements 
OnClickListener, OnGestureListener, CombinedBookmarksCallbacks {

	private static final String STATE_SELECTED_TAB = "tab";
	public static final String EXTRA_COMBO_ARGS = "combo_args";
	public static final String EXTRA_INITIAL_VIEW = "initial_view";

	public static final String EXTRA_OPEN_SNAPSHOT = "snapshot_id";
	public static final String EXTRA_OPEN_ALL = "open_all";
	public static final String EXTRA_CURRENT_URL = "url";


	public static final String BOOKMARK_INTENT_TAG = "bookmark";
	public static final String HISTORY_INTENT_TAG = "history";
	public static final String SNAPSHOT_INTENT_TAG = "third";
	public static final int HISTORY_VIEW = 0;
	public static final int BOOKMARK_VIEW = 1;
	public static final int SNAPSHOT_VIEW = 2;

	ScrollLayout mRoot;
	private View mBookmarkView, mHistoryView, mSnapshotView;

	private ImageButton mBookmarkButton;
	private ImageButton mHistoryButton;
	private ImageButton mSnapshotButton;

	/**
	 * This field should be made private, so it is hidden from the SDK.
	 * {@hide}
	 */
	protected LocalActivityManager mLocalActivityManager;

	public Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {

			super.handleMessage(msg);
			switch (msg.what) {
			case BOOKMARK_VIEW:
				mBookmarkButton.setSelected(true);
				mHistoryButton.setSelected(false);
				mSnapshotButton.setSelected(false);
				break;
			case HISTORY_VIEW:
				mBookmarkButton.setSelected(false);
				mHistoryButton.setSelected(true);
				mSnapshotButton.setSelected(false);
				break;

			case SNAPSHOT_VIEW:
				mBookmarkButton.setSelected(false);
				mHistoryButton.setSelected(false);
				mSnapshotButton.setSelected(true);
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// this.getWindow().addFlags(Window.FEATURE_ACTION_BAR);

		setResult(RESULT_CANCELED);
		Bundle extras = getIntent().getExtras();
		Bundle args = extras.getBundle(EXTRA_COMBO_ARGS);
		String svStr = extras.getString(EXTRA_INITIAL_VIEW, null);
		ComboViews startingView = svStr != null
				? ComboViews.valueOf(svStr)
						: ComboViews.Bookmarks;

				mLocalActivityManager = getLocalActivityManager();
				setContentView(R.layout.combo_view);
				mRoot = (ScrollLayout) findViewById(R.id.combo_root);
				mBookmarkButton = (ImageButton) findViewById(R.id.bookmark_button);
				mHistoryButton = (ImageButton) findViewById(R.id.history_button);
				mSnapshotButton = (ImageButton) findViewById(R.id.snapshot_button);
				mBookmarkButton.setOnClickListener(this);
				mHistoryButton.setOnClickListener(this);
				mSnapshotButton.setOnClickListener(this);

				int lastView = 0;

				if (savedInstanceState != null) {
					lastView = savedInstanceState.getInt(STATE_SELECTED_TAB, 0);
				} else {
					switch (startingView) {
					case Bookmarks:
						lastView = BOOKMARK_VIEW;
						break;
					case History:
						lastView = HISTORY_VIEW;
						break;
					case Snapshots:
						lastView = SNAPSHOT_VIEW;
						break;
					}
				}

				initView(lastView);
	}



	public void initView(int lastView) {

		mRoot.removeAllViews();

		Intent firstIntent = new Intent(this, BrowserHistoryPage.class);
		mHistoryView = activityToView(this, firstIntent, HISTORY_INTENT_TAG);
		mHistoryView.setTag(HISTORY_INTENT_TAG);
		mRoot.addView(mHistoryView);

		Intent secondIntent = new Intent(this, BrowserBookmarksPage.class);
		mBookmarkView = activityToView(this, secondIntent, BOOKMARK_INTENT_TAG);
		mBookmarkView.setTag(BOOKMARK_INTENT_TAG);
		mRoot.addView(mBookmarkView);

		Intent thirdIntent = new Intent(this, BrowserSnapshotPage.class);
		mSnapshotView = activityToView(this, thirdIntent, SNAPSHOT_INTENT_TAG);
		mSnapshotView.setTag(SNAPSHOT_INTENT_TAG);
		mRoot.addView(mSnapshotView);
	}

	public View activityToView(Context parent, Intent intent, String tag){
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		Window w = mLocalActivityManager.startActivity(tag, intent);

		View wd = w != null ? w.getDecorView() : null;
		if (wd != null) {
			wd .setVisibility(View.VISIBLE);
			wd .setFocusableInTouchMode(true);
			((ViewGroup) wd ).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
		}
		return wd ;
	}



	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(STATE_SELECTED_TAB, mRoot.getCurrentViewIndex());
	}

	@Override
	public void openUrl(String url) {
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public void openInNewTab(String... urls) {
		Intent i = new Intent();
		i.putExtra(EXTRA_OPEN_ALL, urls);
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public void close() {
		finish();
	}

	// @Override
	public void openSnapshot(long id) {
		Intent i = new Intent();
		i.putExtra(EXTRA_OPEN_SNAPSHOT, id);
		setResult(RESULT_OK, i);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.combined, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		} else if (item.getItemId() == R.id.preferences_menu_id) {
			
			String url = getIntent().getStringExtra(EXTRA_CURRENT_URL);
			Intent intent = new Intent(this, BrowserPreferencesPage.class);
			intent.putExtra(BrowserPreferencesPage.CURRENT_PAGE, url);
			startActivityForResult(intent, Controller.PREFERENCES_PAGE);
			 
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	private static String makeFragmentName(int viewId, int index) {
		return "android:switcher:" + viewId + ":" + index;
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
	}

	@Override
	public boolean onDown(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onClick(View v) {
		int index;
		switch (v.getId()) {
		case R.id.bookmark_button:
			index = mRoot.indexOfChild(mBookmarkView);
			Log.d("onClick", "index = " + index + "; first = " + mRoot.getChildAt(0).getTag());
			mRoot.setToScreen(index);
			break;
		case R.id.history_button:
			index = mRoot.indexOfChild(mHistoryView);
			Log.d("onClick", "index = " + index + "; second = " + mRoot.getChildAt(1).getTag());
			mRoot.setToScreen(index);
			break;
		case R.id.snapshot_button:
			index = mRoot.indexOfChild(mSnapshotView);
			Log.d("onClick", "index = " + index + "; third = " + mRoot.getChildAt(2).getTag());
			mRoot.setToScreen(index);
			break;
		default:
			break;
		}
	}



	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
		super.onActivityResult(requestCode, resultCode, data);

		// Activity currentActivity = getLocalActivityManager().getCurrentActivity();  
		// currentActivity.handleActivityResult(requestCode, resultCode, data);
	}  
}