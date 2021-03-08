package cn.edu.whu.smartsensing.service;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import cn.edu.whu.smartsensing.util.UploadUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class FileListCallbackService implements Callback {

    private String type;

    public FileListCallbackService(String type) {
        super();
        this.type = type;
    }

    @Override
    public void onFailure(Call call, IOException e) {

    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        Map<String, String> res;
        try {
            res = com.alibaba.fastjson.JSON.parseArray(
                    response.body().string()).stream().collect(Collectors.toMap(
                    o -> o.toString().split(",")[0],
                    o -> o.toString().split(",")[1]
                    )
            );
        }
        catch (Exception e) {
            res = null;
        }
        if (type.equals("acceleration")) {
            UploadUtil.uploadSensorData(res);
        }
        else if (type.equals("audio")) {
            UploadUtil.uploadAudioData(res);
        }
    }
}
