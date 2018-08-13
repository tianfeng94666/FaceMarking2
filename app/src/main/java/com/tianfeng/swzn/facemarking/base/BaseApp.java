package com.tianfeng.swzn.facemarking.base;

import android.app.Application;

import com.tianfeng.swzn.facemarking.drag.component.AppComponent;
import com.tianfeng.swzn.facemarking.drag.component.DaggerAppComponent;
import com.tianfeng.swzn.facemarking.drag.module.AppModule;
import com.tianfeng.swzn.facemarking.utils.SpUtils;


public class BaseApp extends Application {
    AppComponent mAppComponent;
    public  static SpUtils spUtils;
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(getApplicationContext());
        spUtils = SpUtils.getInstace(this);

        mAppComponent= DaggerAppComponent.builder().appModule(new AppModule(getBaseContext(),Constants.BASE_URL)).build();
    }
    public AppComponent getAppCommponent(){
        return mAppComponent;
    }
}
