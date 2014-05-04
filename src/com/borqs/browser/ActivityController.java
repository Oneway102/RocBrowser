package com.borqs.browser;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public interface ActivityController {
 
    void start(Intent intent);

    void onSaveInstanceState(Bundle outState);

    void handleNewIntent(Intent intent);

    void onResume();

    boolean onMenuOpened(int featureId, Menu menu);

    void onOptionsMenuClosed(Menu menu);

    void onContextMenuClosed(Menu menu);

    boolean onCreateOptionsMenu(Menu menu);

    boolean onPrepareOptionsMenu(Menu menu);

    boolean onOptionsItemSelected(MenuItem item);

    void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo);

    boolean onContextItemSelected(MenuItem item);

    void onPause();

    void onDestroy();
    
    boolean onKeyDown(int keyCode, KeyEvent event);

    boolean onKeyLongPress(int keyCode, KeyEvent event);

    boolean onKeyUp(int keyCode, KeyEvent event);
}