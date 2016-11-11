package cn.mike.me.mcamera.camera;

import android.hardware.Camera;

import com.orhanobut.logger.Logger;

/**
 * Created by ske on 2016/11/11.
 */

public class CameraManager {
    private Camera mCamera;

    public static CameraManager getInstance() {
        return new CameraManager();
    }

    private CameraManager() {
    }

    public Camera getCameraInstance(int cameraId) {
        try {
            mCamera = Camera.open(cameraId);
        } catch (Exception e) {
            Logger.d("相机正在被使用。。。(无权限？)");
        }
        return mCamera;
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();//调用完，释放
            mCamera = null;
        }
    }
}
