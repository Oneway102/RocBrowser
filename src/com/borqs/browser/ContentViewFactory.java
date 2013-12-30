package com.borqs.browser;

import org.chromium.content.browser.ContentView;

/**
 * Factory for WebViews
 */
public interface ContentViewFactory {

    public ContentView createContentView(boolean privateBrowsing);

    public ContentView createSubContentView(boolean privateBrowsing);

}