package com.zyj.morseapp.pages;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.zyj.morseapp.R;
import com.zyj.morseapp.application.MyApplication;
import com.zyj.morseapp.audio.MorseAudio;
import com.zyj.morseapp.morsecoder.MorseLongCoder;
import com.zyj.morseapp.morsecoder.MorseShortCoder;
import com.zyj.morseapp.subcontract.HalfDuplexMode;
import com.zyj.morseapp.subcontract.LongCodeMessage;
import com.zyj.morseapp.subcontract.ShortCodeMessage;
import com.zyj.morseapp.utils.ArraysUtils;
import com.zyj.morseapp.utils.AudioRecordUtils;
import com.zyj.morseapp.utils.FileUtils;
import com.zyj.morseapp.utils.HalfDuplexUtils;
import com.zyj.morseapp.utils.MessageUtils;
import com.zyj.morseapp.utils.PcmToWavUtils;
import com.zyj.morseapp.utils.StringUtils;
import com.zyj.morseapp.utils.ptt.MyAudio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 录音并存储
 */
public class HalfDuplex extends AppCompatActivity {
    private static Context context = MyApplication.getContext();
    // 按钮
    private static Button send_button = null;
    private static Button start_half_duplex = null;

    // 输入文本框
    public static EditText longCodeDeviceIdInput = null;
    public static EditText longCodeGidInput = null;
    public static EditText longCodeGNumInput = null;
    public static EditText longCodeSpeedInput = null;
    public static EditText longCodeLevel = null;
    public static EditText longCodeMMDD = null;
    public static EditText longCodeHHMM = null;
    public static EditText longCodeOther = null;
    public static EditText maxGLenInput = null;

    public static EditText shortCodeInput = null;
    public static EditText shortCodeSpeedInput = null;
    public static EditText input_text = null;

    //输出文本框
    public static TextView output_text = null;

    private HalfDuplexUtils halfDuplexUtils;

    // 码速（words per minute）
    public static int longCodeWpm = 25;
    public static int shortCodeWpm = 35;
    public static int recLongCodeWpm = longCodeWpm;
    public static int maxGLen = 5;
    // 返回内容是长码或短码
    public static boolean recIsShortCode = false;
    public static boolean recIsLongCode = false;

    // 建立连接的设备号
    String lastSrcDeviceId = "-1";

    // 是否准备关闭录音，发送音频
    boolean recordingIsReadyClose = false;
    // 是否正在进行半双工通信工作
    private int isHalfDuplexWorking = 0;
    // 是否是短码编码，短码-1，长码-0
    private int encoder = 1;
    // 短码译码器
    private MorseShortCoder morseShortCoder = new MorseShortCoder();
    // 长码译码器
    private MorseLongCoder morseLongCoder = new MorseLongCoder();

    private int maxAttempts = 40; // 长码轮询的最大次数

    // 前导码组数
    public static int preambleNum = 1;

    private final long timeout = 30; // 超时时间：3000ms

    // 存储长码和短码的CRC内容
    public static String shortCrcCode = null;
    public static String longCrcCode = null;

    // 定义状态量
    public volatile static boolean sendLongCodeMessage1 = false; // 发端长码报文格式1
    public volatile static boolean sendShortCodeMessage2 = false; // 发端短码报文格式2
    public volatile static boolean recLongCodeMessage1 = false; // 收端长码报文格式1
    public volatile static boolean recLongCodeMessage2 = false; // 收端长码报文格式2
    public volatile static boolean connectStatus = false; // 建立连接的状态

    public volatile static int sendLongCodeTime = 0; // 发端发射长码报文次数

    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    private static final Object lock3 = new Object();

    public static volatile String rec_content = "";
    String myDeviceId;

    public static MediaPlayer mediaPlayer;

    public static int gussianNoise = 4000;

    private boolean isSelfAdaptionWpm;
    private boolean isPlayAudio;



    //// 分包发送
    // 短码发送的线程池，控制多包的顺序发送
    ExecutorService multiShortCodePkgSendingExec = Executors.newSingleThreadScheduledExecutor();

    // 发送短码缓冲区
    List<ShortCodeMessage> shortCodeMessageCacheForSend;
    List<String> shortCodeMessageContentCacheForSend;
    List<String> shortMorseCacheForSend;
    int[] shortMorseCheckFlag;

    // 长码对象
    LongCodeMessage longCodeMessage;
    // 长码内容
    String longCodeMessageContent;
    String longMorseContent;



    ////接收
    // 接收到的短码crc列表
    List<String> shortCrcListForRec;
    // 接收到的短码正文(id, text)
    HashMap<String, String> shortCodeTextMap = new HashMap<>();
    // 接收空报文的次数
    int emptyShortCodeTime = 0;
    // 最大次数
    int maxEmptyShortCodeTime = 3;

