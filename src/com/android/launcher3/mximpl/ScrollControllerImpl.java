package com.android.launcher3.mximpl;

/**
 * 二级界面和菜单的控制接口
 *
 * Created by yuchuan on 2018/4/14.
 */

public interface ScrollControllerImpl {

    void updateCaret(float containerProgress, float velocity, boolean dragging);

    void animateCaretToProgress(float progress);

    float getThreshold();

    void onDragStart();

}