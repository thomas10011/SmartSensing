package cn.edu.whu.smartsensing.util;

import android.util.Log;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import cn.edu.whu.smartsensing.service.FileListCallbackService;
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
    private static final String filepath_mc = "/storage/emulated/0/Android/data" +
            "/cn.edu.whu.smartsensing/files/mc/";
    private static final String TAG = "UploadFile";
    private static final String TAG_sensor = "UploadFile_sensor";

    private static String server = "http://124.156.134.117/";
    // private static String server = "http://host.tanhuiri.cn:19526/";

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


    public static void execUploadData(String type) {
        // 要首先查询文件列表
        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Request request = new Request.Builder()
                                .addHeader("Connection","keep-alive")
                                .url(server + "api/file/" + FileUtil.readFile(filepath, "info") + "?type=" + type)
                                .get()
                                .build();
                        client.newCall(request).enqueue(new FileListCallbackService(type));

                    }
                }
        ).start();
    }

    public static void uploadSensorData(@Nullable Map<String, String> fileList) {
        // 先查询服务器上面的文件列表
        getFilesAllName(filepath_sensor).forEach(
                file -> {
                    String fileName = file.replaceAll(filepath_sensor,"");
                    Log.i("Upload Util", "准备开始上传加速度数据：" + fileName);
                    String fileLen = String.valueOf(new File(file).length());
                    // 查询不到文件列表 或者文件列表中不包含该文件 或者包含该文件但大小不一致 则上传该文件
                    if (fileList == null || !fileList.containsKey(fileName) || !fileList.get(fileName).equals(fileLen) ) {
                        /*----Add SendFile Code----*/
                        sendFiletoServer.setTimeOut(350);  //60 means 60 seconds, 120 means 2 minutes
                        sendFiletoServer.upload(
                                file, fileName, MediaType.parse("text/csv"),
                                server + "api/file",
                                new HashMap<String, String>(){
                                    {
                                        put("uid", FileUtil.readFile(filepath, "info"));
                                        put("type", "acceleration");
                                    }
                                }
                        );
                    }
                    else {
                        Log.i(TAG, "uploadSensorData: " + "文件" + fileName + "在服务器上已存在，略过");
                    }
                }
        );
    }


    public static void uploadAudioData(@Nullable Map<String, String> fileList) {
        // 先查询服务器上面的文件列表

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

    private static final int TIME_OUT = 60*1000;                          //超时时间为60s*1000 = 1000min
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(TIME_OUT, TimeUnit.SECONDS).readTimeout(TIME_OUT,TimeUnit.SECONDS).build();;


    public static void uploadMcData(List<String> date) {
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
                                .url(server + "api/mc-data/" + FileUtil.readFile(filepath, "info"))
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

    public static void uploadMcFile() {
        // 始终上传更新
        getFilesAllName(filepath_mc).forEach(
                file -> {
                    String fileName = file.replaceAll(filepath_mc,"");
                    Log.i("Upload Util", "准备开始上传mc数据：" + fileName);
                    /*----Add SendFile Code----*/
                    sendFiletoServer.setTimeOut(350);  //60 means 60 seconds, 120 means 2 minutes
                    sendFiletoServer.upload(
                            file, fileName, MediaType.parse("text/csv"),
                            server + "api/file",
                            new HashMap<String, String>(){
                                {
                                    put("uid", FileUtil.readFile(filepath, "info"));
                                    put("type", "mc");
                                }
                            }
                    );
                }
        );
    }


    public static void getMcData()  {

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        Request request = new Request.Builder()
                                .addHeader("Connection","keep-alive")
                                .url(server + "api/mc-data/" + FileUtil.readFile(filepath, "info"))
                                .get()
                                .build();
                        client.newCall(request).enqueue(new McDataCallbackService());
                    }
                }
        ).start();
    }


}
