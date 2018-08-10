package com.tianfeng.swzn.facemarking.base;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.tianfeng.swzn.facemarking.drag.component.AppComponent;

import javax.inject.Inject;



public class BaseActivity extends AppCompatActivity {
    private BaseApp app;
    private AppComponent mAppComponent;

    public AppComponent getmAppComponent() {
        return app.getAppCommponent();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (BaseApp) getApplication();
    }


}
