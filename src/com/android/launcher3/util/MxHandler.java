package com.android.launcher3.util;

import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Created by yuchuan on 8/16/16.
 */

public abstract class MxHandler<T> extends Handler {

    private WeakReference<T> weak;

    public MxHandler(T t) {
        this.weak = new WeakReference<T>(t);
    }

    @Override
    public void handleMessage(Message msg) {
        if (null == weak || null == weak.get()) {
            return;
        }
        handleMessage(msg, weak);
        super.handleMessage(msg);
    }

    protected abstract void handleMessage(Message msg, WeakReference<T> weak);
}
