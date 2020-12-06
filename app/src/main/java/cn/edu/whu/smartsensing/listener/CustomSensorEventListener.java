package cn.edu.whu.smartsensing.listener;

import android.hardware.SensorEvent;

public interface CustomSensorEventListener {

    // activity供sensor service回调
    void onSensorChanged(SensorEvent sensorEvent, float[] values);

    void onUnLockTimeChanged(Long time);

}
