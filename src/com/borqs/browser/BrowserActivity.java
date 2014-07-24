
package com.borqs.browser;

import org.chromium.base.ActivityStatus;
import org.chromium.content.app.LibraryLoader;
import org.chromium.content.browser.AndroidBrowserProcess;
import org.chromium.content.browser.DeviceUtils;
import org.chromium.content.common.CommandLine;
import org.chromium.content.common.ProcessInitException;
import org.chromium.ui.WindowAndroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;


public class BrowserActivity extends Activity {

    public static final String COMMAND_LINE_FILE = "/data/local/tmp/content-shell-command-line";
    public static final String COMMAND_LINE_ARGS_KEY = "commandLineArgs";

    public static final String ACTION_SHOW_BOOKMARKS = "show_bookmarks";
    public static final String ACTION_SHOW_BROWSER = "show_browser";
    public static final String ACTION_RESTART = "--restart--";
    private static final String EXTRA_STATE = "state";

    private final static String LOGTAG = "browser";

    private final static boolean LOGV_ENABLED = Browser.LOGV_ENABLED;

    private ActivityController mController;

    private WindowAndroid mWindow;
    
    
    @Override
    public void onCreate(Bundle icicle) {
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, this + " onStart, has state: "
                    + (icicle == null ? "false" : "true"));
        }
        super.onCreate(icicle);

        if (shouldIgnoreIntents()) {
            finish();
            return;
        }

        // Initializing the command line must occur before loading the library.
        if (!CommandLine.isInitialized()) {
            CommandLine.initFromFile(COMMAND_LINE_FILE);
            String[] commandLineParams = getCommandLineParamsFromIntent(getIntent());
            if (commandLineParams != null) {
                CommandLine.getInstance().appendSwitchesAndArguments(commandLineParams);
            }
        }
        waitForDebuggerIfNeeded();

        DeviceUtils.addDeviceSpecificUserAgentSwitch(this);
        try {
            LibraryLoader.ensureInitialized();
            
            if (!AndroidBrowserProcess.init(this, AndroidBrowserProcess.MAX_RENDERERS_AUTOMATIC)) {
                Log.e(LOGTAG, "Already running!");
            }
        } catch (ProcessInitException e) {
            Log.e(LOGTAG, "ContentView initialization failed.", e);
            finish();
        }

        mWindow = new WindowAndroid(this);
        mWindow.restoreInstanceState(icicle);
        
        // If this was a web search request, pass it on to the default web
        // search provider and finish this activity.
        if (IntentHandler.handleWebSearchIntent(this, null, getIntent())) {
            finish();
            return;
        }
        mController = createController();

        Intent intent = (icicle == null) ? getIntent() : null;
        mController.start(intent);
    }

    private void waitForDebuggerIfNeeded() {
        if (CommandLine.getInstance().hasSwitch(CommandLine.WAIT_FOR_JAVA_DEBUGGER)) {
            Log.e(LOGTAG, "Waiting for Java debugger to connect...");
            android.os.Debug.waitForDebugger();
            Log.e(LOGTAG, "Java debugger connected. Resuming execution.");
        }
    }

    private static String[] getCommandLineParamsFromIntent(Intent intent) {
        return intent != null ? intent.getStringArrayExtra(COMMAND_LINE_ARGS_KEY) : null;
    }

    public static boolean isTablet(Context context) {
        //return context.getResources().getBoolean(R.bool.isTablet);
        return false;
    }

    private Controller createController() {
        Controller controller = new Controller(this, this.mWindow);
        boolean xlarge = isTablet(this);
        UI ui = null;
        if (xlarge) {
            ui = new XLargeUi(this, controller);
        } else {
            ui = new PhoneUi(this, controller);
        }
        controller.setUi(ui);
        return controller;
    }

    private boolean shouldIgnoreIntents() {
        return false;
    }

    ///////////////
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mController.onKeyDown(keyCode, event) ||
            super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return mController.onKeyLongPress(keyCode, event) ||
            super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return mController.onKeyUp(keyCode, event) ||
            super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return mController.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        return mController.onPrepareOptionsMenu(menu);
    }


    /**
     *  onSaveInstanceState(Bundle map)
     *  onSaveInstanceState is called right before onStop(). The map contains
     *  the saved state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "BrowserActivity.onSaveInstanceState: this=" + this);
        }
        mController.onSaveInstanceState(outState);
        mWindow.saveInstanceState(outState);
    }
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        // mController.onActivityResult(requestCode, resultCode, intent);
        mWindow.onActivityResult(requestCode, resultCode, intent);
    }
 

    @Override
    protected void onPause() {
        mController.onPause();
        ActivityStatus.onStateChange(this, ActivityStatus.PAUSED);
        // ((ChromeMobileApplication)getApplicationContext()).setChromeInForeground(false);
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (LOGV_ENABLED) {
            Log.v(LOGTAG, "BrowserActivity.onResume: this=" + this);
        }
        mController.onResume();
        
        ActivityStatus.onStateChange(this, ActivityStatus.RESUMED);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mController.onOptionsItemSelected(item)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
