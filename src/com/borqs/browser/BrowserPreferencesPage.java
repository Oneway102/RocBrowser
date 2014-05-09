/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.borqs.browser;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.MenuItem;

import com.borqs.browser.preferences.BandwidthPreferencesFragment;
import com.borqs.browser.preferences.DebugPreferencesFragment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.util.Log;

public class BrowserPreferencesPage extends PreferenceActivity {

    public static final String CURRENT_PAGE = "currentPage";
    private List<Header> mHeaders;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(
                    ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        }
    }

    /**
     * Populate the activity with the top-level headers.
     */
    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);

        if (BrowserSettings.getInstance().isDebugEnabled()) {
            Header debug = new Header();
            debug.title = getText(R.string.pref_development_title);
            debug.fragment = DebugPreferencesFragment.class.getName();
            target.add(debug);
        }
        mHeaders = target;
    }

    @Override
    public Header onGetInitialHeader() {
        String action = getIntent().getAction();
        if( action == null ) {
        	Log.i("BrowserPreferencesPage", "-------------------------------");
        } else {
        	Log.i("BrowserPreferencesPage", action);
        }
        if (Intent.ACTION_MANAGE_NETWORK_USAGE.equals(action)) {
            String fragName = BandwidthPreferencesFragment.class.getName();
            Log.i("BrowserPreferencesPage", fragName);
            for (Header h : mHeaders) {
                if (fragName.equals(h.fragment)) {
                    return h;
                }
            }
        }
        Log.i("BrowserPreferencesPage", "fragment: " + super.onGetInitialHeader().fragment);
        return super.onGetInitialHeader();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                } else {
                    finish();
                }
                return true;
        }

        return false;
    }

    @Override
    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args,
            int titleRes, int shortTitleRes) {
        Intent intent = super.onBuildStartFragmentIntent(fragmentName, args,
                titleRes, shortTitleRes);
        String url = getIntent().getStringExtra(CURRENT_PAGE);
        intent.putExtra(CURRENT_PAGE, url);
        return intent;
    }

    private static final Set<String> sKnownFragments = new HashSet<String>(Arrays.asList(
            "com.borqs.browser.preferences.GeneralPreferencesFragment",
            "com.borqs.browser.preferences.PrivacySecurityPreferencesFragment",
            "com.borqs.browser.preferences.AccessibilityPreferencesFragment",
            "com.borqs.browser.preferences.AdvancedPreferencesFragment",
            "com.borqs.browser.preferences.BandwidthPreferencesFragment",
            "com.borqs.browser.preferences.LabPreferencesFragment"));

    @Override
    protected boolean isValidFragment(String fragmentName) {
    	Log.i("BrowserPreferencesPage", fragmentName);
        return sKnownFragments.contains(fragmentName);
    }
}
