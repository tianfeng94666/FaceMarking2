package com.tianfeng.swzn.facemarking.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.tianfeng.swzn.facemarking.R;
import com.tianfeng.swzn.facemarking.apiFace.ApiFace;
import com.tianfeng.swzn.facemarking.base.BaseActivity;
import com.tianfeng.swzn.facemarking.bean.MessageBean;
import com.tianfeng.swzn.facemarking.camera.CameraView;
import com.tianfeng.swzn.facemarking.camera.FaceSDK;
import com.tianfeng.swzn.facemarking.fragment.ConfirmationDialogFragment;
import com.tianfeng.swzn.facemarking.jsonBean.FaceResult;
import com.tianfeng.swzn.facemarking.utils.BitmapUtils;
import com.tianfeng.swzn.facemarking.utils.CameraUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {


    @BindView(R.id.camera_view)
    CameraView cameraView;
    @BindView(R.id.tv_result)
    TextView tvResult;
    @BindView(R.id.iv_picture)
    ImageView ivPicture;

    private ApiFace apiFace;
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private Handler mBackgroundHandler;
    long lastModirTime;

    /**
     * 接收百度那边人脸检测的结果
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageBean event) {

        JsonObject jsonObject = new Gson().fromJson(event.getResult(), JsonObject.class);
        String error = jsonObject.get("error_code").getAsString();
        if (error.equals("0")) {
            FaceResult result = new Gson().fromJson(event.getResult(), FaceResult.class);
            tvResult.setText("你的年龄是；" + result.getResult().getFace_list().get(0).getAge()
                + ";\n你的颜值评分为：" + (int) result.getResult().getFace_list().get(0).getBeauty());
        } else {
            tvResult.setText("未检测到人脸");
        }

    }

    /**
     * 初始化界面
     */
    private void initView() {
        if (cameraView != null) {
            cameraView.addCallback(mCallback);
        }
    }




    private CameraView.Callback mCallback = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }

        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
        }

        @Override
        public void onPreviewFrame(final byte[] data, final Camera camera) {
            if (System.currentTimeMillis() - lastModirTime <= 300 || data == null || data.length == 0) {
                return;
            }
            Log.i(TAG, "onPreviewFrame " + (data == null ? null : data.length));
            getBackgroundHandler().post(new FaceThread(data, camera));
            lastModirTime = System.currentTimeMillis();
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 1 || grantResults.length != 1) {
                    throw new RuntimeException("Error on requesting cameraView permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "获取到拍照权限",
                            Toast.LENGTH_SHORT).show();
                }
                // No need to start cameraView here; it is handled by onResume
                break;
        }
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        initView();
        apiFace = ApiFace.getInstance();


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            cameraView.start();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ConfirmationDialogFragment
                    .newInstance("获取相机权限失败",
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION,
                            "没有相机权限，app不能为您进行脸部检测")
                    .show(getSupportFragmentManager(), "");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        cameraView.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBackgroundHandler.getLooper().quitSafely();
            } else {
                mBackgroundHandler.getLooper().quit();
            }
            mBackgroundHandler = null;
        }
    }


    private class FaceThread implements Runnable {
        private byte[] mData;
        private ByteArrayOutputStream mBitmapOutput;
        private Matrix mMatrix;
        private Camera mCamera;

        public FaceThread(byte[] data, Camera camera) {
            mData = data;
            mBitmapOutput = new ByteArrayOutputStream();
            mMatrix = new Matrix();
            int mOrienta = cameraView.getCameraDisplayOrientation();
            mMatrix.postRotate(mOrienta * -1);
            mMatrix.postScale(-1, 1);//默认是前置摄像头，直接写死 -1 。
            mCamera = camera;
        }

        @Override
        public void run() {
            Log.i(TAG, "thread is run");
            Bitmap bitmap = null;
            Bitmap roteBitmap = null;
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                int width = parameters.getPreviewSize().width;
                int height = parameters.getPreviewSize().height;

                YuvImage yuv = new YuvImage(mData, parameters.getPreviewFormat(), width, height, null);
                mData = null;
                yuv.compressToJpeg(new Rect(0, 0, width, height), 100, mBitmapOutput);

                byte[] bytes = mBitmapOutput.toByteArray();
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;//必须设置为565，否则无法检测
                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);

                mBitmapOutput.reset();
                roteBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mMatrix, false);
                List<Rect> rects = FaceSDK.detectionBitmap(bitmap, getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels);

                if (null == rects || rects.size() == 0) {
//                    tvResult.setText("没有检测到人脸哦");

                } else {
                    Log.i("janecer", "检测到有" + rects.size() + "人脸");
                    for (int i = 0; i < rects.size(); i++) {//返回的rect就是在TexutView上面的人脸对应的实际坐标
                        Log.i("janecer", "rect : left " + rects.get(i).left + " top " + rects.get(i).top + "  right " + rects.get(i).right + "  bottom " + rects.get(i).bottom);
                        ivPicture.setImageBitmap(roteBitmap);

                        String img_path = getExternalFilesDir(Environment.DIRECTORY_DCIM).getPath() +
                                File.separator + System.currentTimeMillis() + ".jpeg";

                        BitmapUtils.saveJPGE_After(MainActivity.this, roteBitmap, img_path, 100);

                        Log.e("path", img_path);
                        apiFace.getResult(img_path);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mMatrix = null;
//                if (bitmap != null) {
//                    bitmap.recycle();
//                }
//                if (roteBitmap != null) {
//                    roteBitmap.recycle();
//                }

                if (mBitmapOutput != null) {
                    try {
                        mBitmapOutput.close();
                        mBitmapOutput = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


}
