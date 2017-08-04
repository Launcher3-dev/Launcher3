package com.android.launcher3.menu;

import android.content.Context;

import com.android.launcher3.util.LauncherSpUtil;

/**
 * 菜单设置，可以更换顺序以及快捷方式，以及旋转位置。设置后保存。
 * <p>
 * Created by yuchuan on 02/08/2017.
 */

public class MenuSetting {

    private static final String KEY_MENU_START_ANGLE = "key_menu_start_angle";

    private Context mCxt;

    public MenuSetting(Context context) {
        this.mCxt = context;
    }

    public float getMenuStartAngle() {
        return LauncherSpUtil.getFloatData(mCxt, KEY_MENU_START_ANGLE, 0f);
    }

}
