package cn.mike.me.mcamera.camera;

import android.support.annotation.NonNull;

/**
 * Created by ske on 2016/11/11.
 */

public class PicSize implements Comparable {
    /**
     * Sets the dimensions for pictures.
     *
     * @param w the photo width (pixels)
     * @param h the photo height (pixels)
     */
    public PicSize(int w, int h) {
        width = w;
        height = h;
    }

    @Override
    public int hashCode() {
        return width * 32713 + height;
    }

    /**
     * width of the picture
     */
    public int width;
    /**
     * height of the picture
     */
    public int height;

    @Override
    public int compareTo(@NonNull Object o) {
        PicSize size = (PicSize) o;
        return size.width * size.height - width * height;
    }

    @Override
    public String toString() {
        return Math.round(width * height / 100000) * 10 + "万像素";
    }
}