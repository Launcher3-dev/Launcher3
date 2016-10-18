package com.mxlibrary.net.bean;


import java.io.Serializable;

/**
 * Created by Administrator on 2016/4/7.
 */
public class UpdataInfo implements Serializable {
    private String isUpgrade;//版本号,对应软件的versionCode
    private String versionCode;//版本更新描述
    private String versionName;//apk下载url
    private String note;
    private String pathName;

    public String getIsUpgrade() {
        return isUpgrade;
    }

    public void setIsUpgrade(String isUpgrade) {
        this.isUpgrade = isUpgrade;
    }

    public String getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getPathName() {
        return pathName;
    }

    public void setPathName(String pathName) {
        this.pathName = pathName;
    }

    @Override
    public String toString() {
        return "UpdataInfo{" +
                "isUpgrade='" + isUpgrade + '\'' +
                ", versionCode='" + versionCode + '\'' +
                ", versionName='" + versionName + '\'' +
                ", note='" + note + '\'' +
                ", pathName='" + pathName + '\'' +
                '}';
    }


}
