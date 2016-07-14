package com.mxlibrary.utils;

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by yuchuan
 * DATE 3/22/16
 * TIME 12:05
 */
public class UtilString {


    /**
     * 取字符串某个字符后的一串字符串，例如
     * http://8.37.229.100:7080/group1/M01/09/E1/pYYBAFUF2iKAdRaGAAATz4Q_8ts509.png
     * 传入/则得到pYYBAFUF2iKAdRaGAAATz4Q_8ts509.png
     *
     * @param value 字符串
     * @param mark  标记
     *
     * @return 标记后的字符串
     */
    public static String getLastMarkString(String value, String mark) {
        int position = value.lastIndexOf(mark);
        if (position < 0 || position >= (value.length() - 1)) {
            return value;
        }
        return value.substring(position + 1);
    }

    /**
     * 判断字符串是否为空
     * @param str
     * @return
     */
    public static boolean isEmpty(String str) {
        return !(str != null && !"".equals(str.trim()));
    }


    /**
     * 获取UTF-8编码的url
     *
     * @param str url
     *
     * @return
     */
    public static String urlEncode(String str) {
        if (str == null)
            return "";
        try {
            return URLEncoder.encode(str, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return " ";
    }


    /**
     * 去掉引号
     *
     * @param word 字符串
     *
     * @return
     */
    public static String removeDiagonal(String word) {
        if (TextUtils.isEmpty(word)) {
            return word;
        }
        if (word.endsWith("\"") || word.startsWith("\"")) {
            word = word.replaceAll("\"", "");
        }
        return word;
    }



}
