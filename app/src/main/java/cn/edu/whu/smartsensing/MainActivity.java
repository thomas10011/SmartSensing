package cn.edu.whu.smartsensing;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.time.LocalDateTime;

import cn.edu.whu.smartsensing.listener.CustomSensorEventListener;
import cn.edu.whu.smartsensing.service.SensorService;
import cn.edu.whu.smartsensing.util.UploadUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private SensorManager mSensorMgr;
    // xyz方向的加速度
    private TextView tvx = null;
    private TextView tvy = null;
    private TextView tvz = null;

    // xyz方向的角速度
    private TextView gvx = null;
    private TextView gvy = null;
    private TextView gvz = null;

    private TextView ortx = null;
    private TextView orty = null;
    private TextView ortz = null;

    private TextView unLockText = null;


    Intent sensorIntent;
    private SensorService sensorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button bt = findViewById(R.id.bt_start);
        bt.setOnClickListener(this);

        Button bt_stop = findViewById(R.id.bt_stop);
        bt_stop.setOnClickListener(this);

        // 注册text view
        tvx = findViewById(R.id.tvx);
        tvy = findViewById(R.id.tvy);
        tvz = findViewById(R.id.tvz);

        gvx = findViewById(R.id.gvx);
        gvy = findViewById(R.id.gvy);
        gvz = findViewById(R.id.gvz);

        ortx = findViewById(R.id.ort3);
        orty = findViewById(R.id.ort2);
        ortz = findViewById(R.id.ort1);

        unLockText = findViewById(R.id.unLockText);

        // 获取传感器服务
        mSensorMgr = (SensorManager)getSystemService(Context.SENSOR_SERVICE);


    }

    private final ServiceConnection connection = new ServiceConnection() {
        // 可交互的后台服务与普通服务的不同之处，就在于这个connection建立起了两者的联系
        @Override
        public void onServiceDisconnected(ComponentName name) {
            sensorService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            sensorService = ((SensorService.SensorServiceBinder) service).getService(); // 获取service实例
            sensorService.setCustomSensorEventListener (
                    new CustomSensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent sensorEvent, float[] values) {
                            switch (sensorEvent.sensor.getType()) {
                                case Sensor.TYPE_LINEAR_ACCELERATION:
                                    tvx.setText("加速度x：\n" + values[0]);
                                    tvy.setText("加速度y：\n" + values[1]);
                                    tvz.setText("加速度z：\n" + values[2]);
                                    break;
                                case Sensor.TYPE_GYROSCOPE:
                                    gvx.setText("角速度x：\n" + values[0]);
                                    gvy.setText("角速度y：\n" + values[1]);
                                    gvz.setText("角速度z：\n" + values[2]);
                                    break;
                                // Get readings from accelerometer and magnetometer. To simplify calculations,
                                // consider storing these readings as unit vectors.
                                case Sensor.TYPE_ACCELEROMETER:
                                    ortx.setText("倾斜角x：\n" + values[0]);
                                    ortx.setText("倾斜角y：\n" + values[1]);
                                    ortx.setText("倾斜角z：\n" + values[2]);
                                    break;
                                case Sensor.TYPE_MAGNETIC_FIELD:
                                    ortx.setText("倾斜角x：\n" + values[0]);
                                    orty.setText("倾斜角y：\n" + values[1]);
                                    ortz.setText("倾斜角z：\n" + values[2]);
                                    break;
                                default:
                                    break;
                            }
                        }

                        @Override
                        public void onUnLockTimeChanged(Long time) {
                            unLockText.setText("上次解锁花费时间：" + time);
                        }
                    }
            );
        }// onServiceConnected()方法关键，在这里实现对服务的方法的调用
    };


    protected void onPause()
    {
        super.onPause();
    }

    protected void onResume()
    {
        super.onResume();
    }

    protected void onStop()
    {
        super.onStop();

    }


    @Override
    public void onClick(View v)
    {
        try {
            if(v.getId() == R.id.bt_start)
            {

                sensorIntent = new Intent(this, SensorService.class);
                startForegroundService(sensorIntent);
                bindService(sensorIntent, connection, BIND_AUTO_CREATE);


            }
            else if(v.getId()== R.id.bt_stop)
            {
                Log.i("Main Activity", "-----------停止记录数据---------");
                unbindService(connection);
                stopService(sensorIntent);

            }
            else if(v.getId() == R.id.bt_upload) {
                Log.i("Main Activity", "-----------准备开始上传文件---------");
                UploadUtil.uploadSensorData();

            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}