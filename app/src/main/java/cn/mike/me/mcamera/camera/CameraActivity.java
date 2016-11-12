package cn.mike.me.mcamera.camera;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
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

public class CameraActivity extends AppCompatActivity implements Camera.AutoFocusCallback, View.OnTouchListener {
    private static final String TAG = CameraActivity.class.getSimpleName();

    @BindView(R.id.button_capture)
    Button buttonCapture;
    @BindView(R.id.zoom_seek)
    AppCompatSeekBar zoomSeek;

    private CameraPreview mPreview;
    private Camera mCamera;
    private CameraManager cameraManager;

    //只用4：3和16：9的尺寸
    private List<PicSize> filterSupportPicSize;

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
        cameraManager = CameraManager.getInstance();
        mCamera = cameraManager.getCameraInstance(0);
        if (mCamera != null) {
            CameraUtil.setCameraDisplayOrientation(this, 0, mCamera);
            Camera.Parameters parameters = mCamera.getParameters();
//            获取指定尺寸的size
            filterSupportPicSize = CameraUtil.getFilterSupportPicSize(parameters.getSupportedPictureSizes());
//            设置最大像素为默认值
            PicSize picSize = filterSupportPicSize.get(0);
            parameters.setPictureSize(picSize.width, picSize.height);
//           对焦模式
//           CameraUtil.logListString("SupportFocus", parameters.getSupportedFocusModes());
            if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                Logger.d("original:" + parameters.getFocusMode());
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                Logger.d("new:" + parameters.getFocusMode());
            }
//            缩放
            if (parameters.isZoomSupported()) {
                zoomSeek.setMax(parameters.getMaxZoom());
                Logger.d("zoom:" + parameters.isSmoothZoomSupported() + " " + parameters.getZoom() + " " + parameters.getMaxZoom() + " " + parameters.getZoomRatios().size() + " item:" + parameters.getZoomRatios().get(0));
            }
//            场景
//            CameraUtil.logListString("SceneMode", parameters.getSupportedSceneModes());
            parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
//            滤镜
            CameraUtil.logListString("ColorEffect", parameters.getSupportedColorEffects());
//            parameters.setColorEffect("sketch");

//            预览图层数据
            mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {

                }
            });

            mCamera.setParameters(parameters);
            mPreview.setCamera(mCamera);
            mCamera.setFaceDetectionListener(new MyFaceDetectionListener());
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
        mPreview = new CameraPreview(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        mPreview.setOnTouchListener(this);
        matrix = new Matrix();
        zoomSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setZoom(progress);
                mCamera.setParameters(parameters);
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
        mCamera.takePicture(null, null, new JPEGPictureCallBack());
    }

    @OnClick(R.id.btn_focus)
    void focus(View view) {
        mCamera.autoFocus(this);
    }

    @OnClick(R.id.btn_zoom)
    void zoom(View view) {
        zoomSeek.setVisibility(zoomSeek.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    @OnClick(R.id.btn_pic_size)
    void setPicSize() {
        new MaterialDialog.Builder(CameraActivity.this)
                .title("照片尺寸")
                .items(filterSupportPicSize)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                        PicSize picSize = filterSupportPicSize.get(position);
                        Camera.Parameters parameters = mCamera.getParameters();
                        parameters.setPictureSize(picSize.width, picSize.height);
                        mCamera.setParameters(parameters);
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
                    if (null != mCamera) {
                        Camera.Parameters parameters = mCamera.getParameters();
                        parameters.setRotation(rotation);
                        mCamera.setParameters(parameters);
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
            cameraManager.releaseCamera();
        }
    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        float[] distances = new float[3];
        camera.getParameters().getFocusDistances(distances);
        Logger.d("focusSuccess:" + success + " " + distances[0] + " " + distances[1] + " " + distances[2]);
    }

    class JPEGPictureCallBack implements Camera.PictureCallback {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
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
            camera.startPreview();
            buttonCapture.setClickable(true);
        }
    }

    Matrix matrix;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            focusOnTouch(event);
        }
        return false;
    }

    //    设置聚焦区域
    protected void focusOnTouch(MotionEvent event) {
        if (mCamera != null) {

            mCamera.cancelAutoFocus();
            Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f);

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            List<Camera.Area> areas = new ArrayList<>();
            areas.add(new Camera.Area(focusRect, 1000));
            parameters.setFocusAreas(areas);
            Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f);
            Logger.d(focusRect + " " + meteringRect);
            if (parameters.getMaxNumMeteringAreas() > 0) {
                List<Camera.Area> meterAreas = new ArrayList<>();
                meterAreas.add(new Camera.Area(meteringRect, 1000));
                parameters.setMeteringAreas(meterAreas);
            }

            mCamera.setParameters(parameters);
            mCamera.autoFocus(this);
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
}