    HalfDuplexMode mode;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_half_duplex);

        //获取设备ID并显示
        try {
            myDeviceId = FileUtils.readTxt(getExternalFilesDir("").getAbsolutePath() + "/deviceId.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        setTitle("设备ID：" + myDeviceId);
        longCodeDeviceIdInput = findViewById(R.id.deviceId_input);
        longCodeGidInput = findViewById(R.id.Gid_input);
        longCodeGNumInput = findViewById(R.id.GNum_input);
        longCodeSpeedInput = findViewById(R.id.longCode_speed_input);
        longCodeLevel = findViewById(R.id.level);
        longCodeHHMM = findViewById(R.id.hhmm);
        longCodeMMDD = findViewById(R.id.mmdd);
        longCodeOther = findViewById(R.id.other);
        //TODO 新输入框
//        maxGLenInput = findViewById(R.id.);
        shortCodeInput = findViewById(R.id.shortCode_input);
        shortCodeSpeedInput = findViewById(R.id.shortCode_speed_input);


        send_button = findViewById(R.id.send_button);
        start_half_duplex = findViewById(R.id.start_half_duplex);
        output_text = findViewById(R.id.output);
        input_text = findViewById(R.id.shortCode_input);

        MyOnClick myOnClick = new MyOnClick();
        send_button.setOnClickListener(myOnClick);
        start_half_duplex.setOnClickListener(myOnClick);

        halfDuplexUtils = new HalfDuplexUtils();

        mediaPlayer = new MediaPlayer();

        output_text.setMovementMethod(ScrollingMovementMethod.getInstance());
        output_text.setMaxLines(Integer.MAX_VALUE); // 设置足够大的行数以容纳所有文本
        output_text.setScrollContainer(true);
        output_text.setFocusable(true);
        output_text.setSelected(true);


        input_text.setMovementMethod(ScrollingMovementMethod.getInstance());
        input_text.setMaxLines(Integer.MAX_VALUE); // 设置足够大的行数以容纳所有文本
        input_text.setScrollContainer(true);
        input_text.setFocusable(true);
        input_text.setSelected(true);

        // 获取超时时间
        maxAttempts = Sockets.expiredTime;

        // 避免父布局拦截滑动事件
        output_text.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });

        input_text.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });

        // 信噪比下拉框
        // 在你的Activity或Fragment中
        Spinner spinner = findViewById(R.id.snr_spinner);
        // 定义下拉列表的选项
        String[] items = new String[] {"无噪声", "噪声低", "噪声中","噪声高"};
        // 创建适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);

        // 设置下拉列表的下拉样式（可选）
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // 将适配器设置给Spinner
        spinner.setAdapter(adapter);
        spinner.setSelection(2);
        // 设置监听器
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 获取选中项的文本
                String selectedItemText = (String) parent.getItemAtPosition(position);
                // 根据选中项做进一步处理
                System.out.println(selectedItemText);
                switch (selectedItemText) {
                    case "无噪声":
                        gussianNoise = 0;
                        break;
                    case "噪声低":
                        gussianNoise = 4000;
                        break;
                    case "噪声中":
                        gussianNoise = 8000;
                        break;
                    case "噪声高":
                        gussianNoise = 12000;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 可以在没有选项被选中时做一些处理
            }
        });

        //设置发送报文的默认值
        String deviceIdTxt = context.getExternalFilesDir("").getAbsolutePath()+"/deviceId.txt";
        String deviceId = "0";
        try {
            if(!FileUtils.fileExist(deviceIdTxt)){
                FileUtils.writeTxt(deviceIdTxt, "1");
            }
            deviceId = FileUtils.readTxt(deviceIdTxt);
        } catch (IOException e) {
            e.printStackTrace();
        }
        changeLongCodeGidInput(deviceId);
        changeLongCodeGNumInput(deviceId);
        changelongCodeLevel(deviceId);
        changeLongCodeOther(deviceId);

        // 创建文件
        try {
            if(!FileUtils.fileExist(isSaveWavPath)){
                FileUtils.writeTxt(isSaveWavPath, "1");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshLogView(String msg){
        output_text.post(new Runnable() {
            @Override
            public void run() {
                output_text.append(msg);

                Layout layout = output_text.getLayout();
                // 获取最后一行的底部
                int desired = layout.getLineTop(output_text.getLineCount());
                // 获取空间的顶部padding和底部padding
                int padding = output_text.getCompoundPaddingTop() + output_text.getCompoundPaddingBottom();
                // 二者相加才是真正需要定位的位置
                // 作比较，需要跳转才跳转
                if ((desired + padding) > output_text.getHeight()) {
                    output_text.scrollTo(0, desired + padding - output_text.getHeight());
                }
            }
        });
    }

    public static void show(String str) {
        output_text.post(new Runnable() {
            @Override
            public void run() {
                output_text.setText(str);
            }
        });
    }

    public static void changeLongCodeGidInput(String str) {
        output_text.post(new Runnable() {
            @Override
            public void run() {
                longCodeGidInput.setText(str);
            }
        });
    }

    public static void changeLongCodeGNumInput(String str) {
        output_text.post(new Runnable() {
            @Override
            public void run() {
                longCodeGNumInput.setText(str);
            }
        });
    }

    public static void changelongCodeLevel(String str) {
        output_text.post(new Runnable() {
            @Override
            public void run() {
                longCodeLevel.setText(str);
            }
        });
    }

    public static void changeLongCodeOther(String str) {
        output_text.post(new Runnable() {
            @Override
            public void run() {
                longCodeOther.setText(str);
            }
        });
    }

    /**
     * 发送长码报文
     */
    private class SendLongCodeAsyncTask implements Runnable {
        private String content;
        private int wpm;

        public SendLongCodeAsyncTask(String content, int wpm) {
            this.content = content;
            this.wpm = wpm;
        }

        @Override
        public void run() {
            try {
                isSelfAdaptionWpm = FileUtils.readTxt(context.getExternalFilesDir("").getAbsolutePath()+"/isSelfAdaptionWpm.txt").equals("1");
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("【发送长码报文线程】启动！");
            System.out.println("【发送长码报文线程】正在进行第一次长码发送...");
            refreshLogView("[1]第一次发送连接报文...");
            refreshLogView("\n");
            // 发送过程，关闭录音
            stopRecording();
            // 发送报文
            sendLongMorseCode(content, wpm);
            // 播放完成后继续录音
            synchronized (lock1) {
                try {
                    lock1.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 如果接收到长码响应，则建立连接成功，呼叫线程结束
            if (recLongCodeMessage1) {
                System.out.println("【发送长码报文线程】第一次长码发送，线程终止！");
                return;
            }


            System.out.println("【发送长码报文线程】正在进行第二次长码发送...");
            refreshLogView("[1]第二次发送连接报文...");
            refreshLogView("\n");

            // 发送过程，关闭录音
            stopRecording();
            // 发送报文
            int tempWpm = wpm;
            if (isSelfAdaptionWpm) {
                tempWpm = Math.max(tempWpm - 5, 15);
                System.out.println("tempWpm" + tempWpm);
            }
            sendLongMorseCode(content, tempWpm);
            // 播放完成后继续录音
            synchronized (lock1) {
                try {
                    lock1.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 如果接收到长码响应，则建立连接成功，呼叫线程结束
            if (recLongCodeMessage1) {
                System.out.println("【发送长码报文线程】第二次长码发送，线程终止！");
                return;
            }

            System.out.println("【发送长码报文线程】正在进行第三次长码发送...");
            refreshLogView("[1]第三次发送连接报文...");
            refreshLogView("\n");

            // 发送过程，关闭录音
            stopRecording();
            // 发送报文
            if (isSelfAdaptionWpm) {
                tempWpm = Math.max(tempWpm - 5, 15);
                System.out.println("tempWpm" + tempWpm);
            }
            sendLongMorseCode(content, tempWpm);
            // 播放完成后继续录音
            System.out.println("【发送长码报文线程】已发送三次长码，线程终止！");
        }
    }

    /**
     * 发送短码报文
     */
    private class SendShortCodeAsyncTask implements Runnable {
        private int wpm;

        public SendShortCodeAsyncTask(int wpm) {
            this.wpm = wpm;
        }

        @Override
        public void run() {
            synchronized (lock2) {
                try {
                    lock2.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("【发送短码报文线程】启动！");
            // 连接建立失败，不再发送短码，线程结束
            if (!connectStatus) {
                System.out.println("【发送短码报文线程】与收端连接建立失败，线程结束！");
                return;
            }

            // 这里是长时间运行的任务，例如发送摩尔斯电码
            System.out.println("【发送短码报文线程】正在进行第一次短码发送...");
            refreshLogView("[3]第一次发送短码报文...");
            refreshLogView("\n");

            // 发送过程，关闭录音
            stopRecording();
            // 顺序发送分包后的报文
            int count = 0;
            int sendTime = 0;
            for (int i : shortMorseCheckFlag){
                if (i == 1){
                    count++;
                }
            }
            CountDownLatch latchForMultiSend1 = new CountDownLatch(count);
            for (int i = 0; i < shortMorseCacheForSend.size(); i++){
                // 标志位是1，表示需要发送
                if (shortMorseCheckFlag[i] == 1){
                    sendTime++;
                    String str = shortMorseCacheForSend.get(i);
                    String index = String.valueOf(i + 1);
                    boolean lastOne = sendTime == count;
                    multiShortCodePkgSendingExec.submit(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("<=========发送第" + index + "个短码数据包=========>");
                            sendShortMorseCode(str, wpm);
                            if (!lastOne){
                                try {
                                    Thread.sleep(6000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            latchForMultiSend1.countDown();
                        }
                    });
                }
            }

            try {
                latchForMultiSend1.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 恢复录音
            System.out.println("短码报文全部发送完成，恢复录音！");
            onAudioPlaybackComplete();
            HalfDuplex.sendShortCodeMessage2 = true;


            // 播放完成后继续录音
            synchronized (lock2) {
                try {
                    lock2.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 如果接收到长码响应，则建立连接成功，呼叫线程结束
            if (recLongCodeMessage2) {
                System.out.println("【发送短码报文线程】第一次短码发送成功，线程终止！");
                return;
            }


            System.out.println("【发送短码报文线程】正在进行第二次短码发送...");
            refreshLogView("[3]第二次发送短码报文...");
            refreshLogView("\n");

            // 发送过程，关闭录音
            stopRecording();
            // 顺序发送分包后的报文
            count = 0;
            sendTime = 0;
            for (int i : shortMorseCheckFlag){
                if (i == 1){
                    count++;
                }
            }
            CountDownLatch latchForMultiSend2 = new CountDownLatch(count);
            for (int i = 0; i < shortMorseCacheForSend.size(); i++){
                // 标志位是1，表示需要发送
                if (shortMorseCheckFlag[i] == 1){
                    sendTime++;
                    String str = shortMorseCacheForSend.get(i);
                    String index = String.valueOf(i + 1);
                    boolean lastOne = sendTime == count;
                    multiShortCodePkgSendingExec.submit(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("<=========发送第" + index + "个短码数据包=========>");
                            sendShortMorseCode(str, wpm);
                            if (!lastOne){
                                try {
                                    Thread.sleep(6000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            latchForMultiSend2.countDown();
                        }
                    });
                }
            }
            try {
                latchForMultiSend2.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 恢复录音
            System.out.println("短码报文全部发送完成，恢复录音！");
            onAudioPlaybackComplete();
            HalfDuplex.sendShortCodeMessage2 = true;


            synchronized (lock2) {
                try {
                    lock2.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 如果接收到长码响应，则建立连接成功，呼叫线程结束
            if (recLongCodeMessage2) {
                System.out.println("【发送短码报文线程】第二次短码发送成功，线程终止！");
                return;
            }

            System.out.println("【发送短码报文线程】正在进行第三次短码发送...");
            refreshLogView("[3]第三次发送短码报文...");
            refreshLogView("\n");

            // 发送过程，关闭录音
            stopRecording();
            // 顺序发送分包后的报文
            count = 0;
            sendTime = 0;
            for (int i : shortMorseCheckFlag){
                if (i == 1){
                    count++;
                }
            }
            CountDownLatch latchForMultiSend3 = new CountDownLatch(count);
            for (int i = 0; i < shortMorseCacheForSend.size(); i++){
                // 标志位是1，表示需要发送
                if (shortMorseCheckFlag[i] == 1){
                    sendTime++;
                    String str = shortMorseCacheForSend.get(i);
                    String index = String.valueOf(i + 1);
                    boolean lastOne = sendTime == count;
                    multiShortCodePkgSendingExec.submit(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("<=========发送第" + index + "个短码数据包=========>");
                            sendShortMorseCode(str, wpm);
                            if (!lastOne){
                                try {
                                    Thread.sleep(6000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            latchForMultiSend3.countDown();
                        }
                    });
                }
            }

            try {
                latchForMultiSend3.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 恢复录音
            System.out.println("短码报文全部发送完成，恢复录音！");
            onAudioPlaybackComplete();
            HalfDuplex.sendShortCodeMessage2 = true;

            // 播放完成后继续录音
            System.out.println("【发送短码报文线程】已发送三次短码，线程终止！");
        }
    }

    /**
     * 等待长码报文1
     */
    private class WaitLongCode1AsyncTask implements Runnable {
        @Override
        public void run() {
            System.out.println("【等待长码1报文线程】启动！");
            // 第一次发送接收
            System.out.println("【等待长码1报文线程】等待长码报文发送...");
            while (!sendLongCodeMessage1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            refreshLogView("[1]连接报文发送完成！");
            refreshLogView("\n");

            System.out.println("【等待长码1报文线程】监测到长码报文发送完成！");
            // 等待报文响应
            System.out.println("【等待长码1报文线程】等待收端响应");
            refreshLogView("[2]等待连接报文响应...");
            refreshLogView("\n");


            int count = 0; // 轮询计数器
            boolean condition = false; // 用于检查的变量
            while (count < maxAttempts && !condition) {
                System.out.println("【等待长码1报文线程】轮询收端响应: " + (count + 1) + "次");
                // 阻塞并轮询
                try {
                    Thread.sleep(1000); // 休眠1秒
                    // 获取返回内容，没有则为空
                    String res = rec_content;
                    String destDeviceId = StringUtils.getId1FromRecLongCodeMessage1(res);
                    String srcDeviceId = StringUtils.getId2FromRecLongCodeMessage1(res);
                    System.out.println("res:" + res);
                    System.out.println("【等待长码1报文线程】destDeviceId:" + destDeviceId);
                    System.out.println("【等待长码1报文线程】srcDeviceId:" + srcDeviceId);

                    // 如果返回内容的设备号匹配，则进入下一个状态
                    // 启动发送短码的线程 && 启动接收短码的线程
                    // 接受内容判断是该设备
                    if ((destDeviceId.equals(myDeviceId) && rec_content.contains("K"))) {
                        // 收到响应报文后，延时6s，防止报文未接收完
                        refreshLogView("[2]收到连接报文响应！");
                        refreshLogView("\n");
                        Thread.sleep(6000); // 休眠6秒

                        // 唤醒其他线程
                        recLongCodeMessage1 = true;
                        connectStatus = true;
                        System.out.println("【等待长码1报文线程】收到响应长码报文，线程结束！");
                        synchronized (lock1) {
                            lock1.notify();
                        }
                        synchronized (lock3) {
                            lock3.notify();
                        }
                        synchronized (lock2) {
                            lock2.notify();
                        }
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count++; // 增加轮询计数器
            }
            refreshLogView("[2]连接报文未收到响应！");
            refreshLogView("\n");

            // 第一次轮询，30秒没有收到响应
            // 进行第二次发送
            count = 0;
            System.out.println("【等待长码1报文线程】触发第二次长码报文发送");
            synchronized (lock1) {
                lock1.notify();
            }
            // 等待长码报文发送完成
            sendLongCodeMessage1 = false;
            System.out.println("【等待长码1报文线程】等待长码报文发送完成");
            while (!sendLongCodeMessage1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            refreshLogView("[1]连接报文发送完成！");
            refreshLogView("\n");

            System.out.println("【等待长码1报文线程】监测到长码报文发送完成");
            refreshLogView("[2]等待连接报文响应...");
            refreshLogView("\n");

            // 等待报文响应
            while (count < maxAttempts && !condition) {
                System.out.println("【等待长码1报文线程】轮询收端响应: " + (count + 1) + "次");
                // 阻塞并轮询
                try {
                    Thread.sleep(1000); // 休眠2秒
                    // 获取返回内容，没有则为空
                    String res = rec_content;
                    String destDeviceId = StringUtils.getId1FromRecLongCodeMessage1(res);
                    String srcDeviceId = StringUtils.getId2FromRecLongCodeMessage1(res);
                    System.out.println("res" + res);
                    System.out.println("【等待长码1报文线程】destDeviceId:" + destDeviceId);
                    System.out.println("【等待长码1报文线程】srcDeviceId:" + srcDeviceId);

                    // 如果返回内容的设备号匹配，则进入下一个状态
                    // 启动发送短码的线程 && 启动接收短码的线程
                    // 接受内容判断是该设备
                    if ((destDeviceId.equals(myDeviceId) && rec_content.contains("K K"))) {
                        // 收到响应报文后，延时6s，防止报文未接收完
                        refreshLogView("[2]收到连接报文响应！");
                        refreshLogView("\n");
                        Thread.sleep(6000); // 休眠6秒

                        // 唤醒其他线程
                        recLongCodeMessage1 = true;
                        connectStatus = true;
                        System.out.println("【等待长码1报文线程】收到响应长码报文，线程结束！");
                        synchronized (lock1) {
                            lock1.notify();
                        }
                        synchronized (lock3) {
                            lock3.notify();
                        }
                        synchronized (lock2) {
                            lock2.notify();
                        }
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count++; // 增加轮询计数器
            }

            refreshLogView("[2]连接报文未收到响应！");
            refreshLogView("\n");

            // 第二次轮询，40秒没有收到响应
            // 进行第三次发送
            count = 0;
            System.out.println("【等待长码1报文线程】触发第三次长码报文发送！");
            synchronized (lock1) {
                lock1.notify();
            }
            // 等待长码报文发送完成
            sendLongCodeMessage1 = false;
            System.out.println("【等待长码1报文线程】等待长码报文发送完成...");
            while (!sendLongCodeMessage1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("【等待长码1报文线程】监测到长码报文发送完成！");
            refreshLogView("[1]连接报文发送完成！");
            refreshLogView("\n");


            // 等待报文响应
            System.out.println("【等待长码1报文线程】等待收端响应");
            refreshLogView("[2]等待连接报文响应...");
            refreshLogView("\n");

            while (count < maxAttempts && !condition) {
                System.out.println("【等待长码1报文线程】轮询收端响应: " + (count + 1));
                // 阻塞并轮询
                try {
                    Thread.sleep(1000); // 休眠2秒
                    // 获取返回内容，没有则为空
                    String res = rec_content;
                    String destDeviceId = StringUtils.getId1FromRecLongCodeMessage1(res);
                    String srcDeviceId = StringUtils.getId2FromRecLongCodeMessage1(res);
                    System.out.println("【等待长码1报文线程】res:" + res);
                    System.out.println("【等待长码1报文线程】destDeviceId:" + destDeviceId);
                    System.out.println("【等待长码1报文线程】srcDeviceId:" + srcDeviceId);

                    // 如果返回内容的设备号匹配，则进入下一个状态
                    // 启动发送短码的线程 && 启动接收短码的线程
                    // 接受内容判断是该设备
                    if ((destDeviceId.equals(myDeviceId) && rec_content.contains("K K"))) {
                        // 收到响应报文后，延时6s，防止报文未接收完
                        refreshLogView("[2]收到连接报文响应！");
                        refreshLogView("\n");
                        Thread.sleep(6000); // 休眠6秒

                        // 唤醒其他线程
                        recLongCodeMessage1 = true;
                        connectStatus = true;
                        System.out.println("【等待长码1报文线程】收到响应长码报文，线程结束！");
                        synchronized (lock1) {
                            lock1.notify();
                        }
                        synchronized (lock3) {
                            lock3.notify();
                        }
                        synchronized (lock2) {
                            lock2.notify();
                        }
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count++; // 增加轮询计数器
            }

            // 线程结束前，唤醒两个【短码线程】
            synchronized (lock3) {
                lock3.notify();
            }
            synchronized (lock2) {
                lock2.notify();
            }
            // 三次均失败，显示告警信息
            System.out.println("【等待长码1报文线程】已经尝试三次建立连接，失败三次，线程退出！");
            refreshLogView("[2]连接报文未收到响应！");
            refreshLogView("\n");
            refreshLogView("[2]与收端建立连接失败！");
            refreshLogView("\n");


        }
    }

    /**
     * 等待长码报文2
     */
    private class WaitLongCode2AsyncTask implements Runnable {
        @Override
        public void run() {
            synchronized (lock3) {
                try {
                    lock3.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // 连接建立失败，不再发送短码，线程结束
            if (!connectStatus) {
                System.out.println("【等待长码2报文线程】结束！");
                return;
            }

            System.out.println("【等待长码2报文线程】启动！");
            // 第一次发送接收
            System.out.println("【等待长码2报文线程】等待第一次短码报文发送...");
            while (!sendShortCodeMessage2) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("【等待长码2报文线程】监测到短码报文发送完成！");
            refreshLogView("[3]短码报文发送完成！");
            refreshLogView("\n");

            // 等待报文响应
            System.out.println("【等待长码2报文线程】等待收端响应");
            refreshLogView("[4]等待短码报文响应...");
            refreshLogView("\n");

            int count = 0; // 轮询计数器
            boolean condition = false; // 用于检查的变量
            while (count < maxAttempts * 2 && !condition) {
                System.out.println("【等待长码2报文线程】轮询收端响应: " + (count + 1) + "次");
                // 阻塞并轮询
                try {
                    Thread.sleep(1000); // 休眠5秒
                    // 获取返回内容，没有则为空
                    String res = rec_content;
                    String destDeviceId = StringUtils.getId1FromRecLongCodeMessage2(res);
                    String srcDeviceId = StringUtils.getId2FromRecLongCodeMessage2(res);
                    System.out.println("【等待长码2报文线程】res:" + res);
                    System.out.println("【等待长码2报文线程】destDeviceId:" + destDeviceId);
                    System.out.println("【等待长码2报文线程】srcDeviceId:" + srcDeviceId);

                    // 如果返回内容的设备号匹配，则进入下一个状态
                    // 启动发送短码的线程 && 启动接收短码的线程
                    // 接受内容判断是该设备
                    if ((destDeviceId.equals(myDeviceId) && rec_content.contains("K") && rec_content.contains("9801"))) {
                        if (rec_content.contains("OK")){
                            // 接收到短码，关闭发送短码线程
                            recLongCodeMessage2 = true;
                            synchronized (lock2) {
                                lock2.notify();
                            }
                            refreshLogView("[4]收到短码报文响应，发送结束！");
                            refreshLogView("\n");

                            System.out.println("【等待长码2报文线程】收到长码接收报文，线程结束！");
                            return;
                        } else {
                            System.out.println("rec_content:" + rec_content);

                            // 解析响应内容
                            JSONObject object = new JSONObject(rec_content);
                            String talkContent = object.getString("talkContent");
                            System.out.println("talkContent:" + talkContent);
                            List<String> idListFromLongCodeRec2 = MessageUtils.getIDListFromLongCodeRec2(talkContent);
                            System.out.println("收到短码报文响应，不完整的报文id：" + idListFromLongCodeRec2);
                            Arrays.fill(shortMorseCheckFlag, 0);
                            for (String id : idListFromLongCodeRec2){
                                shortMorseCheckFlag[Integer.parseInt(id)] = 1;
                            }
                            refreshLogView("[4]收到短码报文响应，报文不完整，继续接收！");
                            refreshLogView("\n");

                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                count++; // 增加轮询计数器
            }



            // 第一次轮询，30秒没有收到响应
            // 进行第二次发送
            // 等待上次的录音结果清空，避免影响下次解码
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            count = 0;
            synchronized (lock2) {
                lock2.notify();
            }
            // 等待长码报文发送完成
            sendShortCodeMessage2 = false;
            System.out.println("【等待长码2报文线程】等待第二次短码报文发送...");
            while (!sendShortCodeMessage2) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("【等待长码2报文线程】监测到短码报文发送完成");
            refreshLogView("[3]短码报文发送完成！");
            refreshLogView("\n");

            // 等待报文响应
            System.out.println("【等待长码2报文线程】等待收端响应");
            refreshLogView("[4]等待短码报文响应...");
            refreshLogView("\n");

            // 等待报文响应
            while (count < maxAttempts * 2 && !condition) {
                System.out.println("【等待长码2报文线程】轮询收端响应: " + (count + 1) + "次");
                // 阻塞并轮询
                try {
                    Thread.sleep(1000); // 休眠5秒
                    // 获取返回内容，没有则为空
                    String res = rec_content;
                    String destDeviceId = StringUtils.getId1FromRecLongCodeMessage2(res);
                    String srcDeviceId = StringUtils.getId2FromRecLongCodeMessage2(res);
                    System.out.println("【等待长码2报文线程】res:" + res);
                    System.out.println("【等待长码2报文线程】destDeviceId:" + destDeviceId);
                    System.out.println("【等待长码2报文线程】srcDeviceId:" + srcDeviceId);

                    // 如果返回内容的设备号匹配，则进入下一个状态
                    // 启动发送短码的线程 && 启动接收短码的线程
                    // 接受内容判断是该设备
                    if ((destDeviceId.equals(myDeviceId) && rec_content.contains("K K") && rec_content.contains("9801"))) {
                        if (rec_content.contains("OK")){
                            // 接收到短码，关闭发送短码线程
                            recLongCodeMessage2 = true;
                            synchronized (lock2) {
                                lock2.notify();
                            }
                            refreshLogView("[4]收到短码报文响应，发送结束！");
                            refreshLogView("\n");

                            System.out.println("【等待长码2报文线程】收到长码接收报文，线程结束！");
                            return;
                        } else {
                            System.out.println("rec_content:" + rec_content);

                            // 解析响应内容
                            JSONObject object = new JSONObject(rec_content);
                            String talkContent = object.getString("talkContent");
                            System.out.println("talkContent:" + talkContent);
                            List<String> idListFromLongCodeRec2 = MessageUtils.getIDListFromLongCodeRec2(talkContent);
                            System.out.println("收到短码报文响应，不完整的报文id：" + idListFromLongCodeRec2);
                            Arrays.fill(shortMorseCheckFlag, 0);
                            for (String id : idListFromLongCodeRec2){
                                shortMorseCheckFlag[Integer.parseInt(id)] = 1;
                            }
                            refreshLogView("[4]收到短码报文响应，报文不完整，继续接收！");
                            refreshLogView("\n");
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                count++; // 增加轮询计数器
            }

            // 第二次轮询，30秒没有收到响应
            // 进行第三次发送
            // 等待上次的录音结果清空，避免影响下次解码
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count = 0;
            synchronized (lock2) {
                lock2.notify();
            }
            // 等待长码报文发送完成
            sendShortCodeMessage2 = false;
            System.out.println("【等待长码2报文线程】等待第三次短码报文发送...");
            while (!sendShortCodeMessage2) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("【等待长码2报文线程】检测到短码报文发送完成");
            refreshLogView("[3]短码报文发送完成！");
            refreshLogView("\n");

            // 等待报文响应
            System.out.println("【等待长码2报文线程】等待收端响应");
            refreshLogView("[4]等待短码报文响应...");
            refreshLogView("\n");

            // 等待报文响应
            while (count < maxAttempts * 2 && !condition) {
                System.out.println("【等待长码2报文线程】轮询收端响应: " + (count + 1) + "次");
                // 阻塞并轮询
                try {
                    Thread.sleep(1000); // 休眠1秒
                    // 获取返回内容，没有则为空
                    String res = rec_content;
                    String destDeviceId = StringUtils.getId1FromRecLongCodeMessage2(res);
                    String srcDeviceId = StringUtils.getId2FromRecLongCodeMessage2(res);
                    System.out.println("【等待长码2报文线程】res:" + res);
                    System.out.println("【等待长码2报文线程】destDeviceId:" + destDeviceId);
                    System.out.println("【等待长码2报文线程】srcDeviceId:" + srcDeviceId);

                    // 如果返回内容的设备号匹配，则进入下一个状态
                    // 启动发送短码的线程 && 启动接收短码的线程
                    // 接受内容判断是该设备
                    if ((destDeviceId.equals(myDeviceId) && rec_content.contains("K K") && rec_content.contains("9801"))) {
                        if (rec_content.contains("OK")){
                            // 接收到短码，关闭发送短码线程
                            recLongCodeMessage2 = true;
                            synchronized (lock2) {
                                lock2.notify();
                            }
                            refreshLogView("[4]收到短码报文响应，发送结束！");
                            refreshLogView("\n");

                            System.out.println("【等待长码2报文线程】收到长码接收报文，线程结束！");
                            return;
                        } else {
                            System.out.println("rec_content:" + rec_content);

                            // 解析响应内容
                            JSONObject object = new JSONObject(rec_content);
                            String talkContent = object.getString("talkContent");
                            System.out.println("talkContent:" + talkContent);
                            List<String> idListFromLongCodeRec2 = MessageUtils.getIDListFromLongCodeRec2(talkContent);
                            System.out.println("收到短码报文响应，不完整的报文id：" + idListFromLongCodeRec2);
                            Arrays.fill(shortMorseCheckFlag, 0);
                            for (String id : idListFromLongCodeRec2){
                                shortMorseCheckFlag[Integer.parseInt(id)] = 1;
                            }
                            refreshLogView("[4]收到短码报文响应，报文不完整，继续接收！");
                            refreshLogView("\n");
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                count++; // 增加轮询计数器
            }
            // 三次均失败，显示告警信息
            refreshLogView("[4]短码报文未收到响应！");
            refreshLogView("\n");

            refreshLogView("[4]短码报文发送三次，均失败！");
            refreshLogView("\n");


        }
    }

    /**
     * 点击响应
     */
    class MyOnClick implements View.OnClickListener {

        // 发送长码报文
        Thread thread1 = null;
        // 等待长码响应
        Thread thread2 = null;
        // 发送短码报文
        Thread thread3 = null;
        // 等待短码响应
        Thread thread4 = null;

        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                // 发送
                case R.id.send_button:
                    preambleNum = Sockets.preambleNum;
                    if (audioRecord == null) {
                        // 重新初始化AudioRecord
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, recordBufSize);
                        }
                    }
                    String myDeviceId = null;
                    try {
                        myDeviceId = FileUtils.readTxt(getExternalFilesDir("").getAbsolutePath() + "/deviceId.txt");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // 重置状态
                    sendLongCodeMessage1 = false;
                    sendShortCodeMessage2 = false;
                    recLongCodeMessage1 = false;
                    recLongCodeMessage2 = false;
                    connectStatus = false;


                    // 收集发送信息
                    longCodeWpm = Integer.parseInt((longCodeSpeedInput.getText().toString() == null) || (longCodeSpeedInput.getText().toString().equals("")) ? String.valueOf(longCodeWpm) : longCodeSpeedInput.getText().toString());
                    String destDeviceId = longCodeDeviceIdInput.getText().toString() == null || longCodeDeviceIdInput.getText().toString().equals("") ? "0" : longCodeDeviceIdInput.getText().toString();
                    String gid = longCodeGidInput.getText().toString() == null || longCodeGidInput.getText().toString().equals("") ? "0" : longCodeGidInput.getText().toString();
                    String gNum = longCodeGNumInput.getText().toString() == null || longCodeGNumInput.getText().toString().equals("") ? "0" : longCodeGNumInput.getText().toString();
                    String level = longCodeLevel.getText().toString() == null || longCodeLevel.getText().toString().equals("") ? "0" : longCodeLevel.getText().toString();
                    String mmDD = longCodeMMDD.getText().toString() == null || longCodeMMDD.getText().toString().equals("") ? "0820" : longCodeMMDD.getText().toString();
                    String hhMM = longCodeHHMM.getText().toString() == null || longCodeHHMM.getText().toString().equals("") ? "1255" : longCodeHHMM.getText().toString();
                    String other = longCodeOther.getText().toString() == null || longCodeOther.getText().toString().equals("") ? "0" : longCodeOther.getText().toString();

                    shortCodeWpm = Integer.parseInt((shortCodeSpeedInput.getText().toString() == null) || (shortCodeSpeedInput.getText().toString().equals("")) ? String.valueOf(shortCodeWpm) : shortCodeSpeedInput.getText().toString());
//                    maxGLen = (maxGLenInput.getText().toString() == null) || (maxGLenInput.getText().toString().equals("")) ? maxGLen : Integer.parseInt(maxGLenInput.getText().toString());
                    String shortContent = shortCodeInput.getText().toString() == null ? "" : shortCodeInput.getText().toString();
                    // 将shortCode按组数分开
                    List<String> shortCodeGroup = MessageUtils.createShortCodeGroup(shortContent);
                    int gLenSum = MessageUtils.getNoTrimCode(shortContent).size();
                    MessageUtils.setGLen(3);
                    System.out.println("每包最大长度：" + MessageUtils.getGLen());

                    // 构建短码报文的发送缓冲区
                    List<String> shortCodeCrcList = new ArrayList<>();
                    shortCodeMessageCacheForSend = new ArrayList<>();
                    shortCodeMessageContentCacheForSend = new ArrayList<>();
                    shortMorseCacheForSend = new ArrayList<>();
                    for (int i = 0; i < shortCodeGroup.size(); i++){
                        ShortCodeMessage shortCodeMessage = new ShortCodeMessage(String.valueOf(i), String.valueOf(MessageUtils.getNoTrimCode(shortCodeGroup.get(i)).size()), shortCodeGroup.get(i));
                        shortCodeCrcList.add(MessageUtils.getCRC16(shortCodeMessage.getShortCodeText()));
                        shortCodeMessageCacheForSend.add(shortCodeMessage);
                        String shortCodeMessageContent = shortCodeMessage.getShortCodeMessage();
                        shortCodeMessageContentCacheForSend.add(shortCodeMessageContent);
                        shortMorseCacheForSend.add(morseShortCoder.encode(shortCodeMessageContent));
                    }
                    shortMorseCheckFlag = new int[shortCodeGroup.size()];
                    // 初始化后全为1，代表全部发送
                    Arrays.fill(shortMorseCheckFlag, 1);
                    // 构建长码报文
                    longCodeMessage = new LongCodeMessage(myDeviceId, destDeviceId, String.valueOf(0), String.valueOf(gLenSum),
                            String.valueOf(shortCodeMessageCacheForSend.size()), shortCodeCrcList);
                    longCodeMessageContent = longCodeMessage.getLongCodeMessage();
                    longMorseContent = morseLongCoder.encode(longCodeMessageContent);

                    maxAttempts = Sockets.expiredTime;
                    try {
                        isPlayAudio = FileUtils.readTxt(context.getExternalFilesDir("").getAbsolutePath()+"/isPlayAudio.txt").equals("1");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // 发送长码报文
                    Thread thread1 = new Thread(new SendLongCodeAsyncTask(longMorseContent, longCodeWpm));
                    // 等待长码响应
                    Thread thread2 = new Thread(new WaitLongCode1AsyncTask());
                    // 发送短码报文
                    Thread thread3 = new Thread(new SendShortCodeAsyncTask(shortCodeWpm));
                    // 等待短码响应
                    Thread thread4 = new Thread(new WaitLongCode2AsyncTask());
                    thread1.start();
                    thread2.start();
                    thread3.start();
                    thread4.start();
                    break;
                case R.id.start_half_duplex:
                    // 半双工通信按钮
                    if (isHalfDuplexWorking == 0) {
                        isHalfDuplexWorking = 1;
                        Toast.makeText(HalfDuplex.this,"半双工通信开启！",Toast.LENGTH_LONG).show();
                        // 初始化AudioRecord并开始录音
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, recordBufSize);
                        }

                        startRecording();
                        start_half_duplex.setText("停止半双工通信");

//                        audioRecord.startRecording();
//                        start_half_duplex.setText("停止半双工通信");
//                        // 开始定时读取缓冲区的任务
//                        handler.post(readBufferTask);
                        try {
                            refreshIsPlayAudio();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                    else {
                        isHalfDuplexWorking = 0;
                        start_half_duplex.setText("启动半双工通信");
                        stopRecording();
                        Toast.makeText(HalfDuplex.this,"半双工通信结束！",Toast.LENGTH_LONG).show();
                    }
                    maxAttempts = Sockets.expiredTime;
                    break;
                default:
                    break;
            }
        }
    }



    private AudioRecord audioRecord;
    // 音频源：音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    // 采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 8000;
    // 声道设置：android支持双声道立体声和单声道。MONO单声道，STEREO立体声
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    // 采样深度：16bit
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static int recordBufSize = 16000*4;
    private Handler handler = new Handler();
    private Handler receiveHandler = new Handler();

    private static byte data[] = new byte[recordBufSize];
    private static byte dataNew[] = new byte[recordBufSize];
    private volatile boolean isRecording = true;

    // 录音数据
    List<byte[]> allRecordArrays = new ArrayList<>();
    private PcmToWavUtils tool = new PcmToWavUtils(AUDIO_SAMPLE_RATE,AUDIO_CHANNEL,AUDIO_ENCODING);
    private AudioRecordUtils audioRecordUtils = new AudioRecordUtils();
    private String isSaveWavPath = context.getExternalFilesDir("").getAbsolutePath()+"/isSaveWav.txt";

    private String content = "";
    private StringBuilder shortCodeContentForDisplay = new StringBuilder();
    private boolean playAudio1 = false;
    private boolean playAudio2 = false;

    private String morseStr1 = "";
    private String morseStr2 = "";
    private String recv_content1 = "";
    private String recv_content2 = "";

    private Runnable readBufferTask = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                // 读取音频数据的代码
                // 从音频硬件读取音频数据，以便记录到字节数组中。
                if (Objects.isNull(audioRecord)) {
                    return ;
                }
                audioRecord.read(data, 0, recordBufSize);

                // 记录所有录音数据
                byte[] copiedData = new byte[recordBufSize];
                byte[] newCopiedData = new byte[recordBufSize];
                System.arraycopy(data, 0, copiedData, 0, recordBufSize);
                //调整音量大小
                amplifyPCMData(copiedData, copiedData.length, newCopiedData,16, (float) Math.pow(10, (double)5 / 20));
                allRecordArrays.add(newCopiedData);

                //调整音量大小
                amplifyPCMData(data, data.length, dataNew,16, (float) Math.pow(10, (double)5 / 20));
                short[] shortData = ArraysUtils.byteToShortInBigEnd(dataNew);
                String str = "{\"data\":\"" + Arrays.toString(shortData) + "\"}";
                // 将数据发送到服务器的代码
                sendToServer(str);
                // 再次调度任务
                handler.postDelayed(this, 4000);
            }
        }
    };

    private void sendToServer(String str) {
        // 使用线程池执行网络请求
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // 1. 获取访问地址URL
                URL url = new URL("http://"+Sockets.ip+":"+Sockets.port+"/upload_json");
                System.out.println(url);
                // 2. 创建HttpURLConnection对象
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                /* 3. 设置请求参数等 */
                // 请求方式
                connection.setRequestMethod("POST");
                // 设置连接超时时间
                connection.setConnectTimeout(4000);// 设置是否向 HttpUrlConnection 输出，对于post请求，参数要放在 http 正文内，因此需要设为true，默认为false。
                connection.setDoOutput(true);
                // 设置是否从 HttpUrlConnection读入，默认为true
                connection.setDoInput(true);
                // 设置是否使用缓存
                connection.setUseCaches(false);
                // 设置此 HttpURLConnection 实例是否应该自动执行 HTTP 重定向
                connection.setInstanceFollowRedirects(true);
                // 设置使用标准编码格式编码参数的名-值对
                connection.setRequestProperty("Content-Type", "application/json");
                // JDK8中，HttpURLConnection默认开启Keep-Alive
                // 连接
                connection.connect();
                /* 4. 处理输入输出 */
                // 写入参数到请求中
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
                writer.write(str);
                writer.flush();
                writer.close();
                // 从连接中读取响应信息
                int code = 0;
                code = connection.getResponseCode();
                String msg="";
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        msg += line + "\n";
                    }
                    reader.close();
                }
                // 5. 断开连接
                connection.disconnect();
                // 处理结果
                System.out.println("响应码："+code);
                System.out.println("响应内容：");
                System.out.println(msg);
                // 返回内容作为全局变量
                rec_content = msg;
                // 解析响应内容
                JSONObject object = new JSONObject(msg);


                if (playAudio1 || playAudio2) {
                    // 发送长码1
                    if (playAudio1 && recordingIsReadyClose) {
                        final String sendContent = morseStr1;
                        // 通过主线程的Handler来播放音频和停止录音
                        // 延迟1秒后发送
                        receiveHandler.post(() -> {
                            refreshLogView("[6]开始发送连接报文应答...");
                            refreshLogView("\n");

                            System.out.println("开始发送音频，音频内容：" + sendContent);
                            // 关闭录音，等1秒
                            stopRecording();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            // 播放音频
                            recLongCodeWpm = Integer.parseInt((longCodeSpeedInput.getText().toString() == null) || (longCodeSpeedInput.getText().toString().equals("")) ? "20" : longCodeSpeedInput.getText().toString());
                            playLongAudio1(sendContent, recLongCodeWpm);
                            recordingIsReadyClose = false;
                            playAudio1 = false;
                        });
                    }
                    // 发送短码报文响应
                    if (playAudio2 && recordingIsReadyClose) {
                        final String sendContent = morseStr2;
                        // 通过主线程的Handler来播放音频和停止录音
                        receiveHandler.post(() -> {
                            refreshLogView("[8]开始发送短码报文应答...");
                            refreshLogView("\n");

                            System.out.println("开始发送音频，音频内容：" + sendContent);
                            // 关闭录音，等1秒
                            stopRecording();
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            // 播放音频
                            recLongCodeWpm = Integer.parseInt((longCodeSpeedInput.getText().toString() == null) || (longCodeSpeedInput.getText().toString().equals("")) ? "20" : longCodeSpeedInput.getText().toString());
                            playLongAudio2(sendContent, recLongCodeWpm);
                            recordingIsReadyClose = false;
                            playAudio2 = false;
                        });
                    }
                    // 录音关闭
                    if ((playAudio1 || playAudio2) && !recordingIsReadyClose) {
                        System.out.println("发送应答就绪！");
                        recordingIsReadyClose = true;
                    }
                }
                else {
                    // 匹配到短码，准备发送内容
                    if (object.has("content") && object.getJSONArray("content").length() != 0) {
                        content = object.getJSONArray("content").toString();
                        if (content.contains("+++")) {
                            JSONArray contentArr;
                            String crcContent = "";
                            String checkedCrcCode;
                            try {
                                mode = HalfDuplexMode.RECEIVE_SHORT_CODE;
                                emptyShortCodeTime = 0;
                                contentArr = object.getJSONArray("content");
                                crcContent = "";
                                for (int i = 0; i < contentArr.length(); i++) {
                                    if (contentArr.get(i).toString().contains("+") || contentArr.get(i).toString().contains("=")) {
                                        continue;
                                    } else {
                                        crcContent = crcContent + contentArr.get(i).toString() + " ";
                                    }
                                }
                                crcContent = crcContent.trim();

                                // 拿到一包报文
                                List<String> shortCodes = MessageUtils.getNoTrimCode(crcContent);
                                String tempShortText = "";
                                String id = shortCodes.get(0).trim();
                                for (int i = 0; i < shortCodes.size(); i++){
                                    tempShortText = tempShortText + shortCodes.get(i) + " ";
                                }
                                tempShortText = tempShortText.trim();
                                crcContent = tempShortText;
                                checkedCrcCode = MessageUtils.getCRC16(tempShortText);
                                // 该包报文crc和长码中相的的crc一致
                                if (checkedCrcCode.equals(shortCrcListForRec.get(Integer.parseInt(id)))){
                                    shortCodeTextMap.put(id, tempShortText);
                                    shortCodeContentForDisplay.append(content).append("\n");
                                    System.out.println("报文" + id + "：校验成功！");
                                } else {
                                    System.out.println("报文" + id + "：校验失败！");
                                }

                                System.out.println("shortCrcListForRec: " + shortCrcListForRec);
                                System.out.println("crcContent:" + crcContent);
                                System.out.println("checkedCrcCode: " + checkedCrcCode);
                            } catch (Exception e) {
                                return ;
                            }
                        }
                    }
                    // 匹配到长码，准备发送内容
                    else if (object.has("talkContent") && object.getString("talkContent").length() != 0) {
                        content = object.getString("talkContent");
                        if (content.contains("K")) {
                            // 解析设备号
                            String src_deviceId = StringUtils.getId1FromLongCode(content).trim();
                            String dest_deviceId = StringUtils.getId2FromLongCode(content).trim();
                            System.out.println("接收到长码，src_deviceId: " + src_deviceId);
                            System.out.println("接收到长码，dest_deviceId: " + dest_deviceId);
                            System.out.println("myDeviceId: " + myDeviceId);

                            // 目标设备匹配到本机
                            if (dest_deviceId.equals(myDeviceId)) {
                                System.out.println("设备号匹配");
                                // 新连接产生后，旧信息直接丢弃
                                shortCodeTextMap.clear();

                                String validContent;
                                String containsShortCodeCrc;
                                String cmpLongCrc;

                                try {
                                    validContent = StringUtils.getContentFromLongCode(content);
                                    containsShortCodeCrc = validContent.substring(0, validContent.length() - 4).trim();
                                    cmpLongCrc = MessageUtils.getCRC16(containsShortCodeCrc);
                                    lastSrcDeviceId = src_deviceId;

                                    // 从解码内容中获取两个CRC
                                    List<String> noTrimCode = MessageUtils.getNoTrimCode(validContent);
                                    shortCrcListForRec = new ArrayList<>();
                                    for (int i = 7; i < noTrimCode.size() - 1; i++){
                                        shortCrcListForRec.add(noTrimCode.get(i));
                                    }
                                    longCrcCode = StringUtils.getLongCrc(content);
                                } catch (Exception e) {
                                    return;
                                }
                                System.out.println("接收到长码，vaildContent: " + validContent);
                                System.out.println("接收到长码，shortCrcCode: " + shortCrcListForRec);
                                System.out.println("接收到长码，longCrcCode: " + longCrcCode);
                                System.out.println("接收到长码，cmpLongCrc: " + cmpLongCrc);
                                // 长码CRC校验
                                if (cmpLongCrc.equals(longCrcCode)) {
                                    // 播放音频
                                    playAudio1 = true;
                                    // 构造返回内容
                                    recv_content1 = "R R R " + lastSrcDeviceId + " "
                                            + "DE " + myDeviceId + " "
                                            + "Ready K K K";
                                    recv_content1 = addPreamble(recv_content1, preambleNum);
                                    System.out.println("长码响应报文:" + recv_content1);

                                    // 长码译码
                                    morseStr1 = morseLongCoder.encode(recv_content1);
                                    // 接收到长码
                                    recIsLongCode = true;
                                    refreshLogView("[5]接收到连接报文，内容:" + content);
                                    refreshLogView("\n");
                                    refreshLogView("[5]连接报文CRC校验成功！");
                                    refreshLogView("\n");

                                    System.out.println("准备发送连接应答！");
                                } else {
                                    refreshLogView("[5]接收到连接报文，内容:" + content);
                                    refreshLogView("\n");
                                    refreshLogView("[5]连接报文CRC校验失败！");
                                    refreshLogView("\n");
                                    return ;
                                }
                            }
                        }
                    }
                    else if (!Objects.isNull(mode) && mode.equals(HalfDuplexMode.RECEIVE_SHORT_CODE)){
                        if (emptyShortCodeTime <= maxEmptyShortCodeTime) {
                            emptyShortCodeTime++;
                        } else {
                            mode = HalfDuplexMode.SEND_SHORT_CODE_RESPONSE;
                            emptyShortCodeTime = 0;
                            // 播放音频
                            playAudio2 = true;

                            // 统计未校验的报文
                            List<String> uncheckedPkgIds = new ArrayList<>();
                            for (int i = 0; i < shortCrcListForRec.size(); i++){
                                if (!shortCodeTextMap.containsKey(String.valueOf(i))){
                                    uncheckedPkgIds.add(String.valueOf(i));
                                }
                            }

                            System.out.println("uncheckedPkgIds:" + uncheckedPkgIds);

                            if (uncheckedPkgIds.isEmpty()){
                                // 构造返回内容
                                recv_content2 = "R R R " + lastSrcDeviceId + " "
                                        + "DE " + myDeviceId + " "
                                        + "OK "
                                        + "9801 "
                                        + "Ready "
                                        + "K K K";
                            } else {
                                StringBuilder idBuilder = new StringBuilder();
                                for (String id : uncheckedPkgIds){
                                    idBuilder.append(id).append(" ");
                                }
                                // 构造返回内容
                                recv_content2 = "R R R " + lastSrcDeviceId + " "
                                        + "DE " + myDeviceId + " "
                                        + idBuilder.toString()
                                        + "9801 "
                                        + "Ready "
                                        + "K K K";
                            }
                            recv_content2 = addPreamble(recv_content2, preambleNum);
                            System.out.println("短码响应报文:" + recv_content2);
                            // 短码译码
                            morseStr2 = morseLongCoder.encode(recv_content2);
                            // 接收状态重置
                            recIsLongCode = false;


                            refreshLogView("[7]接收到短码报文，内容:" + shortCodeContentForDisplay.delete(shortCodeContentForDisplay.length() -1, shortCodeContentForDisplay.length()));
                            refreshLogView("\n");
                            refreshLogView("[7]短码报文CRC校验成功！");
                            refreshLogView("\n");
                            System.out.println("准备发送响应应答！");
                        }
                    }
                }



            } catch (Exception e) {
                // 网络请求异常处理
                e.printStackTrace();
            }
        });
    }

    private void playLongAudio1(String string, int wpm) {
        recSendLongMorseCode1(string, wpm);
    }

    private void playLongAudio2(String string, int wpm) {
        recSendLongMorseCode2(string, wpm);
    }

    private void stopRecording() {
        System.out.println("关闭录音！");
        isRecording = false;
        audioRecord.stop(); // 停止录音
        audioRecord.release(); // 释放AudioRecord资源
        // 取消定时任务
        handler.removeCallbacks(readBufferTask);

        boolean isSaveWav = false;
        try {
            isSaveWav = FileUtils.readTxt(context.getExternalFilesDir("").getAbsolutePath()+"/isSaveWav.txt").equals("1");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("isSaveWav:" + isSaveWav);

        if (isSaveWav){
            // 将录音数据写入到文件中
            long currentTimeMillis = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
            Date date = new Date(currentTimeMillis);
            String formattedDate = sdf.format(date);

            String pcmFileName = context.getExternalFilesDir("").getAbsolutePath()+"/comm_" + formattedDate + ".pcm";
            String wavFileName = context.getExternalFilesDir("").getAbsolutePath()+"/comm_" + formattedDate + ".wav";

            System.out.println("保存录音文件");
            System.out.println("allRecordArrays.size()= " + allRecordArrays.size());

            combineAudioDataAndWrite(allRecordArrays, pcmFileName);
            //利用自定义工具类将pcm格式的文件转换为wav格式文件才能进行播放
            tool.pcmToWav(pcmFileName,wavFileName);
        }
    }

    private void onAudioPlaybackComplete() {
        // 重新开启录音
        startRecording();
    }

    private void startRecording() {
        System.out.println("重新开启录音！");
        // 重新初始化AudioRecord
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, recordBufSize * 2);
        }
        audioRecord.startRecording();
        // 重新启动定时读取缓冲区的任务
        isRecording = true;

        // 录音数据清空
        System.out.println("清空录音数据");
        allRecordArrays.clear();
        Arrays.fill(data, (byte) 0);
        System.out.println("allRecordArrays.size()= " + allRecordArrays.size());

        // 开启录音线程
        handler.post(readBufferTask);
    }

    /**
     * 将录音数据写入到文件中
     * @param dataArrays
     * @return
     */
    private void combineAudioDataAndWrite(List<byte[]> dataArrays, String path) {
        FileOutputStream os = null;
        File file=new File(path);
        try {
            //如果文件不存在，就创建文件
            if(file.exists()){
                file.delete();
            }
            //创建MorseCode.wav文件
            file.createNewFile();
            os = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        for (byte[] data : dataArrays) {
            try {
                os.write(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            //关闭文件
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //调整pcm音量
    public int amplifyPCMData(byte[] pData, int nLen, byte[] data2, int nBitsPerSample, float multiple)
    {
        int nCur = 0;
        if (16 == nBitsPerSample)
        {
            while (nCur < nLen)
            {
                short volum = getShort(pData, nCur);
                volum = (short)(volum * multiple);
                data2[nCur]   = (byte)( volum       & 0xFF);
                data2[nCur+1] = (byte)((volum >> 8) & 0xFF);
                nCur += 2;
            }
        }
        return 0;
    }
    //short转data
    private short getShort(byte[] data, int start)
    {
        return (short)((data[start] & 0xFF) | (data[start+1] << 8));
    }

    /**
     * 发送莫尔斯长码
     * @param content
     * @param wpm
     */
    public void recSendLongMorseCode1(String content, int wpm) {
        try {
            MorseAudio morseAudio = new MorseAudio();
            morseAudio.setChangeSnr(true);

            short[] shorts = morseAudio.codeConvert2Sound(content, wpm);
            byte[] bytes=new byte[shorts.length*2];
            //大端序转小端序
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
            byte[] header = morseAudio.writeWavFileHeader(shorts.length*2, 8000, 1, 16);

            ByteArrayOutputStream byteArrayOutputStream_WAV = new ByteArrayOutputStream();
            byteArrayOutputStream_WAV.write(header);
            // 如果不播放音频，则将音频信息全部置0
            byte[] bytesForNotPlayAudio=new byte[shorts.length*2];
            if (!isPlayAudio) {
                byteArrayOutputStream_WAV.write(bytesForNotPlayAudio);
            } else {
                byteArrayOutputStream_WAV.write(bytes);
            }
            byte[] byteArray = byteArrayOutputStream_WAV.toByteArray();
            //创建文件
            String Path_WAV = context.getExternalFilesDir("").getAbsolutePath()+"/morse.wav";
            System.out.println(Path_WAV);
            File file_WAV = new File(Path_WAV);
            if(file_WAV.exists()){
                file_WAV.delete();
            }
            file_WAV.createNewFile();//创建MorseCode.wav文件
            //覆盖写入
            OutputStream os_WAV = new FileOutputStream(file_WAV);
            os_WAV.write(byteArray);
            os_WAV.close();

            //生成pcm文件
            ByteArrayOutputStream byteArrayOutputStream_PCM = new ByteArrayOutputStream();
            byteArrayOutputStream_PCM.write(bytes);
            byteArray = byteArrayOutputStream_PCM.toByteArray();
            //创建文件
            String Path_PCM = context.getExternalFilesDir("").getAbsolutePath()+"/morse.pcm";
            System.out.println(Path_PCM);
            File file_PCM = new File(Path_PCM);
            if(file_PCM.exists()){
                file_PCM.delete();
            }
            file_PCM.createNewFile();//创建MorseCode.wav文件
            //覆盖写入
            OutputStream os_PCM = new FileOutputStream(file_PCM);
            os_PCM.write(byteArray);
            os_PCM.close();
            //开始播放
            try {
                mediaPlayer.setDataSource(Path_WAV);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(false);  // 设置非循环播放
                mediaPlayer.start(); // 开始播放

                System.out.println("开始播放");
                // 设置播放完成后的监听器
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // 音乐播放完成，进行轮询阻塞
                        mediaPlayer.reset();
                        // 延迟1秒后发送
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        onAudioPlaybackComplete();
                        refreshLogView("[6]连接响应报文发送完成!");
                        refreshLogView("\n");

                        System.out.println("播放完成！");
                    }
                });

                //异步发送 ptt脉冲输出
                System.out.println("content:" + content);
                System.out.println("longWpm:" + wpm);
                new Thread(){
                    @Override
                    public void run() {
                        MyAudio.getInstance().playMorse(content, wpm);
                    }
                }.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送莫尔斯短码
     * @param content
     * @param wpm
     */
    public void recSendLongMorseCode2(String content, int wpm) {
        try {
            MorseAudio morseAudio = new MorseAudio();
            morseAudio.setChangeSnr(true);

            short[] shorts = morseAudio.codeConvert2Sound(content, wpm);
            byte[] bytes=new byte[shorts.length*2];
            //大端序转小端序
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
            byte[] header = morseAudio.writeWavFileHeader(shorts.length*2, 8000, 1, 16);

            ByteArrayOutputStream byteArrayOutputStream_WAV = new ByteArrayOutputStream();
            byteArrayOutputStream_WAV.write(header);
            // 如果不播放音频，则将音频信息全部置0
            byte[] bytesForNotPlayAudio=new byte[shorts.length*2];
            if (!isPlayAudio) {
                byteArrayOutputStream_WAV.write(bytesForNotPlayAudio);
            } else {
                byteArrayOutputStream_WAV.write(bytes);
            }
            byte[] byteArray = byteArrayOutputStream_WAV.toByteArray();
            //创建文件
            String Path_WAV = context.getExternalFilesDir("").getAbsolutePath()+"/morse.wav";
            System.out.println(Path_WAV);
            File file_WAV = new File(Path_WAV);
            if(file_WAV.exists()){
                file_WAV.delete();
            }
            file_WAV.createNewFile();//创建MorseCode.wav文件
            //覆盖写入
            OutputStream os_WAV = new FileOutputStream(file_WAV);
            os_WAV.write(byteArray);
            os_WAV.close();

            //生成pcm文件
            ByteArrayOutputStream byteArrayOutputStream_PCM = new ByteArrayOutputStream();
            byteArrayOutputStream_PCM.write(bytes);
            byteArray = byteArrayOutputStream_PCM.toByteArray();
            //创建文件
            String Path_PCM = context.getExternalFilesDir("").getAbsolutePath()+"/morse.pcm";
            System.out.println(Path_PCM);
            File file_PCM = new File(Path_PCM);
            if(file_PCM.exists()){
                file_PCM.delete();
            }
            file_PCM.createNewFile();//创建MorseCode.wav文件
            //覆盖写入
            OutputStream os_PCM = new FileOutputStream(file_PCM);
            os_PCM.write(byteArray);
            os_PCM.close();
            //开始播放
            try {
                mediaPlayer.setDataSource(Path_WAV);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(false);  // 设置非循环播放
                mediaPlayer.start(); // 开始播放

                System.out.println("开始播放");
                // 设置播放完成后的监听器
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // 音乐播放完成，进行轮询阻塞
                        mediaPlayer.reset();
                        // 延迟1秒后发送
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        onAudioPlaybackComplete();

                        refreshLogView("[8]短码响应报文发送完成!");
                        refreshLogView("\n");

                        System.out.println("播放完成！");
                    }
                });

                //异步发送 ptt脉冲输出
                System.out.println("content:" + content);
                System.out.println("longWpm:" + wpm);
                new Thread(){
                    @Override
                    public void run() {
                        MyAudio.getInstance().playMorse(content, wpm);
                    }
                }.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 发送莫尔斯长码
     * @param content
     * @param wpm
     */
    public void sendLongMorseCode(String content, int wpm) {
        try {
            MorseAudio morseAudio = new MorseAudio();
            morseAudio.setChangeSnr(true);
            short[] shorts = morseAudio.codeConvert2Sound(content, wpm);
            byte[] bytes=new byte[shorts.length*2];
            //大端序转小端序
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
            byte[] header = morseAudio.writeWavFileHeader(shorts.length*2, 8000, 1, 16);

            ByteArrayOutputStream byteArrayOutputStream_WAV = new ByteArrayOutputStream();
            byteArrayOutputStream_WAV.write(header);
            // 如果不播放音频，则将音频信息全部置0
            byte[] bytesForNotPlayAudio=new byte[shorts.length*2];
            if (!isPlayAudio) {
                byteArrayOutputStream_WAV.write(bytesForNotPlayAudio);
            } else {
                byteArrayOutputStream_WAV.write(bytes);
            }
            byte[] byteArray = byteArrayOutputStream_WAV.toByteArray();
            //创建文件
            String Path_WAV = context.getExternalFilesDir("").getAbsolutePath()+"/morse.wav";
            System.out.println(Path_WAV);
            File file_WAV = new File(Path_WAV);
            if(file_WAV.exists()){
                file_WAV.delete();
            }
            file_WAV.createNewFile();//创建MorseCode.wav文件
            //覆盖写入
            OutputStream os_WAV = new FileOutputStream(file_WAV);
            os_WAV.write(byteArray);
            os_WAV.close();

            //生成pcm文件
            ByteArrayOutputStream byteArrayOutputStream_PCM = new ByteArrayOutputStream();
            byteArrayOutputStream_PCM.write(bytes);
            byteArray = byteArrayOutputStream_PCM.toByteArray();
            //创建文件
            String Path_PCM = context.getExternalFilesDir("").getAbsolutePath()+"/morse.pcm";
            System.out.println(Path_PCM);
            File file_PCM = new File(Path_PCM);
            if(file_PCM.exists()){
                file_PCM.delete();
            }
            file_PCM.createNewFile();//创建MorseCode.wav文件
            //覆盖写入
            OutputStream os_PCM = new FileOutputStream(file_PCM);
            os_PCM.write(byteArray);
            os_PCM.close();
            //开始播放
            try {
                mediaPlayer.setDataSource(Path_WAV);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(false);  // 设置非循环播放
                mediaPlayer.start(); // 开始播放
                System.out.println("开始播放");
                // 设置播放完成后的监听器
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // 音乐播放完成，进行轮询阻塞
                        mediaPlayer.reset();
                        // 音乐播放完成，进行轮询阻塞
                        System.out.println("播放完成！");
                        // 恢复录音
                        onAudioPlaybackComplete();
                        HalfDuplex.sendLongCodeMessage1 = true;
                    }
                });

                //异步发送 ptt脉冲输出
                System.out.println("content:" + content);
                System.out.println("longWpm:" + wpm);
                new Thread(){
                    @Override
                    public void run() {
                        MyAudio.getInstance().playMorse(content, wpm);
                    }
                }.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 发送莫尔斯短码
     * @param content
     * @param wpm
     */
    public void sendShortMorseCode(String content, int wpm) {
        try {
            MorseAudio morseAudio = new MorseAudio();
            morseAudio.setChangeSnr(true);
            short[] shorts = morseAudio.codeConvert2Sound(content, wpm);
            byte[] bytes=new byte[shorts.length*2];
            //大端序转小端序
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
            byte[] header = morseAudio.writeWavFileHeader(shorts.length*2, 8000, 1, 16);

            ByteArrayOutputStream byteArrayOutputStream_WAV = new ByteArrayOutputStream();
            byteArrayOutputStream_WAV.write(header);
            // 如果不播放音频，则将音频信息全部置0
            byte[] bytesForNotPlayAudio=new byte[shorts.length*2];
            if (!isPlayAudio) {
                byteArrayOutputStream_WAV.write(bytesForNotPlayAudio);
            } else {
                byteArrayOutputStream_WAV.write(bytes);
            }
            byte[] byteArray = byteArrayOutputStream_WAV.toByteArray();
            //创建文件
            String Path_WAV = context.getExternalFilesDir("").getAbsolutePath()+"/morse.wav";
            System.out.println(Path_WAV);
            File file_WAV = new File(Path_WAV);
            if(file_WAV.exists()){
                file_WAV.delete();
            }
            file_WAV.createNewFile();//创建MorseCode.wav文件
            //覆盖写入
            OutputStream os_WAV = new FileOutputStream(file_WAV);
            os_WAV.write(byteArray);
            os_WAV.close();

            //生成pcm文件
            ByteArrayOutputStream byteArrayOutputStream_PCM = new ByteArrayOutputStream();
            byteArrayOutputStream_PCM.write(bytes);
            byteArray = byteArrayOutputStream_PCM.toByteArray();
            //创建文件
            String Path_PCM = context.getExternalFilesDir("").getAbsolutePath()+"/morse.pcm";
            System.out.println(Path_PCM);
            File file_PCM = new File(Path_PCM);
            if(file_PCM.exists()){
                file_PCM.delete();
            }
            file_PCM.createNewFile();//创建MorseCode.wav文件
            //覆盖写入
            OutputStream os_PCM = new FileOutputStream(file_PCM);
            os_PCM.write(byteArray);
            os_PCM.close();
            //开始播放
            try {
                // 创建CountDownLatch，计数为1
                CountDownLatch latch = new CountDownLatch(1);
                // 设置播放完成后的监听器
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // 音乐播放完成，进行轮询阻塞
                        mediaPlayer.reset();
                        // 音乐播放完成，进行轮询阻塞
                        System.out.println("播放完成！");
                        latch.countDown();
                    }
                });
                mediaPlayer.setDataSource(Path_WAV);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(false);  // 设置非循环播放
                mediaPlayer.start(); // 开始播放

                System.out.println("开始播放");

                // 发送ptt
                new Thread(){
                    @Override
                    public void run() {
                        sendPtt(content, wpm);
                    }
                }.start();

                // 阻塞主线程，直到CountDownLatch计数为0
                latch.await();



            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void sendPtt(String content, int wpm) {
        System.out.println("content:" + content);
        System.out.println("shortWpm:" + wpm);
        MyAudio.getInstance().playMorse(content, wpm);
    }

    private void refreshIsPlayAudio() throws IOException {
        isPlayAudio = FileUtils.readTxt(context.getExternalFilesDir("").getAbsolutePath()+"/isPlayAudio.txt").equals("1");
    }

    /**
     * 增加前导码
     * @param str
     * @return
     */
    private String addPreamble(String str, int n){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < n; i++){
            sb.append("7777 ");
        }
        sb.append(str);
        return sb.toString();
    }
}
