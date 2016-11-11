package cn.mike.me.mcamera;

import android.app.Application;

import com.orhanobut.logger.Logger;

/**
 * Created by ske on 2016/11/11.
 */

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.init("MCamera");
    }
}
