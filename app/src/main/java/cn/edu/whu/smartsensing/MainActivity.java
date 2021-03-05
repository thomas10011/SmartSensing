package cn.edu.whu.smartsensing;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cn.edu.whu.smartsensing.listener.CustomSensorEventListener;
import cn.edu.whu.smartsensing.service.McDataCallbackService;
import cn.edu.whu.smartsensing.service.SensorService;
import cn.edu.whu.smartsensing.service.UploadService;
import cn.edu.whu.smartsensing.util.AlarmUtil;
import cn.edu.whu.smartsensing.util.FileUtil;
import cn.edu.whu.smartsensing.util.UploadUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String tag = "Main Activity";

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

    private ListView listView = null;
    private ArrayAdapter<String> adapter = null;
    private TextView unLockText = null;
    private int selectedPosition = 0;

    private DatePickerDialog datePicker = null;

    private List<String> mcData = new ArrayList<>();


    Intent sensorIntent;
    private SensorService sensorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FileUtil.generateUUID(this.getExternalFilesDir("").toString() + "/");
        FileUtil.readFile(this.getExternalFilesDir("").toString(), "/" + "info");
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

        listView = findViewById(R.id.recordList);
        // 允许再主线程中进行网络请求
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
        mcData = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(
                MainActivity.this, android.R.layout.simple_list_item_single_choice, mcData
        );
        listView.setOnItemClickListener(
                (parent, view, position, id) -> {
                    Log.d("Main Activity", "选中第" + position + "条数据");
                    selectedPosition = position;
                }
        );

        listView.setAdapter(adapter);
        listView.setSelection(0);
        McDataCallbackService.registerMcData(mcData, adapter);
        McDataCallbackService.registerActivity(this);
        UploadUtil.getMcData();
        unLockText = findViewById(R.id.unLockText);

        // 获取传感器服务
        mSensorMgr = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        // 启动定时上传任务
//        Intent intent = new Intent(this, UploadService.class);
//        startService(intent);
        AlarmUtil alarmUtil = AlarmUtil.getInstance(this);
        alarmUtil.createGetUpAlarmManager();
        alarmUtil.getUpAlarmManagerStartWork();
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
                Log.i("Main Activity", "-----------用户主动上传文件---------");
                UploadUtil.uploadSensorData();
                UploadUtil.uploadMcData(mcData);
            }
            else if(v.getId() == R.id.add_record) {
                Log.d("Main Activity", "添加数据");
                addMcDataRecord();
            }
            else if (v.getId() == R.id.delete_record) {
                deleteMcData();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addMcDataRecord() {

        datePicker = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        LocalDate date = LocalDate.of(year, month + 1, dayOfMonth);
                        Log.d("TAG", "onDateSet: " + date.toString());
                        mcData.add(date.toString());adapter.notifyDataSetChanged();
                    }
                },
                2020,5,30
            );
        datePicker.show();
    }

    public void deleteMcData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage("确认要删除第" + (selectedPosition + 1) + "条记录吗？");
        builder.setCancelable(true);
        builder.setPositiveButton("确定",
                (dialog, which) -> {
                    Log.d("Main Activity", "删除第" + selectedPosition + "条数据");
                    if (!mcData.isEmpty()) {
                        mcData.remove(selectedPosition);adapter.notifyDataSetChanged();
                    }

        });
        builder.setNegativeButton("取消", (dialog, which) -> { });
        builder.create().show();

    }

    public void updateMcData(List<String> data) {
        mcData.addAll(data);
        adapter.notifyDataSetChanged();
    }


}