    package cn.edu.whu.smartsensing.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import cn.edu.whu.smartsensing.R;
import cn.edu.whu.smartsensing.listener.CustomSensorEventListener;
import cn.edu.whu.smartsensing.util.FileUtil;

public class SensorService extends Service implements SensorEventListener {

    private String dataFileName;
    private String audioFileName;
    private String audioFilesDir;
    private String accelerationFilesDir;
    private String unLockTimeFilesDir;

    private String screenStatus;

    private SensorManager mSensorMgr;
    private MediaPlayer mediaPlayer;

    // 开始记录的时间
    private Long recordStartTime;
    // 上一次记录的时间
    private LocalDateTime lastRecordTime;
    // 上一次屏幕电量时间
    private LocalDateTime lastScreenOnTime;

    private final int ACCELERATION_DELAY = 1000000;   // 10ms

    // 加速度数据
    private float accelerationX;
    private float accelerationY;
    private float accelerationZ;

    // 角速度数据
    private float gyroscopeX;
    private float gyroscopeY;
    private float gyroscopeZ;


    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    File recordingFile;//储存AudioRecord录下来的文件
    boolean isRecordingAudio = false; //true表示正在录音
    boolean isRecordingData = true; // 是否记录传感器数据
    AudioRecord audioRecord = null;
    File parent = null;//文件目录
    int bufferSize = 0;//最小缓冲区大小
    int sampleRateInHz = 48000;//采样率
    int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO; //单声道
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT; //量化位数
    String TAG = "AudioRecord";

    PowerManager.WakeLock wakeLock;
    LockScreenReceiver lockScreenReceiver;
    TelephonyManager telephonyManager;
    TelephoneListener telephoneListener;

