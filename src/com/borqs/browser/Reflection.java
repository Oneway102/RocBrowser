/**
 *  created by ChaoMeng.
 */

/**
 *  modified by ChaoMeng.
 */

package com.borqs.browser;

import org.apache.http.HttpHost;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebIconDatabase;
import android.webkit.WebIconDatabase.IconListener;
import android.widget.RemoteViews;

public class Reflection {

    public static String WebAddressGetHost(Object webAddress) {
    	if (webAddress == null) {
    		return null;
    	}

    	Object returnValue = null;
    	String getHostMethod = "getHost";
    	Object[] args = new Object[0];
    	try {
    		returnValue = ReflectionUtils.invokeMethod(webAddress, getHostMethod, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	if (returnValue != null && ReflectionUtils.isInstance(returnValue, String.class)) {
    		return (String)returnValue;
    	}
    	return null;
    }
    
    public static Object newInstaceWebAddress(String url) {
    	String className = "android.net.WebAddress";
    	Object[] args = new Object[1];
    	args[0] = url;

    	try {
			return ReflectionUtils.newInstance(className, args);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }
    
    public static void WebIconDatabaseBulkRequestIconForPageUrl(
    		WebIconDatabase webIconDatabase,
    		ContentResolver cr, String where,
            IconListener listener) {
    	
    	if (webIconDatabase == null) {
    		return;
    	}
    	
    	String methodName = "bulkRequestIconForPageUrl";
    	Object[] args = new Object[3];
    	args[0] = cr;
    	args[1] = where;
    	args[2] = listener;

    	try {
    		ReflectionUtils.invokeMethod(webIconDatabase, methodName, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    
    
    public static HttpHost ProxyGetPreferredHttpHost(Context context, String url) {
    	String className = "android.net.Proxy";
    	String methodName = "getPreferredHttpHost";
    	Object[] args = new Object[2];
    	args[0] = context;
    	args[1] = url;
    	
    	Object returnValue = null;
    	try {
    		returnValue = ReflectionUtils.invokeStaticMethod(className,methodName, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	if (returnValue != null && ReflectionUtils.isInstance(returnValue, HttpHost.class)) {
    		return (HttpHost)returnValue;
    	}
    	return null;
    }
    
    
    
    public static void MenuBuilderSetCurrentMenuInfo(ContextMenu menu, 
    		ContextMenuInfo contextMenuInfo) {
    	String className = "com.android.internal.view.menu.MenuBuilder";
    	String methodName = "setCurrentMenuInfo";
    	Object[] args = new Object[1];
    	args[0] = contextMenuInfo;
    	
    	try {
    		Object owner = ReflectionUtils.cast(menu, className);
    		ReflectionUtils.invokeMethod(owner, methodName, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
