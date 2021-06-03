package com.tencent.yolov5ncnn;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.facebook.common.util.UriUtil;

import java.io.FileNotFoundException;
import java.io.IOException;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;


public class DetectionModule extends UniModule {
    Boolean yoloIsLoad = false;
    String TAG = "DetectionModule";
    public static int REQUEST_CODE = 1000;
    private YoloV5Ncnn yolov5ncnn = new YoloV5Ncnn();
    private Bitmap bitmap = null;
    // 待识别的图片
    private Bitmap yourSelectedImage = null;
    //run ui thread
    // options = {uri:'文件路径',isGpu：0 否，1表示是}
    @RequiresApi(api = Build.VERSION_CODES.N)
    @UniJSMethod(uiThread = true)
    public void detecteAsyncFunc(JSONObject options, UniJSCallback callback) {
        if(callback == null) {
            return;
        }
        if(!yoloIsLoad){
            String stride8OutName = (String) options.get("stride8OutName");
            String stride16OutName = (String) options.get("stride16OutName");
            String stride32OutName = (String) options.get("stride32OutName");
            String inputName = (String) options.get("inputName");
            int imgSize = options.getInteger("imgSize");
            float probThreshold = options.getFloatValue("probThreshold");
            float nmsThreshold = options.getFloatValue("nmsThreshold");
            YoloV5Ncnn.InitParams initParams = new YoloV5Ncnn.InitParams();
            if(stride8OutName != null && stride8OutName != ""){
                initParams.stride8OutName = stride8OutName;
            }
            if(stride16OutName != null && stride16OutName != ""){
                initParams.stride16OutName = stride16OutName;
            }
            if(stride32OutName != null && stride32OutName != ""){
                initParams.stride32OutName = stride32OutName;
            }
            if(inputName != null && inputName != ""){
                initParams.inputName = inputName;
            }
            if(imgSize != 0){
                initParams.imgSize = imgSize;
            }
            if(probThreshold != 0f){
                initParams.probThreshold = probThreshold;
            }
            if(nmsThreshold != 0f){
                initParams.nmsThreshold = nmsThreshold;
            }
            yoloIsLoad = yolov5ncnn.Init(mUniSDKInstance.getContext().getAssets(),
                    initParams.stride8OutName,
                    initParams.stride16OutName,
                    initParams.stride32OutName,
                    initParams.inputName,
                    initParams.imgSize,
                    initParams.probThreshold,
                    initParams.nmsThreshold
                    );
        }
        if(!yoloIsLoad){
            Log.e(TAG, "识别模块加载失败了--");
        }
        Log.e(TAG, "进来了--"+options);
        String uri = (String) options.get("uri");
        Integer isGpu = (Integer) options.get("isGpu");
        Boolean isGpuBool = false;
        if(isGpu != null && isGpu ==1){
            isGpuBool = true;
        }
        if(uri == null || uri.length() == 0){
            Log.e(TAG, "没有图片路径");
        }
        JSONObject data = new JSONObject();
        Uri parseUri = UriUtil.parseUriOrNull(uri);
        Boolean finalIsGpuBool = isGpuBool;
        new Thread(){
            @Override
            public void run() {
                try {
                    bitmap = decodeUri(parseUri);
                    yourSelectedImage = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    YoloV5Ncnn.Obj[] objects = yolov5ncnn.Detect(yourSelectedImage, finalIsGpuBool);
                    String s = JSONObject.toJSONString(objects);
                    System.out.println("识别的结果是");
                    System.out.println(s);
                    data.put("code", "success");
                    data.put("data",s);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    data.put("code", "error");
                    data.put("data","图片处理异常");
                }
                callback.invoke(data);
            }
        }.start();
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException
    {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(mUniSDKInstance.getContext().getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 640;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeStream(mUniSDKInstance.getContext().getContentResolver().openInputStream(selectedImage), null, o2);

        // Rotate according to EXIF
        int rotate = 0;
        try
        {
            ExifInterface exif = new ExifInterface(mUniSDKInstance.getContext().getContentResolver().openInputStream(selectedImage));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        }
        catch (IOException e)
        {
            Log.e("MainActivity", "ExifInterface IOException");
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
