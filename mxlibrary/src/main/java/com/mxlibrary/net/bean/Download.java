package com.mxlibrary.net.bean;

/**
 * Created by Administrator on 2016/4/12.
 */
public class Download {

    public float getVersionSize() {
        return VersionSize;
    }

    public void setVersionSize(float versionSize) {
        VersionSize = versionSize;
    }

    private float VersionSize;

    public int downloadProgress;

    public boolean isDownloadcompleted()
    {
        return isDownloadcompleted;
    }

    public void setDownloadcompleted(boolean downloadcompleted) {
        isDownloadcompleted = downloadcompleted;
    }

    public int getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(int downloadProgress) {
        this.downloadProgress = downloadProgress;
    }

    public boolean isDownloadcompleted;

    @Override
    public String toString() {
        return "Download{" +
                "VersionSize=" + VersionSize +
                ", downloadProgress=" + downloadProgress +
                ", isDownloadcompleted=" + isDownloadcompleted +
                '}';
    }
}
