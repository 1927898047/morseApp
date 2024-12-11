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
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zyj.morseapp.R;
import com.zyj.morseapp.audio.MorseAudio;
import com.zyj.morseapp.utils.ArraysUtils;
import com.zyj.morseapp.utils.FileUtils;
import com.zyj.morseapp.utils.PostUtils;
import com.zyj.morseapp.application.MyApplication;
import com.zyj.morseapp.utils.ptt.CWEncoder;
import com.zyj.morseapp.utils.ptt.MyAudio;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sockets extends AppCompatActivity {
    private static TextView output_text =null;
    private static Button send_shortWave=null;
    private static Button send_longWave=null;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //控件初始化
        setContentView(R.layout.activity_socket);
        send_shortWave = findViewById(R.id.send_shortWave);
        send_longWave = findViewById(R.id.send_longWave);

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
//                            System.out.println(str1.substring(str1.length()-100));
//                            System.out.println(end.length);
//                            short[] test=Arrays.copyOfRange(shorts1, i1 * 32000, (i1 + 1) * 32000);
//                            String str3="{\"data\":\"" + Arrays.toString(test) + "\"}";
//                            System.out.println(test.length);
//                            System.out.println(str3.substring(str3.length()-100));
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
                                    show("网络通信失败！");
                                    System.out.println("网络通信失败");
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

                    // ptt脉冲输出
                    System.out.println("ShortCoder.shortMorseContentForPtt:" + ShortCoder.shortMorseContentForPtt);
                    System.out.println("shortWpm:" + shortWpm);
                    try {
                        String shortMorseStr = ShortCoder.shortMorseContentForPtt;
                        MorseAudio morseAudioObj1 = new MorseAudio();
                        short[] shortMorseStrArr = morseAudioObj1.codeConvert2Sound(shortMorseStr, shortWpm);
                        MyAudio.getInstance().playMorse(shortMorseStr, shortWpm);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


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
                                    show("网络通信失败！");
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

                    // ptt脉冲输出
                    System.out.println("LongCoder.longMorseContentForPtt:" + LongCoder.longMorseContentForPtt);
                    System.out.println("longWpm:" + longWpm);
                    try {
                        String longMorseStr = LongCoder.longMorseContentForPtt;
                        MorseAudio morseAudioObj2 = new MorseAudio();
                        short[] longMorseStrArr = morseAudioObj2.codeConvert2Sound(longMorseStr, longWpm);
                        MyAudio.getInstance().playMorse(longMorseStr, longWpm);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


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
                default:
                    break;
            }

        }
    }
}
