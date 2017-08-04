package com.android.launcher3.menu;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.android.launcher3.Launcher;

/**
 * 桌面底部展示的菜单详情
 *
 * Created by yuchuan on 02/08/2017.
 */

public class MenuDetail extends LinearLayout {

    private Context mCxt;
    private Launcher mLauncher;

    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    public MenuDetail(Context context) {
        super(context);
        init(context);
    }

    public MenuDetail(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MenuDetail(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public MenuDetail(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context){
        this.mCxt = context;
    }


}
