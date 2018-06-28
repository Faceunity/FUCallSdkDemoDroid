package com.tencent.calldemo;

import android.app.Application;

import com.faceunity.beautycontrolview.FURenderer;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FURenderer.initFURenderer(this);
    }
}
