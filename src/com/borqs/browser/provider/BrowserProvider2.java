package com.borqs.browser.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
//import android.provider.BrowserContract;

public class BrowserProvider2 extends SQLiteContentProvider {

    public static interface OmniboxSuggestions {
//        public static final Uri CONTENT_URI = Uri.withAppendedPath(
//                BrowserContract.AUTHORITY_URI, "omnibox_suggestions");
        public static final String _ID = "_id";
        public static final String URL = "url";
        public static final String TITLE = "title";
        public static final String IS_BOOKMARK = "bookmark";
    }

}