package cn.mike.me.mcamera.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import cn.mike.me.mcamera.camera.PicSize;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

/**
 * Created by ske on 2016/11/10.
 */

public class CameraUtil {
    /**
     * 判断是否存在相机硬件
     */
    public static boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * Create a File for saving an image or video
     */
    public static File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "MyCameraApp");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    /**
     * 这里由于应用设置只为竖屏，rotation不变
     *
     * @param cameraId
     * @param camera
     * @see #setCameraDisplayOrientation(Activity, int, Camera)
     */
    public static void setCameraDisplayOrientation(int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = info.orientation % 360;
            result = (360 - result) % 360;//前置摄像头，补偿镜面效果
        } else {// 后置摄像头
            result = (info.orientation + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * 设置预览需要转动的个度
     *
     * @param activity
     * @param cameraId
     * @param camera
     */
    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // 前置摄像头，补偿镜面效果
        } else {  // 后置摄像头
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * 过滤，只获取4：3和16：9的值
     *
     * @param sizeList
     * @return
     */
    public static List<PicSize> getFilterSupportPicSize(List<Camera.Size> sizeList) {
        List<PicSize> result = new ArrayList<>();
        for (Camera.Size size : sizeList) {
            int width = size.width;
            int height = size.height;
            if (((4 * height == 3 * width) || (16 * height == 9 * width)) && width * height > 300000) {
                result.add(new PicSize(size.width, size.height));
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * 获取指定比率的最大size
     *
     * @param isRatioThreeFour
     * @param sizeList
     * @return
     */
    public static Camera.Size getPrefferSupportPreviewSize(boolean isRatioThreeFour, List<Camera.Size> sizeList) {
        // TODO: 2016/11/11 previewSize是否需要获取最大值，是否对其他东西有影响？
        Collections.sort(sizeList, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                return o2.width * o2.height - o1.width * o1.height;
            }
        });
        if (isRatioThreeFour) {
            for (Camera.Size size : sizeList) {
                if (size.width * 3 == size.height * 4) {
                    return size;
                }
            }
        } else {
            for (Camera.Size size : sizeList) {
                if (size.width * 9 == size.height * 16) {
                    return size;
                }
            }
        }
        return null;
    }

    /**
     * log Camera.Size
     *
     * @param TAG
     * @param sizeList
     * @see Camera.Parameters#getSupportedPictureSizes()
     * @see Camera.Parameters#getSupportedPreviewSizes()
     */
    public static void logSupportSize(String TAG, List<Camera.Size> sizeList) {
        //高宽比
        Log.d(TAG, "start=========================================>");
        for (Camera.Size size : sizeList) {
            int width = size.width;
            int height = size.height;
            Log.d(TAG, width + " " + height + " " + (double) width / height + " " + Math.round(width * height / 100000) * 10 + "万");
        }
        Log.d(TAG, "end=========================================>");
    }

    public static void logListString(String TAG, List<String> strings) {
        Log.d(TAG, "=========================================>" + strings.size());
        for (String str : strings) {
            Log.d(TAG, "support:" + str);
        }
        Log.d(TAG, "=========================================>");
    }
}