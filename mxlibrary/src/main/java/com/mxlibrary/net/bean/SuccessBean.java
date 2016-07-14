package com.mxlibrary.net.bean;


/**
 * Created by yuchuan
 * DATE 16/4/2
 * TIME 15:05
 */
public class SuccessBean {

    public String errorMessage;
    public int errorCode;

    @Override
    public String toString() {
        return "SuccessBean{" +
                "errorMessage='" + errorMessage + '\'' +
                ", errorCode='" + errorCode + '\'' +
                '}';
    }


}
