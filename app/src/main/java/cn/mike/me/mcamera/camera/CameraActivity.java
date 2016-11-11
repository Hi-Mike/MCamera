package cn.mike.me.mcamera.camera;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.OrientationEventListener;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.orhanobut.logger.Logger;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.mike.me.mcamera.R;
import cn.mike.me.mcamera.utils.CameraUtil;
import cn.mike.me.mcamera.view.CameraPreview;

/**
 * Created by ske on 2016/11/10.
 */

public class CameraActivity extends AppCompatActivity {
    private static final String TAG = CameraActivity.class.getSimpleName();

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
//            CameraUtil.logSupportSize("SupportPreview", parameters.getSupportedPreviewSizes());
//            CameraUtil.logSupportSize("SupportPicture", parameters.getSupportedPictureSizes());
//            获取指定尺寸的size
            filterSupportPicSize = CameraUtil.getFilterSupportPicSize(parameters.getSupportedPictureSizes());
//            设置最大像素为默认值
            PicSize picSize = filterSupportPicSize.get(0);
            parameters.setPictureSize(picSize.width, picSize.height);
            mCamera.setParameters(parameters);
            mPreview.setCamera(mCamera);
        }
    }

    private void setupView() {
        mPreview = new CameraPreview(this);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    @OnClick(R.id.button_capture)
    void capture(View view) {
        mCamera.takePicture(null, null, new JPEGPictureCallBack());
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
}
