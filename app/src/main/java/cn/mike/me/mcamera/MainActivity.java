package cn.mike.me.mcamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.mike.me.mcamera.camera.CameraActivity;
import cn.mike.me.mcamera.utils.CameraUtil;
import kr.co.namee.permissiongen.PermissionFail;
import kr.co.namee.permissiongen.PermissionGen;
import kr.co.namee.permissiongen.PermissionSuccess;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_open)
    Button btnOpen;
    @BindView(R.id.info)
    TextView info;
    @BindView(R.id.activity_main)
    RelativeLayout activityMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (!CameraUtil.checkCameraHardware(this)) {
            info.setText("Sorry,the device does not have a hardware camera");
            btnOpen.setClickable(false);
        }

        btnOpen.setOnClickListener(v -> {
                    PermissionGen.with(this)
                            .addRequestCode(10086)
                            .permissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            .request();
                }
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionGen.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @PermissionSuccess(requestCode = 10086)
    void havePermission() {
        startActivity(new Intent(MainActivity.this, CameraActivity.class));
    }

    @PermissionFail(requestCode = 10086)
    void haveNoPermission() {
        Toast.makeText(this, "没有权限，请在应用设置中给出权限", Toast.LENGTH_SHORT).show();
    }
}
