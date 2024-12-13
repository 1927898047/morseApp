package com.zyj.morseapp.pages;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zyj.morseapp.R;
import com.zyj.morseapp.audio.MorseAudio;
import com.zyj.morseapp.morsecoder.MorseShortCoder;
import com.zyj.morseapp.utils.ArraysUtils;
import com.zyj.morseapp.utils.FileUtils;
import com.zyj.morseapp.utils.socket.PostUtils;
import com.zyj.morseapp.application.MyApplication;
import com.zyj.morseapp.utils.ptt.MyAudio;
import com.zyj.morseapp.utils.socket.ResetUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sockets extends AppCompatActivity {
    private static TextView output_text =null;
    private static Button send_shortWave=null;
    private static Button send_longWave=null;
    private static Button connectionTest = null;
    private static Button connectionReset = null;

    private Context context=null;
    public static String port = "5000";
    public static String ip = "127.0.0.1";
    public static String deviceId="1";
    public static Integer expiredTime = 40;
    public static boolean isPlayAudio = true;
    public static boolean isSelfAdaptionWpm = false;
    public static Integer preambleNum = 1;


    private Button bt_settings;
    private EditText et_port;
    private EditText et_ip;
    private EditText et_deviceId;
    private EditText et_expiredTime;
    private CheckBox isPlayAudioButton;
    private CheckBox isSelfAdaptionWpmButton;
    private EditText et_preamble_num;

    private static String ipTxt;
    private static String portTxt;
    private static String deviceIdTxt;
    private static String expiredTimeTxt;
    private static String isPlayAudioTxt;
    private static String isSelfAdaptionWpmTxt;
    private static String preambleNumTxt;


    public static int longWpm = 25;
    public static int shortWpm = 40;
    private static MorseShortCoder morseShortCoder;
    private static String shortMorseWavForTestPath;
    private static String shortMorsePcmForTestPath;

    private MorseAudio morseAudio;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //控件初始化
        setContentView(R.layout.activity_socket);
        send_shortWave = findViewById(R.id.send_shortWave);
        send_longWave = findViewById(R.id.send_longWave);
        connectionTest = findViewById(R.id.serverTest);
        connectionReset = findViewById(R.id.serverRestart);

        bt_settings=findViewById(R.id.bt_settings);
        et_port=findViewById(R.id.et_port);
        et_ip=findViewById(R.id.et_ip);
        et_deviceId = findViewById(R.id.et_deviceId);
        et_expiredTime = findViewById(R.id.et_expiredTime);
        output_text=findViewById(R.id.serverReturn);
        isPlayAudioButton = findViewById(R.id.PlayAudioButton);
        isSelfAdaptionWpmButton = findViewById(R.id.SelfAdaptionButton);
        et_preamble_num = findViewById(R.id.et_preamble_num);

        send_shortWave.setOnClickListener(new MyOnClick());
        send_longWave.setOnClickListener(new MyOnClick());
        connectionTest.setOnClickListener(new MyOnClick());
        connectionReset.setOnClickListener(new MyOnClick());

        et_port.setOnClickListener(new MyOnClick());
        et_ip.setOnClickListener(new MyOnClick());
        et_deviceId.setOnClickListener(new MyOnClick());
        et_expiredTime.setOnClickListener(new MyOnClick());
        et_preamble_num.setOnClickListener(new MyOnClick());

        bt_settings.setOnClickListener(new MyOnClick());
        context = MyApplication.getContext();

        ipTxt = context.getExternalFilesDir("").getAbsolutePath()+"/ip.txt";
        portTxt = context.getExternalFilesDir("").getAbsolutePath()+"/port.txt";
        deviceIdTxt = context.getExternalFilesDir("").getAbsolutePath()+"/deviceId.txt";
        expiredTimeTxt = context.getExternalFilesDir("").getAbsolutePath()+"/expiredTime.txt";
        isPlayAudioTxt = context.getExternalFilesDir("").getAbsolutePath()+"/isPlayAudio.txt";
        isSelfAdaptionWpmTxt = context.getExternalFilesDir("").getAbsolutePath()+"/isSelfAdaptionWpm.txt";
        preambleNumTxt = context.getExternalFilesDir("").getAbsolutePath()+"/preambleNum.txt";
        output_text.setMovementMethod(ScrollingMovementMethod.getInstance());
        //持久化ip设置
        try {
            if(!FileUtils.fileExist(ipTxt)){
                FileUtils.writeTxt(ipTxt, ip);
            }
            if(!FileUtils.fileExist(portTxt)){
                FileUtils.writeTxt(portTxt, port);
            }
            if(!FileUtils.fileExist(deviceIdTxt)){
                FileUtils.writeTxt(deviceIdTxt, deviceId);
            }
            if(!FileUtils.fileExist(expiredTimeTxt)){
                FileUtils.writeTxt(expiredTimeTxt, String.valueOf(expiredTime));
            }
            if(!FileUtils.fileExist(isSelfAdaptionWpmTxt)){
                FileUtils.writeTxt(isSelfAdaptionWpmTxt, isSelfAdaptionWpm ? "1" : "0");
            }
            if(!FileUtils.fileExist(isPlayAudioTxt)){
                FileUtils.writeTxt(isPlayAudioTxt, isPlayAudio ? "1" : "0");
            }
            if(!FileUtils.fileExist(preambleNumTxt)){
                FileUtils.writeTxt(preambleNumTxt, String.valueOf(preambleNum));
            }
            ip = FileUtils.readTxt(ipTxt);
            port = FileUtils.readTxt(portTxt);
            deviceId = FileUtils.readTxt(deviceIdTxt);
            expiredTime = Integer.valueOf(FileUtils.readTxt(expiredTimeTxt));
            isPlayAudio = FileUtils.readTxt(isPlayAudioTxt).equals("1");
            isSelfAdaptionWpm = FileUtils.readTxt(isSelfAdaptionWpmTxt).equals("1");
            preambleNum = Integer.valueOf(FileUtils.readTxt(preambleNumTxt));
        } catch (IOException e) {
            e.printStackTrace();
        }

        et_ip.setText(ip);
        et_port.setText(port);
        et_deviceId.setText(deviceId);
        et_expiredTime.setText(expiredTime.toString());
        isPlayAudioButton.setChecked(isPlayAudio);
        isSelfAdaptionWpmButton.setChecked(isSelfAdaptionWpm);
        et_preamble_num.setText(preambleNum.toString());

        output_text.setMovementMethod(ScrollingMovementMethod.getInstance());
        output_text.setMaxLines(Integer.MAX_VALUE); // 设置足够大的行数以容纳所有文本
        output_text.setScrollContainer(true);
        output_text.setFocusable(true);
        output_text.setSelected(true);

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



        // 初始化测试音频
        shortMorseWavForTestPath = context.getExternalFilesDir("").getAbsolutePath()+"/shortMorseWavForTest.wav";
        shortMorsePcmForTestPath = context.getExternalFilesDir("").getAbsolutePath()+"/shortMorsePcmForTest.pcm";
        morseShortCoder = new MorseShortCoder();
        morseAudio = new MorseAudio();
        String shortCodeForTest = "=== 0000 1111 2222 3333 4444 +++ 3333 4444";
        // 字符转摩尔斯码
        String shortMorseForTest= morseShortCoder.encode(shortCodeForTest);
        // 摩尔斯码转音频
        short[] shorts = morseAudio.codeConvert2Sound(shortMorseForTest, 35);
        byte[] bytes=new byte[shorts.length*2];
        //大端序转小端序
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
        byte[] header = new byte[0];
        try {
            header = morseAudio.writeWavFileHeader(shorts.length*2, 8000, 1, 16);
            //生成wav文件
            ByteArrayOutputStream byteArrayOutputStream_WAV = new ByteArrayOutputStream();
            byteArrayOutputStream_WAV.write(header);
            byteArrayOutputStream_WAV.write(bytes);
            byte[] byteArray = byteArrayOutputStream_WAV.toByteArray();
            //创建文件
            String Path_WAV = shortMorseWavForTestPath;
            System.out.println(Path_WAV);
            File file_WAV = new File(Path_WAV);
            if(!file_WAV.exists()){
                file_WAV.createNewFile();//创建MorseCode.wav文件
                //覆盖写入
                OutputStream os_WAV = new FileOutputStream(file_WAV);
                os_WAV.write(byteArray);
                os_WAV.close();
            }


            //生成pcm文件
            ByteArrayOutputStream byteArrayOutputStream_PCM = new ByteArrayOutputStream();
            byteArrayOutputStream_PCM.write(bytes);
            byteArray = byteArrayOutputStream_PCM.toByteArray();
            //创建文件
            String Path_PCM = shortMorsePcmForTestPath;
            System.out.println(Path_PCM);
            File file_PCM = new File(Path_PCM);
            if(!file_PCM.exists()){
                file_PCM.createNewFile();//创建MorseCode.wav文件
                //覆盖写入
                OutputStream os_PCM = new FileOutputStream(file_PCM);
                os_PCM.write(byteArray);
                os_PCM.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void show(String str) {
        output_text.post(new Runnable() {
            @Override
            public void run() {
                output_text.setText(str);
            }
        });
    }


    class MyOnClick implements View.OnClickListener{
        @Override
        public void onClick(View view) {

            String shortWavePath= context.getExternalFilesDir("").getAbsolutePath()+"/morse_shortCode.pcm";
            String longWavePath= context.getExternalFilesDir("").getAbsolutePath()+"/morse_longCode.pcm";
            ExecutorService exec = Executors.newSingleThreadScheduledExecutor();
            switch (view.getId()){
                //网络通信：发送PCM文件，显示返回内容
                case R.id.send_shortWave:
                    show("");
                    if(!new File(shortWavePath).exists()){
                        Toast.makeText(Sockets.this,"短码音频文件不存在，发送失败！",Toast.LENGTH_LONG).show();
                        break;
                    }

                    Toast.makeText(Sockets.this,"正在发送文件......",Toast.LENGTH_LONG).show();
                    byte[] bytes1 = FileUtils.fileToByteArr(shortWavePath);
                    short[] shorts1 = ArraysUtils.byteToShortInBigEnd(bytes1);
                    String str1="";
                    int i1=0;
                    while(i1*32000<shorts1.length) {
                        if((i1+1)*32000>shorts1.length){
                            short[] end = Arrays.copyOfRange(shorts1, i1 * 32000, shorts1.length);
                            str1 = "{\"data\":\"" + Arrays.toString(end) + "\"}";
                        }
                        else{
                            short[] shorts2 = Arrays.copyOfRange(shorts1, i1 * 32000, (i1 + 1) * 32000);
                            str1 = "{\"data\":\"" + Arrays.toString(shorts2) + "\"}";
                        }
                        exec.submit(new PostUtils(str1));
                        exec.submit(new Thread(){
                            @Override
                            public void run() {
                                if(PostUtils.code!=200) {
                                    show("后台通信失败！");
                                    System.out.println("后台通信失败");
                                }
                                else if(PostUtils.code==200){
                                    JSONArray content;
                                    try {
                                        JSONObject object=new JSONObject(PostUtils.msg);
                                        content=object.getJSONArray("content");
                                        if(content.length()!=0){
                                            show(PostUtils.msg);
                                        }
                                        else if(content.length()==0 && output_text.getText().toString().equals("")){
                                            show("收到空报文");
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                PostUtils.code=0;
                            }
                        });
                        i1++;
                    }
                    exec.submit(new Thread(){
                        @Override
                        public void run() {
                            Looper.prepare();
                            Toast.makeText(Sockets.this,"发送结束！",Toast.LENGTH_LONG).show();
                            Looper.loop();
                        }
                    });
                    exec.shutdown();

//                    // ptt脉冲输出
//                    System.out.println("ShortCoder.shortMorseContentForPtt:" + ShortCoder.shortMorseContentForPtt);
//                    System.out.println("shortWpm:" + shortWpm);
//                    try {
//                        String shortMorseStr = ShortCoder.shortMorseContentForPtt;
//                        MorseAudio morseAudioObj1 = new MorseAudio();
//                        short[] shortMorseStrArr = morseAudioObj1.codeConvert2Sound(shortMorseStr, shortWpm);
//                        MyAudio.getInstance().playMorse(shortMorseStr, shortWpm);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }


                    break;
                case R.id.send_longWave:
                    output_text.setText("");
                    if(!new File(longWavePath).exists()){
                        Toast.makeText(Sockets.this,"长码音频文件不存在，发送失败！",Toast.LENGTH_LONG).show();
                        break;
                    }
                    Toast.makeText(Sockets.this,"正在发送文件......",Toast.LENGTH_LONG).show();
                    byte[] bytes2 = FileUtils.fileToByteArr(longWavePath);
                    short[] shorts3 = ArraysUtils.byteToShortInBigEnd(bytes2);
                    String str2="";
                    int i2=0;
                    while(i2*32000<shorts3.length) {
                        if((i2+1)*32000>shorts3.length){
                            short[] end = Arrays.copyOfRange(shorts3, i2 * 32000, shorts3.length);
                            str2 = "{\"data\":\"" + Arrays.toString(end) + "\"}";
                            System.out.println(str2);
//                            System.out.println(end.length);
                        }
                        else{
                            short[] shorts4 = Arrays.copyOfRange(shorts3, i2 * 32000, (i2 + 1) * 32000);
                            str2 = "{\"data\":\"" + Arrays.toString(shorts4) + "\"}";
                        }
                        exec.submit(new PostUtils(str2));
                        exec.submit(new Thread(){
                            @Override
                            public void run() {
                                if(PostUtils.code!=200) {
                                    show("后台通信失败！");
                                }
                                else if(PostUtils.code==200){
                                    String talkContent;
                                    try {
                                        JSONObject object=new JSONObject(PostUtils.msg);
                                        talkContent=object.getString("talkContent");
                                        if(!talkContent.equals("")){
                                            show(PostUtils.msg);
                                        }
                                        else if(talkContent.equals("") && (output_text.getText().toString().equals(""))){
                                            show("收到空报文");
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                PostUtils.code=0;
                            }
                        });
                        i2++;
                    }
                    exec.submit(new Thread(){
                        @Override
                        public void run() {
                            Looper.prepare();
                            Toast.makeText(Sockets.this,"发送结束！",Toast.LENGTH_LONG).show();
                            Looper.loop();
                        }
                    });
                    exec.shutdown();

//                    // ptt脉冲输出
//                    System.out.println("LongCoder.longMorseContentForPtt:" + LongCoder.longMorseContentForPtt);
//                    System.out.println("longWpm:" + longWpm);
//                    try {
//                        String longMorseStr = LongCoder.longMorseContentForPtt;
//                        MorseAudio morseAudioObj2 = new MorseAudio();
//                        short[] longMorseStrArr = morseAudioObj2.codeConvert2Sound(longMorseStr, longWpm);
//                        MyAudio.getInstance().playMorse(longMorseStr, longWpm);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }


                    break;
                case R.id.bt_settings:
                    ip = et_ip.getText().toString();
                    port = et_port.getText().toString();
                    deviceId = et_deviceId.getText().toString();
                    expiredTime = Integer.valueOf(et_expiredTime.getText().toString());
                    isPlayAudio = isPlayAudioButton.isChecked();
                    isSelfAdaptionWpm = isSelfAdaptionWpmButton.isChecked();
                    preambleNum = Integer.valueOf(et_preamble_num.getText().toString());

                    et_ip.setText(ip);
                    et_port.setText(port);
                    et_deviceId.setText(deviceId);
                    et_expiredTime.setText(expiredTime.toString());
                    et_preamble_num.setText(preambleNum.toString());
                    try {
                        FileUtils.writeTxt(ipTxt, ip);
                        FileUtils.writeTxt(portTxt, port);
                        FileUtils.writeTxt(deviceIdTxt, deviceId);
                        FileUtils.writeTxt(expiredTimeTxt, String.valueOf(expiredTime));
                        FileUtils.writeTxt(isSelfAdaptionWpmTxt, isSelfAdaptionWpm ? "1" : "0");
                        FileUtils.writeTxt(isPlayAudioTxt, isPlayAudio ? "1" : "0");
                        FileUtils.writeTxt(preambleNumTxt, String.valueOf(preambleNum));

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(Sockets.this,"IP设置已更改！",Toast.LENGTH_LONG).show();
                    break;
                // 后台测试
                case R.id.serverTest:
                    shortMorseWavForTestPath = context.getExternalFilesDir("").getAbsolutePath()+"/shortMorseWavForTest.wav";
                    shortMorsePcmForTestPath = context.getExternalFilesDir("").getAbsolutePath()+"/shortMorsePcmForTest.pcm";

                    show("");
                    if(!new File(shortMorsePcmForTestPath).exists()){
                        Toast.makeText(Sockets.this,"测试音频文件不存在，发送失败！",Toast.LENGTH_LONG).show();
                        break;
                    }

                    Toast.makeText(Sockets.this,"正在发送文件......",Toast.LENGTH_LONG).show();
                    byte[] bytes = FileUtils.fileToByteArr(shortMorsePcmForTestPath);
                    short[] shorts = ArraysUtils.byteToShortInBigEnd(bytes);
                    String str = "";
                    int i = 0;
                    while(i *32000 < shorts.length) {
                        if((i + 1) * 32000 > shorts.length){
                            short[] end = Arrays.copyOfRange(shorts, i * 32000, shorts.length);
                            str = "{\"data\":\"" + Arrays.toString(end) + "\"}";
                        }
                        else{
                            short[] shorts2 = Arrays.copyOfRange(shorts, i * 32000, (i + 1) * 32000);
                            str = "{\"data\":\"" + Arrays.toString(shorts2) + "\"}";
                        }
                        exec.submit(new PostUtils(str));
                        exec.submit(new Thread(){
                            @Override
                            public void run() {
                                if(PostUtils.code!=200) {
                                    show("后台通信失败！");
                                    System.out.println("后台测试通信失败！");
                                }
                                else if(PostUtils.code==200){
                                    JSONArray content;
                                    try {
                                        JSONObject object=new JSONObject(PostUtils.msg);
                                        content=object.getJSONArray("content");
                                        if(content.length()!=0){
                                            show(PostUtils.msg);
                                        }
                                        else if(content.length()==0 && output_text.getText().toString().equals("")){
                                            show("收到空报文");
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                PostUtils.code=0;
                            }
                        });
                        i++;
                    }
                    exec.submit(new Thread(){
                        @Override
                        public void run() {
                            Looper.prepare();
                            Toast.makeText(Sockets.this,"发送结束！",Toast.LENGTH_LONG).show();
                            Looper.loop();
                        }
                    });
                    exec.shutdown();

                    break;
                // 后台复位
                case R.id.serverRestart:
                    String resetCommand = "{\"command\":\"" + "reset" + "\"}";
                    Toast.makeText(Sockets.this,"正在复位后台",Toast.LENGTH_SHORT).show();

                    exec.submit(new ResetUtils(resetCommand));
                    exec.submit(new Thread(){
                        @Override
                        public void run() {
                            if(ResetUtils.code != 200) {
                                show("后台复位失败！");
                                System.out.println("后台测试通信失败！");
                            }
                            else if(ResetUtils.code == 200){
                                show(ResetUtils.msg);
                            }
                            ResetUtils.code=0;
                        }
                    });
                    exec.shutdown();

                    break;
                default:
                    break;
            }

        }
    }


}
