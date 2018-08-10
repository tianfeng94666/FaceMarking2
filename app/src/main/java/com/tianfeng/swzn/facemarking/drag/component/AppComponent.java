package com.tianfeng.swzn.facemarking.drag.component;

import android.content.Context;

import com.tianfeng.swzn.facemarking.base.BaseApp;
import com.tianfeng.swzn.facemarking.drag.module.AppModule;
import com.tianfeng.swzn.facemarking.http.APIService;

import javax.inject.Singleton;

import dagger.Component;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;


@Singleton
@Component(modules = AppModule.class)
public interface AppComponent {
    void inject(BaseApp baseApp);

    Context getContext();

    Retrofit getRetrofit();

    OkHttpClient getOkHttpClient();

    APIService getAPIService();
}
