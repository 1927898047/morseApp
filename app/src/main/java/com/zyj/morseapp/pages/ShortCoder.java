package com.zyj.morseapp.pages;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zyj.morseapp.R;
import com.zyj.morseapp.utils.FileUtils;
import com.zyj.morseapp.application.MyApplication;
import com.zyj.morseapp.audio.MorseAudio;
import com.zyj.morseapp.morsecoder.MorseShortCoder;
import com.zyj.morseapp.permission.Permission;
import com.zyj.morseapp.utils.ptt.MyAudio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 短码编码界面
 */
public class ShortCoder extends AppCompatActivity {
    public static String shortMorseContentForPtt = "";


    private Button audio_button=null;
    private EditText input_text;
    private Button switch_to_longCoder=null;
    private Button switch_to_socket=null;
    private Button switch_to_record=null;
    private Button half_duplex_button = null;

    private RadioGroup pcmRadioGroup1=null;
    private RadioGroup pcmRadioGroup2=null;

    private Context context=null;
    Intent intent = new Intent();


    String str_char="";
    String str_morse="";

    // 码速（words per minute）
    private int wpm=15;

    // 是否在播放PTT的同时播放音频
    private boolean isPlayAudio;

    // 初始化mediaPlayer
    MediaPlayer mediaPlayer;

