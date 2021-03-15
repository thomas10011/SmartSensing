package cn.edu.whu.smartsensing.service;

import android.util.Log;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import cn.edu.whu.smartsensing.util.UploadUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class FileListCallbackService implements Callback {

    private static final String tag = "FileListCallbackService";
    private String type;

    public FileListCallbackService(String type) {
        super();
        this.type = type;
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Log.i(tag, "onFailure: 向服务器查询文件列表失败");
    }

    @Override
    public void onResponse(Call call, Response response) {
        Map<String, String> res;
        try {
            res = com.alibaba.fastjson.JSON.parseArray(response.body().string()).stream().collect(
                Collectors.toMap(
                    o -> o.toString().split(",")[0],
                    o -> o.toString().split(",")[1]
                )
            );
        }
        catch (Exception e) {
            res = null;
        }
        UploadUtil.uploadSensorData(res, type);
    }
}
