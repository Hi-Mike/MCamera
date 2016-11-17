package cn.mike.me.mcamera.camera;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.camera2.DngCreator;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.mike.me.mcamera.R;
import cn.mike.me.mcamera.utils.CameraUtil;
import cn.mike.me.mcamera.view.CameraPreview;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

/**
 * Created by ske on 2016/11/10.
 */

public class CameraActivity extends AppCompatActivity implements Camera.AutoFocusCallback {
    private static final String TAG = CameraActivity.class.getSimpleName();

    @BindView(R.id.button_capture)
    Button buttonCapture;
    @BindView(R.id.zoom_seek)
    AppCompatSeekBar zoomSeek;

    private CameraPreview mPreview;
    //    private Camera mCamera;
    private CameraManager1 cameraManager;
    private CameraManager.CameraFeatures cameraFeatures;

    //只用4：3和16：9的尺寸
    private List<Size> filterSupportSize;

    private OrientationEventListener orientationEventListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        setupView();

        setupCamera();

        setupOrientationListener();
    }

    private void setupCamera() {
        cameraManager = CameraManager1.getInstance();
        try {
            cameraManager.openDriver(Camera.CameraInfo.CAMERA_FACING_BACK);
            cameraManager.setCameraDisplayOrientation(CameraUtil.getDisplayRotation(this));
            cameraManager.setFaceDetectionListener(new MyFaceDetectionListener());
            cameraFeatures = cameraManager.getCameraFeatures();
            Size picSize = cameraFeatures.pictureSizes.get(0);
            cameraManager.setPictureSize(picSize.width, picSize.height);
            zoomSeek.setMax(cameraFeatures.maxZoom);
            mPreview.setCamera(cameraManager, cameraFeatures);
            filterSupportSize = cameraFeatures.pictureSizes;
//            drawPreview.setCamera(mCamera);
        } catch (CameraException e) {

        }
    }

    class MyFaceDetectionListener implements Camera.FaceDetectionListener {

        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0) {
                Log.d("FaceDetection", "face detected: " + faces.length +
                        " Face 1 Location X: " + faces[0].rect.centerX() +
                        "Y: " + faces[0].rect.centerY());
            }
        }
    }

    private void setupView() {
        mPreview = new CameraPreview(this,CameraUtil.getDisplayWidth(this));
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        mZoomGestureDetector = new ScaleGestureDetector(this, new ZoomGestureListener());
        mGestureDetector = new GestureDetector(this, new GestureListener());
        matrix = new Matrix();
        zoomSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                cameraManager.setZoom(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @OnClick(R.id.button_capture)
    void capture(View view) {
        buttonCapture.setClickable(false);
        cameraManager.takePicture(null, new JPEGPictureCallBack());
    }

    @OnClick(R.id.btn_focus)
    void focus(View view) {
        cameraManager.autoFocus(this);
    }

    @OnClick(R.id.btn_zoom)
    void zoom(View view) {
        zoomSeek.setProgress(cameraManager.getZoom());
        zoomSeek.setVisibility(zoomSeek.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    @OnClick(R.id.btn_pic_size)
    void setPicSize() {
        new MaterialDialog.Builder(CameraActivity.this)
                .title("照片尺寸")
                .items(filterSupportSize)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                        Size picSize = filterSupportSize.get(position);
                        cameraManager.setPictureSize(picSize.width, picSize.height);
                        mPreview.changePreviewRatioIfNeed(picSize.width, picSize.height);
                    }
                })
                .show();
    }

    private int filterOrientation;

    private void setupOrientationListener() {
        orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;
                }
//              过滤值，只取0度，90度，180度，270度,360度，为了减少频繁设置，要区分0度和360度
                orientation = (orientation + 45) / 90 * 90;

                if (filterOrientation != orientation) {
//                设置固定屏幕，info.orientation不变，上面已经设置为90，一般来说不会再改变
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(0, info);

                    filterOrientation = orientation;
                    int rotation = 0;
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        rotation = (info.orientation - orientation + 360) % 360;
                    } else {
                        rotation = (info.orientation + orientation) % 360;
                    }
                    Logger.d("orientation：" + orientation + " " + info.orientation + " " + filterOrientation + " " + rotation);
                    if (null != cameraManager) {
                        cameraManager.setRotation(rotation);
                    }
                }
            }
        };
        orientationEventListener.enable();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //横屏时，锁屏会调用onDestroy，然而不会调用surface中的其他回调，以此来触发
        if (mPreview != null) {
            mPreview.setVisibility(View.VISIBLE);
        }
        //开启
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.setVisibility(View.INVISIBLE);
        }
        //释放
    }

    @Override
    protected void onDestroy() {
        Logger.d("onDestroy CameraActivity");
        super.onDestroy();
        orientationEventListener.disable();
        if (cameraManager != null) {
            cameraManager.release();
        }
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        float[] distances = new float[3];
        camera.getParameters().getFocusDistances(distances);
        Logger.d("focusSuccess:" + success + " " + distances[0] + " " + distances[1] + " " + distances[2]);
    }

    class JPEGPictureCallBack implements CameraManager.PictureCallback {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onPictureTaken(byte[] data) {
            File pictureFile = CameraUtil.getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null) {
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Logger.d("File not found: " + e.getMessage());
            } catch (IOException e) {
                Logger.d("Error accessing file: " + e.getMessage());
            }
            Toast.makeText(CameraActivity.this, "照片已保存", Toast.LENGTH_SHORT).show();
            cameraManager.startPreview();
            buttonCapture.setClickable(true);
        }

        @Override
        public void onRawPictureTaken(DngCreator dngCreator, Image image) {

        }

        @Override
        public void onBurstPictureTaken(List<byte[]> images) {

        }

        @Override
        public void onFrontScreenTurnOn() {

        }
    }

    Matrix matrix;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            focusOnTouch(event);
