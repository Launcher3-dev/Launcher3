package com.android.launcher3.menu;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.Launcher;

/**
 * 主菜单，八卦形状，上滑时弹出，到屏幕中间展开，可以自由更换位置，旋转圆盘并保存位置
 * <p>
 * Created by yuchuan on 02/08/2017.
 */

public class MenuMain extends ViewGroup {

    private Context mCxt;
    private Launcher mLauncher;
    private boolean isShow = false;
    // 是否展开
    private boolean mIsExpand = false;

    // 圆盘旋转角度，默认是0
    private float mMenuStartAngle = 0;

    private MenuSetting mMenuSetting;

    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    public MenuMain(Context context) {
        super(context);
        init(context);
    }

    public MenuMain(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MenuMain(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public MenuMain(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        this.mCxt = context;
        this.mMenuSetting = new MenuSetting(context);
        this.mMenuStartAngle = mMenuSetting.getMenuStartAngle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }

    public void show() {
        showOrHide(true);
    }

    public void hide() {
        showOrHide(false);
    }

    private ObjectAnimator mAnimatorShowOrHide;

    private void showOrHide(boolean show) {
        if (mAnimatorShowOrHide != null && mAnimatorShowOrHide.isRunning()) {
            return;
        }
        float start = 0;
        float end = 0;
        if (show) {
            end = -(getHeight() + mLauncher.getResources().getDisplayMetrics().heightPixels / 2);
        } else {
            start = -(getHeight() + mLauncher.getResources().getDisplayMetrics().heightPixels / 2);
        }

        mAnimatorShowOrHide = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, start, end);
        mAnimatorShowOrHide.setDuration(500);
        mAnimatorShowOrHide.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isShow = !isShow;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mAnimatorShowOrHide.start();
    }

    public boolean isShow() {
        return isShow;
    }

    public void setShow(boolean show) {
        isShow = show;
    }
}