    CustomSensorEventListener sensorEventListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return new SensorServiceBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        // 申请wakelock
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, SensorService.class.getName());
        wakeLock.acquire(24*6*10*60*1000L /* 24*6*10 minutes*/);
        // 获取传感器服务
        mSensorMgr = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        // 初始化播放器
        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.silent);
        mediaPlayer.setLooping(true);

        // 先取消注册传感器
        mSensorMgr.unregisterListener(this,
                mSensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
        mSensorMgr.unregisterListener(this,
                mSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE));

        // 重新注册传感器
        mSensorMgr.registerListener(this,
                mSensorMgr.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorMgr.registerListener(this,
                mSensorMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        Sensor accelerometer = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            mSensorMgr.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = mSensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            mSensorMgr.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

        lastRecordTime = LocalDateTime.now();
        recordStartTime = lastRecordTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
        // 时间戳加data.txt
        dataFileName = recordStartTime + "-" + "data.txt";
//        audioFileName = lastRecordTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "-" + "audio.pcm";


        accelerationFilesDir = this.getExternalFilesDir("").toString() + "/AccelerationRecord/";
        audioFilesDir = this.getExternalFilesDir("").toString()+ "/AudioRecord/";
        unLockTimeFilesDir = this.getExternalFilesDir("").toString()+ "/UnLockRecord/";

        //生成文件夹之后，再生成文件，不然会出错
        FileUtil.makeFile(accelerationFilesDir, dataFileName);

        // 初始化音频记录
        initAudioRecord();
        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephoneListener = new TelephoneListener();
        telephonyManager.listen(telephoneListener, PhoneStateListener.LISTEN_CALL_STATE);

        // 锁屏状态相关类
        lockScreenReceiver = new LockScreenReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(lockScreenReceiver, filter);

        // recordAudio();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager manager = (NotificationManager)getSystemService (NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("123456","mc tracking", NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification =
                new Notification.Builder(this, "123456")
                        .setContentTitle("MC Tracking")
                        .setContentText("正在后台记录数据")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pendingIntent)
                        .setTicker("ticker")
                        .build();

        // Notification ID cannot be 0.
        startForeground(100, notification);
        //        return super.onStartCommand(intent, flags, startId);
        // 开始播放音乐
        new Thread(this::startPlayMusic).start();
        System.out.println("调用了start方法");
        return Service.START_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
        System.out.println("调用了destroy方法");

        stopPlayMusic();
        stopRecordAudio();
        sensorEventListener = null;
        // 申请了WakeLock要记得释放，否则手机可能无法进入休眠状态
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        unregisterReceiver(lockScreenReceiver);
//        // 在onDestory方法中重新启动自己，即Service被销毁时调用onDestory，就执行重新启动代码。

        isRecordingData = false;
        boolean result = mSensorMgr.flush(this);
        if (result) { Log.i("sensor service", "flush成功"); }
        else { Log.i("sensor service", "flush失败"); }
        // stopRecordAudio();

        stopSelf();



    };

    // 初始化音频记录
    public void initAudioRecord() {

        // 计算最小缓冲区
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        // 创建AudioRecorder对象
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioFormat, bufferSize);

        parent = FileUtil.makeFileDirectory(this.getExternalFilesDir("").toString()+ "/AudioRecord/");


    }

    private void startPlayMusic(){
        if(mediaPlayer != null){
            mediaPlayer.start();
        }
    }

    private void stopPlayMusic(){
        if(mediaPlayer != null){
            mediaPlayer.stop();
        }
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (!isRecordingData) { return; }
        float[] values = sensorEvent.values;
        // 根据传感器类型更新数据
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:

                accelerationX = values[0];
                accelerationY = values[1];
                accelerationZ = values[2];
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroscopeX = values[0];
                gyroscopeY = values[1];
                gyroscopeZ = values[2];
                break;

            // Get readings from accelerometer and magnetometer. To simplify calculations,
            // consider storing these readings as unit vectors.
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(sensorEvent.values, 0, accelerometerReading, 0, accelerometerReading.length);
                // 更新角度数据
                updateOrientationAngles();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(sensorEvent.values, 0, magnetometerReading, 0, magnetometerReading.length);
                // 更新角度数据
                updateOrientationAngles();
                break;

            default:
                break;

        }

        // 微秒数
        Long lastRecordMilliSecond = lastRecordTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
        LocalDateTime now = LocalDateTime.now();
        Long nowMilliSecond = now.toInstant(ZoneOffset.of("+8")).toEpochMilli();

        // 距离上次记录时间过去了10ms则进行记录 同时更新上次记录时间
        if (nowMilliSecond - lastRecordMilliSecond >= 20) {
            // 记录时间差
            double duration = (nowMilliSecond - recordStartTime) / (1000.0 * 3600);
            // 写到txt文件中
            writeData(accelerationX + "," + accelerationY + "," + accelerationZ + "," + gyroscopeX + "," + gyroscopeY + "," + gyroscopeZ + "," + orientationAngles[0] + "," + orientationAngles[1] + "," + orientationAngles[2] + "," + nowMilliSecond + "," + String.format("%.2f", duration));
            lastRecordTime = now;
            // 回调
            if (sensorEventListener != null) {
                sensorEventListener.onSensorChanged(sensorEvent, values);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void writeData(String data) {
        FileUtil.writeTxtToFile(data, accelerationFilesDir, dataFileName);
    }

    private void writeUnLockTime(Duration duration) {
        FileUtil.writeTxtToFile(Long.toString(duration.toMillis()), unLockTimeFilesDir, "record.txt");
    }


    //开始录音
    public void recordAudio() {
        isRecordingAudio = true;
        new Thread(() -> {
            isRecordingAudio = true;

            recordingFile = FileUtil.makeFile(audioFilesDir, generateAudioFileName());

            try {
                DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(recordingFile)));
                byte[] buffer = new byte[bufferSize];
                audioRecord.startRecording();//开始录音
                int r = 0;
                while (isRecordingAudio) {
                    int bufferReadResult = audioRecord.read(buffer,0,bufferSize);
                    for (int i = 0; i < bufferReadResult; i++)
                    {
                        dos.write(buffer[i]);
                    }
                    r++;
                }
                audioRecord.stop(); //停止录音
                dos.close();
            } catch (Throwable t) {
                Log.e(TAG, "Recording Failed");
                t.printStackTrace();
            }

        }).start();

    }

    //停止录音
    public void stopRecordAudio()
    {
        isRecordingAudio = false;
    }

    // Compute the three orientation angles based on the most recent readings from
    // the device's accelerometer and magnetometer.
    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
        // "mRotationMatrix" now has up-to-date information.

        SensorManager.getOrientation(rotationMatrix, orientationAngles);
        // "mOrientationAngles" now has up-to-date information.
//        System.out.println(Arrays.toString(orientationAngles));

    }

    // 接收屏幕状态通知
    private class LockScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (intent != null) {
                    String change = "";
                    if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                        // 更新屏幕点亮时间
                        lastScreenOnTime = LocalDateTime.now();
                        change = "亮屏";
                    } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                        change = "锁屏";
                    } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                        Duration duration = Duration.between(lastScreenOnTime, LocalDateTime.now());
                        writeUnLockTime(duration);
                        if (sensorEventListener != null) {
                            sensorEventListener.onUnLockTimeChanged(duration.toMillis());
                        }
                        change = "解锁";
                    }
                    screenStatus = change;
                    System.out.println(change);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class TelephoneListener extends PhoneStateListener {

        //在电话状态改变的时候调用
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    //空闲状态
                    // stopRecordAudio();
                    System.out.println("电话空闲");
                    break;

                case TelephonyManager.CALL_STATE_RINGING:
                    //响铃状态  需要在响铃状态的时候初始化录音服务
                    System.out.println("来电响铃");
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // 摘机状态（接听）
                    // recordAudio();
                    System.out.println("接听电话");
                    break;
            }
        }

    }

    public class SensorServiceBinder extends Binder {
        public SensorService getService() {
            return SensorService.this;
        }
    }

    private String generateAudioFileName() {
        return audioFileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "-" + "audio.pcm";
    }

    public void setCustomSensorEventListener(CustomSensorEventListener listener) {
        sensorEventListener = listener;
    }

}
