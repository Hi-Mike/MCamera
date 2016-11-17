package cn.mike.me.mcamera.view;

import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import com.orhanobut.logger.Logger;

import cn.mike.me.mcamera.camera.CameraException;
import cn.mike.me.mcamera.camera.CameraManager;
import cn.mike.me.mcamera.camera.CameraManager1;
import cn.mike.me.mcamera.camera.Size;
import cn.mike.me.mcamera.utils.CameraUtil;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = CameraPreview.class.getSimpleName();

    private SurfaceHolder mHolder;
    private CameraManager1 cameraManager;
    private CameraManager.CameraFeatures cameraFeatures;
    private int previewWidth;
    private double previewRadio;

    public CameraPreview(Context context, int width) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        previewWidth = width;
    }

    public void setCamera(CameraManager1 manager, CameraManager.CameraFeatures features) {
//        Logger.d("setCamera");
        if (cameraManager == manager) {
            return;
        }
        cameraFeatures = features;

        stopPreviewAndFreeCamera();

        cameraManager = manager;

        Size picSize = cameraManager.getPictureSize();

        Size previewSize = CameraUtil.getPreferSupportPreviewSize(picSize.width, picSize.height, cameraFeatures.previewSizes);
        if (previewSize != null) {
            cameraManager.setPreviewSize(previewSize.width, previewSize.height);
            changePreviewRatioIfNeed(previewSize.width, previewSize.height);
        }

        try {
            cameraManager.setPreviewDisplay(mHolder);
            // Important: Call startPreview() to start updating the preview
            // surface. Preview must be started before you can take a picture.
            cameraManager.startPreview();
        } catch (CameraException e) {
            e.printStackTrace();
        }
    }

    private void stopPreviewAndFreeCamera() {
        if (cameraManager != null) {
            cameraManager.stopPreview();

            cameraManager.release();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Logger.d("surfaceCreated");
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            cameraManager.setPreviewDisplay(holder);
            // Important: Call startPreview() to start updating the preview
            // surface. Preview must be started before you can take a picture.
            cameraManager.startPreview();
            startFaceDetection();
        } catch (CameraException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Logger.d("surfaceChanged");
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            cameraManager.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

//        Size picSize = cameraManager.getPictureSize();
//
//        Size previewSize = CameraUtil.getPreferSupportPreviewSize(picSize.width, picSize.height, cameraFeatures.previewSizes);
//        if (previewSize != null) {
//            cameraManager.setPreviewSize(previewSize.width, previewSize.height);
//            changePreviewRatioIfNeed(previewSize.width, previewSize.height);
//        }

        // start preview with new settings
        try {
            cameraManager.setPreviewDisplay(mHolder);
            cameraManager.startPreview();
            startFaceDetection();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void changePreviewRatioIfNeed(int width, int height) {
//        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
//        if (params.width * height != params.height * width) {
//            params.width = getWidth();
//            params.height = getWidth() * width / height;
//            Logger.d("layoutParams:" + getWidth() + " " + params.height);
//            setLayoutParams(params);
        previewRadio = (double) width / height;
        requestLayout();
//        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Logger.d("surfaceDestroyed");
        if (cameraManager != null) {
            cameraManager.stopPreview();
        }
        // empty. Take care of releasing the Camera preview in your activity.
    }

    //    开始人脸识别
    public void startFaceDetection() {
        if (cameraFeatures.supportFaceDetection) {
            cameraManager.startFaceDetection();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(previewWidth, (int) (previewWidth * previewRadio));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }
}