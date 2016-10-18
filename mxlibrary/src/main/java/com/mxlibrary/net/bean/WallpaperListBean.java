package com.mxlibrary.net.bean;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by yuchuan
 * DATE 5/12/16
 * TIME 15:18
 */
public class WallpaperListBean {

    /**
     * id : 3
     * author : 超级管理员
     * price : 0
     * description : 壁纸1上传
     * name : ,壁纸1
     * type : 1
     * coverImg : /wallpaper/2016/5/11/f7405183d97845d0968c3da0c3455988.jpg
     * previewImg : /wallpaper/2016/5/11/9ca07f621c534b2db27943f03d89739e.jpg
     */

    private int code;
    private List<WallpaperBean> wallpaperList;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public List<WallpaperBean> getWallpaperList() {
        return wallpaperList;
    }

    public void setWallpaperList(List<WallpaperBean> wallpaperList) {
        this.wallpaperList = wallpaperList;
    }

    public static class WallpaperBean {
        public static final int OTHER_WALL_ID = -100;
        public static final int TYPE_LOCAL_DB = 0;//下载
        public static final int TYPE_LOCAL_OTHER = 1;//其他
        public static final int TYPE_LOCAL_INNER = 2;//内置
        public static final int TYPE_LOCAL_DEFAULT = 3;//内置默认
        public static final int TYPE_ONLINE = 5;//在线

        private int localType = TYPE_LOCAL_DB;
        private int smallId = 0;
        private int bigId = 0;
        private long id;
        private String author;
        private double price;
        private String description;
        private String name;
        private int type;
        @SerializedName("cover_img")
        private String coverImg;//缩略图
        @SerializedName("preview_img")
        private String previewImg;//大图

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getType() {
            return type;
        }

        public void setType(int type) {
            this.type = type;
        }

        public String getCoverImg() {
            return coverImg;
        }

        public void setCoverImg(String coverImg) {
            this.coverImg = coverImg;
        }

        public String getPreviewImg() {
            return previewImg;
        }

        public void setPreviewImg(String previewImg) {
            this.previewImg = previewImg;
        }

        public void setSmallId(int smallId) {
            this.smallId = smallId;
        }
        public void setBigId(int bigId) {
            this.bigId = bigId;
        }
        public int getSmallId() {
            return smallId;
        }
        public int getBigId() {
            return bigId;
        }
        public int getLocalType() {
            return localType;
        }
        public void setLocalType(int localType) {
            this.localType = localType;
        }

        @Override
        public String toString() {
            return "WallpaperBean{" +
                    "localType=" + localType +
                    ", smallId=" + smallId +
                    ", bigId=" + bigId +
                    ", id=" + id +
                    ", author='" + author + '\'' +
                    ", price=" + price +
                    ", description='" + description + '\'' +
                    ", name='" + name + '\'' +
                    ", type=" + type +
                    ", coverImg='" + coverImg + '\'' +
                    ", previewImg='" + previewImg + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "WallpaperListBean{" +
                "code=" + code +
                ", wallpaperList=" + wallpaperList +
                '}';
    }
}
