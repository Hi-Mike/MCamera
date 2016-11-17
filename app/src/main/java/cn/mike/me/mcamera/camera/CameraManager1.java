package cn.mike.me.mcamera.camera;

import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.List;

import cn.mike.me.mcamera.utils.CameraUtil;

import static android.content.ContentValues.TAG;

/**
 * Created by ske on 2016/11/16.
 */

public class CameraManager1 extends CameraManager {
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;

    public static CameraManager1 getInstance() {
        return new CameraManager1();
    }

    private CameraManager1() {
    }

    @Override
    public void openDriver(int cameraId) throws CameraException {
        try {
            mCamera = Camera.open(cameraId);
        } catch (RuntimeException e) {
            Logger.d("相机正在被使用。。。(无权限？)" + e);
            throw new CameraException("无法获取相机:" + e);
        }
        if (mCamera != null) {
            mCameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, mCameraInfo);
        }
    }

    @Override
    public CameraFeatures getCameraFeatures() {
        CameraFeatures features = new CameraFeatures();
        Camera.Parameters parameters = getParameters();
        features.isZoomSupported = parameters.isZoomSupported();
        if (features.isZoomSupported) {
            features.maxZoom = parameters.getMaxZoom();
            features.zoomRatios = parameters.getZoomRatios();
        }
        if (parameters.getMaxNumDetectedFaces() > 0) {
            features.supportFaceDetection = true;
        }
        features.pictureSizes = CameraUtil.getSortedSize(parameters.getSupportedPictureSizes());
        features.previewSizes = CameraUtil.getSortedSize(parameters.getSupportedPreviewSizes());
//        features.pictureSizes = CameraUtil.getFilterSupportPicSize(parameters.getSupportedPictureSizes());
        return features;
    }

    public Camera getCamera() {
        return mCamera;
    }

    private Camera.Parameters getParameters() {
        return mCamera.getParameters();
    }

    private void setCameraParameters(Camera.Parameters parameters) {
        try {
            mCamera.setParameters(parameters);
        } catch (RuntimeException e) {
            e.printStackTrace();
            Logger.d("不合法的参数：" + e);
        }
    }

    @Override
    public void setPictureSize(int width, int height) {
        Camera.Parameters parameters = this.getParameters();
        parameters.setPictureSize(width, height);
        setCameraParameters(parameters);
    }

    @Override
    public void setPreviewSize(int width, int height) {
        Camera.Parameters parameters = this.getParameters();
        parameters.setPreviewSize(width, height);
        setCameraParameters(parameters);
    }

    @Override
    public void setCameraDisplayOrientation(int degrees) {
        int result;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // 前置摄像头，补偿镜面效果
        } else {  // 后置摄像头
            result = (mCameraInfo.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    public void setRotation(int rotation) {
        Camera.Parameters parameters = this.getParameters();
        parameters.setRotation(rotation);
        setCameraParameters(parameters);
    }

    public boolean startFaceDetection() {
        try {
            mCamera.startFaceDetection();
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }

    public void setFaceDetectionListener(Camera.FaceDetectionListener listener) {
        mCamera.setFaceDetectionListener(listener);
    }

    public int getZoom() {
        Camera.Parameters parameters = getParameters();
        return parameters.getZoom();
    }

    public void setZoom(int value) {
        Camera.Parameters parameters = getParameters();
        parameters.setZoom(value);
        setCameraParameters(parameters);
    }

    public void takePicture(Camera.ShutterCallback shutter, PictureCallback picture) {
        Camera.PictureCallback jpeg = picture == null ? null : (Camera.PictureCallback) (data, cam) -> {
            picture.onPictureTaken(data);
            picture.onCompleted();
        };
        mCamera.takePicture(shutter, null, jpeg);
    }

    @Override
    public void startPreview() {
        mCamera.startPreview();
    }

    @Override
    public void stopPreview() {
        mCamera.stopPreview();
    }

    @Override
    public void setPreviewDisplay(SurfaceHolder holder) throws CameraException {
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CameraException(e.getMessage());
        }
    }

    @Override
    public Size getPictureSize() {
        Camera.Parameters parameters = this.getParameters();
        Camera.Size pictureSize = parameters.getPictureSize();
        return new Size(pictureSize.width, pictureSize.height);
    }

    public void cancelAutoFocus() {
        try {
            mCamera.cancelAutoFocus();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void autoFocus(Camera.AutoFocusCallback callback) {
        mCamera.autoFocus(callback);
    }

    @Override
    public void setFocusValue(String focusValue) {
        Camera.Parameters parameters = getParameters();
        if (focusValue.equals("focus_mode_auto") || focusValue.equals("focus_mode_locked")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else if (focusValue.equals("focus_mode_infinity")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
        } else if (focusValue.equals("focus_mode_macro")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        } else if (focusValue.equals("focus_mode_fixed")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
        } else if (focusValue.equals("focus_mode_edof")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
        } else if (focusValue.equals("focus_mode_continuous_picture")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (focusValue.equals("focus_mode_continuous_video")) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
        setCameraParameters(parameters);
    }

    public boolean setFocusAndMeteringArea(List<Camera.Area> focusAreas, List<Camera.Area> meteringAreas) {
        Camera.Parameters parameters = this.getParameters();
        // getFocusMode() is documented as never returning null, however I've had null pointer exceptions reported in Google Play
        if (focusAble(parameters) && focusAreas != null) {
            parameters.setFocusAreas(focusAreas);

            // also set metering areas
            if (parameters.getMaxNumMeteringAreas() > 0) {
                parameters.setMeteringAreas(meteringAreas);
            }
            setCameraParameters(parameters);

            return true;
        } else if (parameters.getMaxNumMeteringAreas() > 0) {
            parameters.setMeteringAreas(meteringAreas);
            setCameraParameters(parameters);
        }
        return false;
    }

    private boolean focusAble(Camera.Parameters parameters) {
        String focusMode = parameters.getFocusMode();
        return parameters.getMaxNumFocusAreas() != 0
                && focusMode != null
                && (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO)
                || focusMode.equals(Camera.Parameters.FOCUS_MODE_MACRO)
                || focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                || focusMode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO));
    }

    @Override
    public void release() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }
}