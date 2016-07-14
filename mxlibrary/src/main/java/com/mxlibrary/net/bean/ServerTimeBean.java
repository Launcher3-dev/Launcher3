package com.mxlibrary.net.bean;

/**
 * Created by lijia on 2016/5/5.
 */
public class ServerTimeBean {
    public String getIsExist() {
        return isExist;
    }

    public void setIsExist(String isExist) {
        this.isExist = isExist;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "ServerTimeBean{" +
                "isExist='" + isExist + '\'' +
                ", code='" + code + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    public  String isExist;
    public  String code;
    public  String value;

}
