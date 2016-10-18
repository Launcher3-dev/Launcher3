package com.mxlibrary.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.mxlibrary.utils.XLog;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yuchuan
 * DATE 5/26/16
 * TIME 09:12
 */
public abstract class BaseDialogUtils<T extends Dialog> {

    private T mDialog;
    private long mTimeOut = 0;// 默认timeOut为0即无限大
    private Timer mTimer = null;// 定时器
    private OnTimeOutListener mTimeOutListener = null;// timeOut后的处理器
    private Handler mHandler = new DialogHandler(this);

    private static class DialogHandler extends Handler {
        WeakReference<BaseDialogUtils> mUtils;

        DialogHandler(BaseDialogUtils utils) {
            this.mUtils = new WeakReference<BaseDialogUtils>(utils);
        }

        @Override
        public void handleMessage(Message msg) {
            BaseDialogUtils util = mUtils.get();
            if (util.mTimeOutListener != null && util.mDialog != null) {
                util.mTimeOutListener.onTimeOut(util);
                util.dismiss();
            }
        }
    }

    protected abstract T getDialog(Context context, BaseDialogUtils baseDialogUtils);

    protected abstract boolean isCanceledOnTouchOutside();

    protected abstract int getStyleId();

    public void dismiss() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    /**
     * 设置timeOut长度，和处理器
     *
     * @param t               timeout时间长度
     * @param timeOutListener 超时后的处理器
     */
    public void setTimeOut(long t, OnTimeOutListener timeOutListener) {
        mTimeOut = t;
        if (timeOutListener != null) {
            this.mTimeOutListener = timeOutListener;
        }
    }

    /**
     * 通过静态Create的方式创建一个实例对象
     *
     * @param context
     * @param time            超时时间长度
     * @param timeOutListener timeOutListener 超时后的处理器
     *
     * @return LtpDialog 对象
     */
    public T createProgressDialog(Context context, long time,
                                  OnTimeOutListener timeOutListener) {
        mDialog = getDialog(context, BaseDialogUtils.this);
        if (mDialog == null) {
            throw new IllegalArgumentException("Your dialog is null");
        }
        // 设置点击进度对话框外的区域对话框不消失
        mDialog.setCanceledOnTouchOutside(isCanceledOnTouchOutside());
        mDialog.show();
        setTimeOutListener(time, timeOutListener);
        return mDialog;
    }

    /**
     * 创建dialog
     *
     * @param context         上下文
     * @param title           dialog标题
     * @param message         dialog内容
     * @param time            超时时间
     * @param timeOutListener 超时监听
     *
     * @return
     */
    public T createProgressDialog(Context context, String title,
                                  String message, long time, OnTimeOutListener timeOutListener) {
        XLog.e(XLog.getTag(), "---");
        dismiss();
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        mDialog = getDialog(context, BaseDialogUtils.this);
        if (mDialog == null) {
            return null;
        }
        // 设置点击进度对话框外的区域对话框不消失
        mDialog.setCanceledOnTouchOutside(isCanceledOnTouchOutside());
        mDialog.show();
        setTimeOutListener(time, timeOutListener);
        return mDialog;
    }

    private void setTimeOutListener(long time, OnTimeOutListener timeOutListener) {
        if (time > 0) {
            setTimeOut(time, timeOutListener);
            mTimer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    Message msg = mHandler.obtainMessage();
                    mHandler.sendMessage(msg);
                }
            };
            mTimer.schedule(timerTask, mTimeOut);
        }
    }

    //处理超时的的接口
    public interface OnTimeOutListener {
        // 当progressDialog超时时调用此方法
        void onTimeOut(BaseDialogUtils dialog);
    }
}
