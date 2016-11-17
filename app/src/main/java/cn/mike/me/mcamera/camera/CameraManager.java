package cn.mike.me.mcamera.camera;

import android.hardware.camera2.DngCreator;
import android.media.Image;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.List;

/**
 * Created by ske on 2016/11/11.
 */

public abstract class CameraManager {

    public static class CameraFeatures {
        public boolean isZoomSupported = false;
        public int maxZoom = 0;
        public List<Integer> zoomRatios = null;
        public boolean supportFaceDetection = false;
        public List<Size> pictureSizes = null;
        public List<Size> videoSizes = null;
        public List<Size> previewSizes = null;
        public List<String> supported_flash_values = null;
        public List<String> supported_focus_values = null;
        public int max_num_focus_areas = 0;
        public float minimum_focus_distance = 0.0f;
        public boolean is_exposure_lock_supported = false;
        public boolean is_video_stabilization_supported = false;
        public boolean supports_iso_range = false;
        public int min_iso = 0;
        public int max_iso = 0;
        public boolean supports_exposure_time = false;
        public long min_exposure_time = 0L;
        public long max_exposure_time = 0L;
        public int min_exposure = 0;
        public int max_exposure = 0;
        public float exposure_step = 0.0f;
        public boolean can_disable_shutter_sound = false;
        public boolean supports_expo_bracketing = false;
        public boolean supports_raw = false;
    }

    public abstract CameraFeatures getCameraFeatures();

    public abstract void openDriver(int cameraId);

    public abstract void release();

    public abstract void setCameraDisplayOrientation(int degree);

    public abstract void startPreview();

    public abstract void stopPreview();

    public abstract void setPreviewDisplay(SurfaceHolder holder);

    public abstract void setPictureSize(int width, int height);

    public abstract Size getPictureSize();

    public abstract void setPreviewSize(int width, int height);

    public abstract void setFocusValue(String focusValue);

    public interface PictureCallback {
        void onCompleted(); // called after all relevant on*PictureTaken() callbacks have been called and returned

        void onPictureTaken(byte[] data);

        /**
         * Only called if RAW is requested.
         * Caller should call image.close() and dngCreator.close() when done with the image.
         */
        void onRawPictureTaken(DngCreator dngCreator, Image image);

        /**
         * Only called if burst is requested.
         */
        void onBurstPictureTaken(List<byte[]> images);

        /* This is called for flash_frontscreen_auto or flash_frontscreen_on mode to indicate the caller should light up the screen
         * (for flash_frontscreen_auto it will only be called if the scene is considered dark enough to require the screen flash).
         * The screen flash can be removed when or after onCompleted() is called.
         */
        void onFrontScreenTurnOn();
    }
}
