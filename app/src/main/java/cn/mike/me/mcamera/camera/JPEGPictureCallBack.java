package cn.mike.me.mcamera.camera;

import android.hardware.Camera;

import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import cn.mike.me.mcamera.utils.CameraUtil;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

/**
 * Created by ske on 2016/11/11.
 * jpeg图片回调
 */

public class JPEGPictureCallBack implements Camera.PictureCallback {
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
    }
}
