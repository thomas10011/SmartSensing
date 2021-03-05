package cn.edu.whu.smartsensing.service;

import android.util.Log;
import android.widget.ArrayAdapter;

import java.io.IOException;
import java.util.List;

import cn.edu.whu.smartsensing.MainActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class McDataCallbackService implements Callback {

    private static List<String> mcData;
    private static ArrayAdapter<String> adapter;

    private static MainActivity activity;

    public static void registerMcData(List<String> mcData, ArrayAdapter<String> adapter) {
        McDataCallbackService.mcData = mcData;
        McDataCallbackService.adapter = adapter;
    }

    public static void registerActivity(MainActivity activity) {
        McDataCallbackService.activity = activity;
    }

    @Override
    public void onFailure(Call call, IOException e) {
        Log.i("回调失败！", "");
    }

    @Override
    public void onResponse(Call call, Response response) throws IOException {
        Log.i("回调成功！", "");

        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            activity.updateMcData(com.alibaba.fastjson.JSON.parseArray(response.body().string()).toJavaList(String.class));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
    }
}