    // 信噪比
    public static int gussianNoise = 4000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_short_encoder);

        MyOnClick myOnClick=new MyOnClick();

        //控件初始化
        audio_button=findViewById(R.id.audio_button);

        //页面切换按钮
        switch_to_longCoder = findViewById(R.id.switch_to_longCode_button);
        switch_to_socket = findViewById(R.id.switch_to_socket_button);
        switch_to_record = findViewById(R.id.button_switch_record);
        half_duplex_button = findViewById(R.id.half_duplex_button);
        input_text = findViewById(R.id.input_text);

        //设置监听函数
        audio_button.setOnClickListener(myOnClick);

        switch_to_longCoder.setOnClickListener(myOnClick);
        switch_to_socket.setOnClickListener(myOnClick);
        switch_to_record.setOnClickListener(myOnClick);
        half_duplex_button.setOnClickListener(myOnClick);


        context=MyApplication.getContext();
        Permission.checkPermission(this);

        //持久化设置
        try {
            if(!FileUtils.fileExist(context.getExternalFilesDir("").getAbsolutePath()+"/ip.txt")){
                FileUtils.writeTxt(context.getExternalFilesDir("").getAbsolutePath()+"/ip.txt",Sockets.ip);
            }
            if(!FileUtils.fileExist(context.getExternalFilesDir("").getAbsolutePath()+"/port.txt")){
                FileUtils.writeTxt(context.getExternalFilesDir("").getAbsolutePath()+"/port.txt",Sockets.port);
            }
            Sockets.ip=FileUtils.readTxt(context.getExternalFilesDir("").getAbsolutePath()+"/ip.txt");
            Sockets.port=FileUtils.readTxt(context.getExternalFilesDir("").getAbsolutePath()+"/port.txt");

        } catch (IOException e) {
            e.printStackTrace();
        }

        //初始化mediaPlayer
        mediaPlayer = new MediaPlayer();

        // 信噪比下拉框
        // 在你的Activity或Fragment中
        Spinner spinner = findViewById(R.id.snr_spinner1);
        // 定义下拉列表的选项
        String[] items = new String[] {"无噪声", "1dB", "-5dB", "-7dB","-11dB", "-12dB"};
        // 创建适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);

        // 设置下拉列表的下拉样式（可选）
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // 将适配器设置给Spinner
        spinner.setAdapter(adapter);
        spinner.setSelection(1);
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
                    case "1dB":
                        gussianNoise = 7130;
                        break;
                    case "-5dB":
                        gussianNoise = 14266;
                        break;
                    case "-7dB":
                        gussianNoise = 17910;
                        break;
                    case "-11dB":
                        gussianNoise = 28385;
                        break;
                    case "-12dB":
                        gussianNoise = 31849;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 可以在没有选项被选中时做一些处理
            }
        });


        // 信噪比下拉框
        // 在你的Activity或Fragment中
        Spinner shortCodeSpeedSpinner = findViewById(R.id.shortCodeSpeed_spinner);
        // 定义下拉列表的选项
        String[] itemsForShortCodeSpeed = new String[] {"15wpm", "20wpm", "25wpm", "30wpm","35wpm", "40wpm", "45wpm", "50wpm"};
        // 创建适配器
        ArrayAdapter<String> adapterForShortCodeSpeed = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, itemsForShortCodeSpeed);

        // 设置下拉列表的下拉样式（可选）
        adapterForShortCodeSpeed.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // 将适配器设置给Spinner
        shortCodeSpeedSpinner.setAdapter(adapterForShortCodeSpeed);
        shortCodeSpeedSpinner.setSelection(1);
        // 设置监听器
        shortCodeSpeedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // 获取选中项的文本
                String selectedItemText = (String) parent.getItemAtPosition(position);
                // 根据选中项做进一步处理
                System.out.println(selectedItemText);
                switch (selectedItemText) {
                    case "15wpm":
                        wpm = 15;
                        break;
                    case "20wpm":
                        wpm = 20;
                        break;
                    case "25wpm":
                        wpm = 25;
                        break;
                    case "30wpm":
                        wpm = 30;
                        break;
                    case "35wpm":
                        wpm = 35;
                        break;
                    case "40wpm":
                        wpm = 40;
                        break;
                    case "45wpm":
                        wpm = 45;
                        break;
                    case "50wpm":
                        wpm = 50;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 可以在没有选项被选中时做一些处理
            }
        });


        input_text.setMovementMethod(ScrollingMovementMethod.getInstance());
        input_text.setMaxLines(Integer.MAX_VALUE); // 设置足够大的行数以容纳所有文本
        input_text.setScrollContainer(true);
        input_text.setFocusable(true);
        input_text.setSelected(true);

        input_text.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    view.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false;
            }
        });

        try {
            isPlayAudio = FileUtils.readTxt(context.getExternalFilesDir("").getAbsolutePath()+"/isPlayAudio.txt").equals("1");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 1.字符转摩尔斯码
     * 2.生成摩尔斯音频
     * 3.切换界面
     */
    class MyOnClick implements View.OnClickListener  {

        MorseShortCoder morseShortCoder = new MorseShortCoder();
        MorseAudio morseAudio=new MorseAudio();
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.audio_button:
                    try {
                        // 字符转摩尔斯
                        str_char = input_text.getText().toString();
                        shortMorseContentForPtt = str_morse;

                        String regex = ".*[a-zA-z].*";
                        if(str_char.equals("")){
                            Toast.makeText(ShortCoder.this,"输入内容不能为空！",Toast.LENGTH_LONG).show();
                        }
                        else if(str_char.matches(regex)){
                            Toast.makeText(ShortCoder.this,"输入内容不能包含英文！",Toast.LENGTH_LONG).show();
                            break;
                        }
                        else{
                            str_morse= morseShortCoder.encode(str_char);
                        }
                        shortMorseContentForPtt = str_morse;


                        // 生成音频播放
                        if(str_morse==null || str_char.equals("")){
                            Toast.makeText(ShortCoder.this,"输入内容为空，无法进行编码！",Toast.LENGTH_LONG).show();
                            break;
                        }
                        morseAudio.setChangeSnr(true);
                        morseAudio.setGussianNoise(gussianNoise);
                        short[] shorts = morseAudio.codeConvert2Sound(str_morse,wpm);
                        byte[] bytes=new byte[shorts.length*2];
                        //大端序转小端序
                        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
                        byte[] header = morseAudio.writeWavFileHeader(shorts.length*2, 8000, 1, 16);

                        ByteArrayOutputStream byteArrayOutputStream_WAV = new ByteArrayOutputStream();
                        byteArrayOutputStream_WAV.write(header);
                        byteArrayOutputStream_WAV.write(bytes);
                        byte[] byteArray = byteArrayOutputStream_WAV.toByteArray();
                        //创建文件
                        String Path_WAV=getExternalFilesDir("").getAbsolutePath()+"/morse_shortCode.wav";
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
                        String Path_PCM=getExternalFilesDir("").getAbsolutePath()+"/morse_shortCode.pcm";
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

                        Toast.makeText(ShortCoder.this,"音频已生成",Toast.LENGTH_LONG).show();

                        try {
                            isPlayAudio = FileUtils.readTxt(context.getExternalFilesDir("").getAbsolutePath()+"/isPlayAudio.txt").equals("1");
                            System.out.println("isPlayAudio:" + isPlayAudio);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // 异步播放音频
                        if (isPlayAudio) {
                            new Thread(){
                                @Override
                                public void run() {

                                    try {
                                        mediaPlayer.setDataSource(Path_WAV);
                                        mediaPlayer.prepare();
                                        mediaPlayer.setLooping(false);  // 设置非循环播放
                                        //开始播放
                                        mediaPlayer.start();
                                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                            @Override
                                            public void onCompletion(MediaPlayer mp) {
                                                // 音乐播放完成，进行轮询阻塞
                                                mediaPlayer.reset();
                                            }
                                        });
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }.start();
                        }

                        //异步发送 ptt脉冲输出
                        System.out.println("ShortCoder.shortMorseContentForPtt:" + str_morse);
                        System.out.println("shortWpm:" + wpm);
                        new Thread(){
                            @Override
                            public void run() {
                                MorseAudio morseAudioObj1 = new MorseAudio();
                                short[] shortMorseStrArr = morseAudioObj1.codeConvert2Sound(str_morse, wpm);
                                MyAudio.getInstance().playMorse(str_morse, wpm);
                            }
                        }.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                    //切换到下一个界面
                case R.id.switch_to_longCode_button:
                    //前一个（MainActivity.this）是目前页面，后面一个是要跳转的下一个页面
                    intent.setClass(ShortCoder.this, LongCoder.class);
                    startActivity(intent);
                    break;
                case R.id.switch_to_socket_button:
                    //前一个（MainActivity.this）是目前页面，后面一个是要跳转的下一个页面
                    intent.setClass(ShortCoder.this, Sockets.class);
                    startActivity(intent);
                    break;
                case R.id.button_switch_record:
//                    前一个（MainActivity.this）是目前页面，后面一个是要跳转的下一个页面
                    intent.setClass(ShortCoder.this, AudioRecords.class);
                    startActivity(intent);
                    break;
                case R.id.half_duplex_button:
                    intent.setClass(ShortCoder.this, HalfDuplex.class);
                    startActivity(intent);
                default:
                    break;
            }

        }

    }
}