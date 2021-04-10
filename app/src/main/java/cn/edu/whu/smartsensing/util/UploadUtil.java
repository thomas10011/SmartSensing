package cn.edu.whu.smartsensing.util;

import android.util.Log;

import androidx.annotation.Nullable;

import org.letterli.sendfile.SendFileToServer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import cn.edu.whu.smartsensing.service.FileListCallbackService;
import cn.edu.whu.smartsensing.service.McDataCallbackService;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadUtil {

    private static final SendFileToServer sendFiletoServer = SendFileToServer.getInstance();
    private static final int TIME_OUT = 60*1000;                          //超时时间为60s*1000 = 1000min
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(TIME_OUT, TimeUnit.SECONDS).readTimeout(TIME_OUT,TimeUnit.SECONDS).build();;


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

    public static void uploadSensorData(@Nullable Map<String, String> fileList, String type) {
        // 先查询服务器上面的文件列表
        getFilesAllName(filepath_sensor).forEach(
            file -> {
                String fileName = file.replaceAll(type.equals("acceleration") ? filepath_sensor : filepath_audio,"");
                Log.i("Upload Util", "准备开始上传" + type + "数据：" + fileName);
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
                                put("type", type);
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

    public static void notifyServer()  {

        new Thread(
            new Runnable() {
                @Override
                public void run() {
                    Request request = new Request.Builder()
                            .addHeader("Connection","keep-alive")
                            .url(server + "api/notice/" + FileUtil.readFile(filepath, "info"))
                            .get()
                            .build();
                    try {
                        client.newCall(request).execute();
                    }
                    catch (IOException e) {
                        Log.e(TAG, "run: 通知服务器用户活动信息时出错");
                    }
                }
            }
        ).start();
    }


}
