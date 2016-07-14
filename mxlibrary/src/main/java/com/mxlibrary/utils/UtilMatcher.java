package com.mxlibrary.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则表达式
 * <p>
 * Created by yuchuan
 * DATE 3/24/16
 * TIME 16:01
 */
public class UtilMatcher {

    /**
     * 判断传入的是否都是数字
     *
     * @param str 传入字符串
     *
     * @return
     */
    public static boolean isNumeric(String str) {
        Pattern pattern = Pattern.compile("[0-9]*");
        Matcher isNum = pattern.matcher(str);
        return isNum.matches();
    }

    /**
     * 是否是中文
     *
     * @param str 字符串
     *
     * @return
     */
    public static boolean isChineseChar(String str) {
        boolean temp = false;
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(str);
        if (m.find()) {
            temp = true;
        }
        return temp;
    }

    /**
     * 是否包含特殊字符
     *
     * @param str   字符串
     * @param empty 是否去掉空位置
     *
     * @return
     */
    public static String containsSpecialString(String str, boolean empty) {
        // String regEx = "[^a-zA-Z0-9]";
        // 清除掉所有特殊字符
        String regEx = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        if (empty) {
            m.replaceAll("").trim();
        }
        return m.replaceAll("uc").trim();
    }
}
