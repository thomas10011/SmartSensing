package cn.edu.whu.smartsensing.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.Calendar;

import cn.edu.whu.smartsensing.util.AlarmUtil;
import cn.edu.whu.smartsensing.util.UploadUtil;

public class UploadService extends Service {

    private static final String tag = "Upload Service";

    AlarmManager alarmManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 21);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);//这里代表 21.14.00

        //通过AlarmManager定时启动广播
//        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//        Intent i = new Intent(this, AlarmReceive.class);
//        PendingIntent pIntent = PendingIntent.getBroadcast(this, 0, i, 0);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {// 6.0 及以上
//            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pIntent);
//        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//  4.4 及以上
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pIntent);
//        } else {
//            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pIntent);
//        }
        Log.i(tag, "开始定时上传任务...");
        UploadUtil.uploadSensorData();
        UploadUtil.uploadSensorData();
        AlarmUtil.getInstance(getApplicationContext()).getUpAlarmManagerWorkOnOthers();

        return super.onStartCommand(intent, flags, startId);
    }

    public static class AlarmReceive extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(tag, "接收到系统广播，准备开始上传...");
            //这里模拟后台操作
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.e("wj", "循环执行了，哈哈." + System.currentTimeMillis());
                }
            }).start();

            //循环启动Service
//            Intent i = new Intent(context, UploadService.class);
//            context.startService(i);
        }
    }
}
