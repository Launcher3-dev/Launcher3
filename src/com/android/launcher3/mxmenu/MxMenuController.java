package com.android.launcher3.mxmenu;

import android.view.MotionEvent;

import com.android.launcher3.allapps.SearchUiManager;
import com.android.launcher3.touch.SwipeDetector;
import com.android.launcher3.util.TouchController;

/**
 * Created by yuchuan on 2018/4/14.
 */

public class MxMenuController implements TouchController, SwipeDetector.Listener,
        SearchUiManager.OnScrollRangeChangeListener {


    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public void onScrollRangeChanged(int scrollRange) {

    }

    @Override
    public void onDragStart(boolean start) {

    }

    @Override
    public boolean onDrag(float displacement, float velocity) {
        return false;
    }

    @Override
    public void onDragEnd(float velocity, boolean fling) {

    }
}
