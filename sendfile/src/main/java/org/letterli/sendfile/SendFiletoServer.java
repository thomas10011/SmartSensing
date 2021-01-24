package org.letterli.sendfile;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
public class SendFiletoServer {

    // 单例模式
    private static volatile SendFiletoServer instance;

    private final String TAG = "UploadFile";
    // 需要识别的文件

    private String FILE_NAME;
    private String FILE_PATH;
    private MediaType MEDIA_TYPE;
    private String TO_URL;

    private static int TIME_OUT = 60*1000;                          //超时时间为60s*1000 = 1000min
    private static final String CHARSET = "utf-8";                         //编码格式
    private static final String PREFIX = "--";                            //前缀
    private static final String BOUNDARY = UUID.randomUUID().toString();  //边界标识 随机生成
    private static final String CONTENT_TYPE = "multipart/form-data";     //内容类型
    private static final String LINE_END = "\r\n";                        //换行
    private static final String IMGUR_CLIENT_ID = "...";


    private String responseData;
    private OkHttpClient client;

    private SendFiletoServer() {}

    public static SendFiletoServer getInstance() {
        if(instance == null) {
            synchronized (SendFiletoServer.class) {
                if(instance == null) {
                    instance = new SendFiletoServer();
                }
            }
        }
        return instance;
    }


    // 运行子线程

    public Runnable fileSend = new Runnable() {
        @Override
        public void run() {
            //准备文件与传入参数
            // Use the imgur image upload API as documented at https://api.imgur.com/endpoints/image
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("myfile", FILE_NAME,
                            RequestBody.create(MEDIA_TYPE, new File(FILE_PATH)))
                    .build();
            try {
                Log.d(TAG+" requestBody", String.valueOf(requestBody.contentLength()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            Request request = new Request.Builder()
                    .addHeader("Connection","keep-alive")
                    .url(TO_URL)
                    .post(requestBody)
                    .build();
            Log.i(TAG+" request", String.valueOf(request.body()));

            Response response = null;
            try {
                 response = client.newCall(request).execute();
                responseData = response.body().string();
                Log.i(TAG, responseData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    };

    public void start() {
        Log.i(TAG+"timeout-set", String.valueOf(TIME_OUT));
        client = new OkHttpClient.Builder()
                .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
                .readTimeout(TIME_OUT,TimeUnit.SECONDS)
                .build();
        synchronized (this) {
            new Thread(fileSend).start();
        }
    }

    public void setFile(String filePath, String fileName, MediaType mediaType, String to_url) {
        this.FILE_PATH = filePath;
        this.MEDIA_TYPE = mediaType;
        this.TO_URL = to_url;
        this.FILE_NAME = fileName;
    }

    public void setTimeOut(int time){
        Log.i(TAG+"timeout-set", String.valueOf(TIME_OUT));
        this.TIME_OUT = time;
    }
}
