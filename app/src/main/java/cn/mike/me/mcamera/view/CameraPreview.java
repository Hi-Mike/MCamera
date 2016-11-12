package cn.mike.me.mcamera.view;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.List;

import cn.mike.me.mcamera.utils.CameraUtil;

import static android.R.attr.width;
import static cn.mike.me.mcamera.utils.CameraUtil.getPrefferSupportPreviewSize;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = CameraPreview.class.getSimpleName();

    private SurfaceHolder mHolder;
    private Camera mCamera;

    private List<Camera.Size> mSupportedPreviewSizes;

    public CameraPreview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void setCamera(Camera camera) {
//        Logger.d("setCamera");
        if (mCamera == camera) {
            return;
        }

        stopPreviewAndFreeCamera();

        mCamera = camera;

        if (mCamera != null) {
            Camera.Parameters parameters = mCamera.getParameters();
            List<Camera.Size> localSizes = parameters.getSupportedPreviewSizes();
            mSupportedPreviewSizes = localSizes;

//            Camera.Size previewSize = parameters.getPreviewSize();
//            Logger.d("preview:" + previewSize.width + " " + previewSize.height);
//
//            Camera.Size picSize = parameters.getPictureSize();
//            Logger.d("picSize:" + picSize.width + " " + picSize.height);

            requestLayout();
            try {
                mCamera.setPreviewDisplay(mHolder);
                // Important: Call startPreview() to start updating the preview
                // surface. Preview must be started before you can take a picture.
                mCamera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopPreviewAndFreeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();

            // Important: Call release() to release the camera for use by other
            // applications. Applications should release the camera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera.release();

            mCamera = null;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Logger.d("surfaceCreated");
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            // Important: Call startPreview() to start updating the preview
            // surface. Preview must be started before you can take a picture.
            mCamera.startPreview();
            startFaceDetection();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logger.d("surfaceChanged");
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        Camera.Parameters parameters = mCamera.getParameters();
        Camera.Size picSize = parameters.getPictureSize();

        changePreviewRatioIfNeed(picSize.width, picSize.height);

        // set preview size and make any resize, rotate or reformatting changes here
//        Camera.Parameters parameters = mCamera.getParameters();
//        parameters.setJpegQuality(95);

        Camera.Size previewSize = CameraUtil.getPrefferSupportPreviewSize(picSize.width * 3 == picSize.height * 4, parameters.getSupportedPreviewSizes());
        if (previewSize != null) {
            parameters.setPreviewSize(previewSize.width, previewSize.height);
        }

//        parameters.setPictureSize(1920, 1080);
        try {
            mCamera.setParameters(parameters);
        } catch (Exception e) {
            Logger.d("有不支持或无效的相机参数");
        }

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
            startFaceDetection();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void changePreviewRatioIfNeed(int width, int height) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        if (params.width * height != params.height * width) {
            params.width = getWidth();
            params.height = getWidth() * width / height;
            Logger.d("layoutParams:" + getWidth() + " " + params.height);
            setLayoutParams(params);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Logger.d("surfaceDestroyed");
        if (mCamera != null) {
            mCamera.stopPreview();
        }
        // empty. Take care of releasing the Camera preview in your activity.
    }

    //    开始人脸识别
    public void startFaceDetection() {
        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();

        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0) {
            // camera supports face detection, so can start it:
            mCamera.startFaceDetection();
        }
    }
}