//        }
        mHasPinchZoomed = false;
        mZoomGestureDetector.onTouchEvent(event);

        if (!mHasPinchZoomed) {
            mGestureDetector.onTouchEvent(event);
        }

        return true;
    }

    //    设置聚焦区域
    protected void focusOnTouch(MotionEvent event) {
        if (cameraManager != null) {
            cameraManager.cancelAutoFocus();

            cameraManager.setFocusValue(Camera.Parameters.FOCUS_MODE_AUTO);

            Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 1000));
            Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f);
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(meteringRect, 1000));

            cameraManager.setFocusAndMeteringArea(focusAreas, meteringAreas);
            cameraManager.autoFocus(this);
        }
    }

    //    聚焦区域
    private Rect calculateTapArea(float x, float y, float coefficient) {
        int areaSize = Float.valueOf(300 * coefficient).intValue();

        int left = clamp((int) x - areaSize / 2, 0, mPreview.getWidth() - areaSize);
        int top = clamp((int) y - areaSize / 2, 0, mPreview.getHeight() - areaSize);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        matrix.mapRect(rectF);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom) > 1000 ? 1000 : Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    private boolean mHasPinchZoomed;
    private boolean mIsFocusing;
    private ScaleGestureDetector mZoomGestureDetector;
    private GestureDetector mGestureDetector;

    private class ZoomGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            Log.d("onScale", "begin=====>");
            return super.onScaleBegin(detector);
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            Log.d("onScale", "onScale=====>");

            if (!mIsFocusing) {
                if (detector.getScaleFactor() > 1.0f) {
                    cameraManager.setZoom(Math.min(cameraManager.getZoom() + 1, cameraFeatures.maxZoom));
                } else if (detector.getScaleFactor() < 1.0f) {
                    cameraManager.setZoom(Math.max(cameraManager.getZoom() - 1, 0));
                } else {
                    return false;
                }
                mHasPinchZoomed = true;
            }

            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            focusOnTouch(e);
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }
}
