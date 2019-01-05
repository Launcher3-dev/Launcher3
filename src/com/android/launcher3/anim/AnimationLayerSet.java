package com.android.launcher3.anim;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Helper class to automatically build view hardware layers for the duration of an animation.\
 *
 * add by codemx.cn  20181026
 */
public class AnimationLayerSet extends AnimatorListenerAdapter {

    private final HashMap<View, Integer> mViewsToLayerTypeMap;

    public AnimationLayerSet() {
        mViewsToLayerTypeMap = new HashMap<>();
    }

    public AnimationLayerSet(View v) {
        mViewsToLayerTypeMap = new HashMap<>(1);
        addView(v);
    }

    public void addView(View v) {
        mViewsToLayerTypeMap.put(v, v.getLayerType());
    }

    @Override
    public void onAnimationStart(Animator animation) {
        // Enable all necessary layers
        Iterator<Map.Entry<View, Integer>> itr = mViewsToLayerTypeMap.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<View, Integer> entry = itr.next();
            View v = entry.getKey();
            entry.setValue(v.getLayerType());
            v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            if (v.isAttachedToWindow() && v.getVisibility() == View.VISIBLE) {
                v.buildLayer();
            }
        }
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        Iterator<Map.Entry<View, Integer>> itr = mViewsToLayerTypeMap.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<View, Integer> entry = itr.next();
            entry.getKey().setLayerType(entry.getValue(), null);
        }
    }

}
