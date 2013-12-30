package com.borqs.browser;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
//import android.webkit.SearchBox;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Class to manage the controlling of preloaded tab.
 */
public class PreloadedTabControl {
    private static final boolean LOGD_ENABLED = com.borqs.browser.Browser.LOGD_ENABLED;
    private static final String LOGTAG = "PreloadedTabControl";

    final Tab mTab;
    private String mLastQuery;
    private boolean mDestroyed;

    public PreloadedTabControl(Tab t) {
        if (LOGD_ENABLED) Log.d(LOGTAG, "PreloadedTabControl.<init>");
        mTab = t;
    }

    private void maybeSetQuery(final String query, SearchBox sb) {
        /* ww
        if (!TextUtils.equals(mLastQuery, query)) {
            if (sb != null) {
                if (LOGD_ENABLED) Log.d(LOGTAG, "Changing searchbox query to " + query);
                sb.setVerbatim(true);
                sb.setQuery(query);
                sb.onchange(new SearchBox.SearchBoxListener() {
                    @Override
                    public void onChangeComplete(boolean called) {
                        if (mDestroyed) return;
                        if (LOGD_ENABLED) Log.d(LOGTAG, "Changed searchbox query: " + called);
                        if (called) {
                            mLastQuery = query;
                        }
                    }
                });
            } else {
                if (LOGD_ENABLED) Log.d(LOGTAG, "Cannot set query: no searchbox interface");
            }
        }*/
    }

    public void setQuery(String query) {
// ww        maybeSetQuery(query, mTab.getWebViewClassic().getSearchBox());
    }

    public boolean searchBoxSubmit(final String query,
            final String fallbackUrl, final Map<String, String> fallbackHeaders) {
        /* ww
        final SearchBox sb = mTab.getWebViewClassic().getSearchBox();
        if (sb == null) {
            // no searchbox, cannot submit. Fallback to regular tab creation
            if (LOGD_ENABLED) Log.d(LOGTAG, "No searchbox, cannot submit query");
            return false;
        }
        maybeSetQuery(query, sb);
        if (LOGD_ENABLED) Log.d(LOGTAG, "Submitting query " + query);
        final String currentUrl = mTab.getUrl();
        sb.onsubmit(new SearchBox.SearchBoxListener() {
            @Override
            public void onSubmitComplete(boolean called) {
                if (mDestroyed) return;
                if (LOGD_ENABLED) Log.d(LOGTAG, "Query submitted: " + called);
                if (!called) {
                    if (LOGD_ENABLED) Log.d(LOGTAG, "Query not submitted; falling back");
                    loadUrl(fallbackUrl, fallbackHeaders);
                    // make sure that the failed, preloaded URL is cleared from the back stack
                    mTab.clearBackStackWhenItemAdded(Pattern.compile(
                            "^" + Pattern.quote(fallbackUrl) + "$"));
                } else {
                    // ignore the next fragment change, to avoid leaving a blank page in the browser
                    // after the query has been submitted.
                    String currentWithoutFragment = Uri.parse(currentUrl)
                            .buildUpon()
                            .fragment(null)
                            .toString();
                    mTab.clearBackStackWhenItemAdded(
                            Pattern.compile(
                                    "^" +
                                    Pattern.quote(currentWithoutFragment) +
                                    "(\\#.*)?" +
                                    "$"));
                }
            }});*/
        return true;
    }

    public void searchBoxCancel() {
        /* ww
        SearchBox sb = mTab.getWebViewClassic().getSearchBox();
        if (sb != null) {
            mLastQuery = null;
            sb.oncancel(new SearchBox.SearchBoxListener(){
                @Override
                public void onCancelComplete(boolean called) {
                    if (LOGD_ENABLED) Log.d(LOGTAG, "Query cancelled: " + called);
                }
            });
        }*/
    }

    public void loadUrlIfChanged(String url, Map<String, String> headers) {
        String currentUrl = mTab.getUrl();
        if (!TextUtils.isEmpty(currentUrl)) {
            try {
                // remove fragment:
                currentUrl = Uri.parse(currentUrl).buildUpon().fragment(null).build().toString();
            } catch (UnsupportedOperationException e) {
                // carry on
            }
        }
        if (LOGD_ENABLED) Log.d(LOGTAG, "loadUrlIfChanged\nnew: " + url + "\nold: " +currentUrl);
        if (!TextUtils.equals(url, currentUrl)) {
            loadUrl(url, headers);
        }
    }

    public void loadUrl(String url, Map<String, String> headers) {
        if (LOGD_ENABLED) Log.d(LOGTAG, "Preloading " + url);
        mTab.loadUrl(url, headers);
    }

    public void destroy() {
        if (LOGD_ENABLED) Log.d(LOGTAG, "PreloadedTabControl.destroy");
        mDestroyed = true;
        mTab.destroy();
    }

    public Tab getTab() {
        return mTab;
    }

    public class SearchBox {
    }
}