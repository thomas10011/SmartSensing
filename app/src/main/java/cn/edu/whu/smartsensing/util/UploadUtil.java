package cn.edu.whu.smartsensing.util;

import android.util.Log;
import android.widget.ArrayAdapter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.hsf.HSFJSONUtils;

import org.json.JSONArray;
import org.letterli.sendfile.SendFiletoServer;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import cn.edu.whu.smartsensing.service.McDataCallbackService;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadUtil {

    private static final SendFiletoServer sendFiletoServer = SendFiletoServer.getInstance();

    // upload data
    private static final String filepath = "/storage/emulated/0/Android/data" +
            "/cn.edu.whu.smartsensing/files/";
    private static final String filepath_sensor = "/storage/emulated/0/Android/data" +
            "/cn.edu.whu.smartsensing/files/AccelerationRecord/";
    private static final String filepath_audio = "/storage/emulated/0/Android/data" +
            "/cn.edu.whu.smartsensing/files/AudioRecord/";
    private static final String TAG = "UploadFile";
    private static final String TAG_sensor = "UploadFile_sensor";

    // private static String server = "http://124.156.134.117/";
    private static String server = "http://host.tanhuiri.cn:19526/";

    //  遍历某路径下所有文件,生成list
    public static List<String> getFilesAllName(String path) {
        File file=new File(path);
        File[] files=file.listFiles();
        if (files == null || files.length == 0){ Log.e("error","空目录"); return new ArrayList<>(); }
        List<String> s = new ArrayList<>();
        for (File value : files) {
            s.add(value.getAbsolutePath());
        }
        Log.d("search","getFilesAllName ok in " + path);
        return s;
    }

    public static void uploadSensorData() {
        getFilesAllName(filepath_sensor).forEach(
                file -> {
                    String fileName = file.replaceAll(filepath_sensor,"");
                    Log.i("Upload Util", "准备开始上传加速度数据：" + fileName);
                    /*----Add SendFile Code----*/
                    sendFiletoServer.setTimeOut(350);  //60 means 60 seconds, 120 means 2 minutes
                    sendFiletoServer.upload(
                            file, fileName, MediaType.parse("text/csv"),
                            server + "/api/file",
                            new HashMap<String, String>(){
                                    {
                                        put("uid", FileUtil.readFile(filepath, "info"));
                                        put("type", "acceleration");
                                    }
                            }
                    )                    ;
                }
        );
    }


    public static void uploadAudioData() {
        getFilesAllName(filepath_audio).forEach(
                file -> {
                    String fileName = file.replaceAll(filepath_audio,"");
                    Log.i("Upload Util", "准备开始上传音频数据：" + fileName);
                    /*----Add SendFile Code----*/
                    sendFiletoServer.setTimeOut(350);  //60 means 60 seconds, 120 means 2 minutes
                    sendFiletoServer.upload(
                            file, fileName, MediaType.parse("text/csv"),
                            server + "api/file",
                            new HashMap<String, String>(){
                                {
                                    put("uid", FileUtil.readFile(filepath, "info"));
                                    put("type", "audio");
                                }
                            }
                    )                    ;
                }
        );
    }

    private static int TIME_OUT = 60*1000;                          //超时时间为60s*1000 = 1000min
    private static final String CHARSET = "utf-8";                         //编码格式
    private static final String PREFIX = "--";                            //前缀
    private static final String BOUNDARY = UUID.randomUUID().toString();  //边界标识 随机生成
    private static final String CONTENT_TYPE = "multipart/form-data";     //内容类型
    private static final String LINE_END = "\r\n";                        //换行
    private static final String IMGUR_CLIENT_ID = "...";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static OkHttpClient client = new OkHttpClient();


    public static void uploadMcData(List<String> date) {
        client = new OkHttpClient.Builder()
                .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
                .readTimeout(TIME_OUT,TimeUnit.SECONDS)
                .build();
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        //准备文件与传入参数

                        RequestBody requestBody = RequestBody.create(JSON, com.alibaba.fastjson.JSON.toJSONString(date));
                        try {
                            Log.d(TAG+" requestBody", String.valueOf(requestBody.contentLength()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        Request request = new Request.Builder()
                                .addHeader("Connection","keep-alive")
                                .url(server + "/api/mc-data/" + FileUtil.readFile(filepath, "info"))
                                .post(requestBody)
                                .build();
                        Log.i(TAG+" request", String.valueOf(request.body()));

                        Response response = null;
                        try {
                            response = client.newCall(request).execute();
                            String responseData = response.body().string();
                            Log.i(TAG, responseData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).start();
    }


    public static void getMcData()  {

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Request request = new Request.Builder()
                                .addHeader("Connection","keep-alive")
                                .url(server + "/api/mc-data/" + FileUtil.readFile(filepath, "info"))
                                .get()
                                .build();
                        client.newCall(request).enqueue(new McDataCallbackService());
                    }
                }
        ).start();
    }

}
