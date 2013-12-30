package com.borqs.browser;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;

public interface ActivityController {
 
    void start(Intent intent);

    void onSaveInstanceState(Bundle outState);

    void handleNewIntent(Intent intent);

    void onResume();

    boolean onMenuOpened(int featureId, Menu menu);

    void onOptionsMenuClosed(Menu menu);

    void onContextMenuClosed(Menu menu);

    void onPause();

    void onDestroy();
    
}