package com.mxlibrary.net.bean;

/**
 * Created by lijia on 2016/4/26.
 */
public class InformationBean {
    //客户端时间
    private String clientTime;
    //IMEI号
    private String imeiNum;
    //国家识别码
    private String countryCode;
    //MAC地址
    private String macAddr;
    //设备ID
    private String deviceId;
    //联网方式
    private String onlineType;
    //手机品牌名
    private String modelNum;
    //设备类型pad phone
    private String deviceType;
    //手机系统版本
    private String osVersion;
    //GPS地址
    private String gpsLoc;
    //服务运营商
    private String mobileVendo;
    //电话号码
    private String phoneNum;
    //屏幕分辨率
    private String resolution;
    //当前使用语言
    private String language;
    //APK版本
    private String apkVersion;
    //应用名字
    private String apkName;
    //版本名称
    private String versionName;



    public String getClientTime() {
        return clientTime;
    }

    public void setClientTime(String clientTime) {
        this.clientTime = clientTime;
    }

    public String getImeiNum() {
        return imeiNum;
    }

    public void setImeiNum(String imeiNum) {
        this.imeiNum = imeiNum;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getMacAddr() {
        return macAddr;
    }

    public void setMacAddr(String macAddr) {
        this.macAddr = macAddr;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getOnlineType() {
        return onlineType;
    }

    public void setOnlineType(String onlineType) {
        this.onlineType = onlineType;
    }

    public String getModelNum() {
        return modelNum;
    }

    public void setModelNum(String modelNum) {
        this.modelNum = modelNum;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    public String getGpsLoc() {
        return gpsLoc;
    }

    public void setGpsLoc(String gpsLoc) {
        this.gpsLoc = gpsLoc;
    }

    public String getMobileVendo() {
        return mobileVendo;
    }

    public void setMobileVendo(String mobileVendo) {
        this.mobileVendo = mobileVendo;
    }

    public String getPhoneNum() {
        return phoneNum;
    }

    public void setPhoneNum(String phoneNum) {
        this.phoneNum = phoneNum;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getApkVersion() {
        return apkVersion;
    }

    public void setApkVersion(String apkVersion) {
        this.apkVersion = apkVersion;
    }

    public String getApkName() {
        return apkName;
    }

    public void setApkName(String apkName) {
        this.apkName = apkName;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    @Override
    public String toString() {
        return "InformationBean{" +
                "clientTime='" + clientTime + '\'' +
                ", imeiNum='" + imeiNum + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", macAddr='" + macAddr + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", onlineType='" + onlineType + '\'' +
                ", modelNum='" + modelNum + '\'' +
                ", deviceType='" + deviceType + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", gpsLoc='" + gpsLoc + '\'' +
                ", mobileVendo='" + mobileVendo + '\'' +
                ", phoneNum='" + phoneNum + '\'' +
                ", resolution='" + resolution + '\'' +
                ", language='" + language + '\'' +
                ", apkVersion='" + apkVersion + '\'' +
                ", apkName='" + apkName + '\'' +
                ", versionName='" + versionName + '\'' +
                '}';
    }
}
