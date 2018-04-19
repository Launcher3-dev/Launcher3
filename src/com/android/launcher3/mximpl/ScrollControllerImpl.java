package com.android.launcher3.mximpl;

import com.android.launcher3.allapps.SearchUiManager;
import com.android.launcher3.touch.SwipeDetector;
import com.android.launcher3.util.TouchController;

/**
 * Created by yuchuan on 2018/4/14.
 */

public interface ScrollControllerImpl extends TouchController, SwipeDetector.Listener,
        SearchUiManager.OnScrollRangeChangeListener{

    boolean isTransitioning();

}
