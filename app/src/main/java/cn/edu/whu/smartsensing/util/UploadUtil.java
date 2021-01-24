package cn.edu.whu.smartsensing.util;

import android.util.Log;

import org.letterli.sendfile.SendFiletoServer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;

public class UploadUtil {

    private static final SendFiletoServer sendFiletoServer = SendFiletoServer.getInstance();

    // upload data
    private static final String filepath = "/storage/emulated/0/Android/data" +
            "/cn.edu.whu.smartsensing/files/AccelerationRecord";
    private static final String filepath_sensor = "/storage/emulated/0/Android/data" +
            "/cn.edu.whu.smartsensing/files/AccelerationRecord";
    private static List<String> filelist_1 = new ArrayList<>();
    private static List<String> filelist_2 = new ArrayList<>();
    private static String filepath_1 = null;
    private static String filepath_2 = null;
    private static String filename_1 = null;
    private static String filename_2 = null;

    private static final String TAG = "UploadFile";
    private static final String TAG_sensor = "UploadFile_sensor";



    //  遍历某路径下所有文件,生成list
    public static List<String> getFilesAllName(String path) {
        File file=new File(path);
        File[] files=file.listFiles();
        if (files == null){Log.e("error","空目录");return null;}
        List<String> s = new ArrayList<>();
        for(int i =0;i<files.length;i++){
            s.add(files[i].getAbsolutePath());
        }
        Log.d("search","getFilesAllName ok.");
        return s;
    }

    public static void uploadFile() {
        filelist_2 = getFilesAllName(filepath_sensor);
        int length = filelist_2.size();
        Log.d(TAG_sensor,String.valueOf(length));
        filepath_2 = filelist_2.get(length-2);
        Log.d(TAG_sensor,filepath_2);
        filename_2 = filepath_2.replaceAll(filepath_sensor+"/","");
        Log.d(TAG_sensor,filename_2);

        /*----Add SendFile Code----*/
        sendFiletoServer.setTimeOut(350);  //60 means 60 seconds, 120 means 2 minutes
        sendFiletoServer.setFile(
                filepath_2, filename_2, MediaType.parse("text/csv"),
                "http://192.168.0.104:19526/file");
        Log.d("Upload_2","try");
        /*----Add SendFile Code----*/
        sendFiletoServer.start();
    }

}
