

package com.borqs.browser.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;


public class CustomScreenLinearLayout extends LinearLayout {

    public CustomScreenLinearLayout(Context context) {
        super(context);
        setChildrenDrawingOrderEnabled(true);
    }

    public CustomScreenLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setChildrenDrawingOrderEnabled(true);
    }

    public CustomScreenLinearLayout(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
        setChildrenDrawingOrderEnabled(true);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        return childCount - i - 1;
    }

}
