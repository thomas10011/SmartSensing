package cn.edu.whu.smartsensing.util;

import android.util.Log;

import org.letterli.sendfile.SendFiletoServer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.MediaType;

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
                            "http://192.168.0.104:19526/file",
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
                            "http://192.168.0.104:19526/file",
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

}
