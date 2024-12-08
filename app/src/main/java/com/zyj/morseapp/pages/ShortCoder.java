package com.zyj.morseapp.pages;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
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


    private Button char_to_morse_button=null;
    private Button morse_to_char_button=null;
    private Button audio_button=null;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_short_encoder);


        MyOnClick myOnClick=new MyOnClick();

        //控件初始化
        char_to_morse_button=findViewById(R.id.char_to_morse_button);
        audio_button=findViewById(R.id.audio_button);

        //页面切换按钮
        switch_to_longCoder = findViewById(R.id.switch_to_longCode_button);
        switch_to_socket = findViewById(R.id.switch_to_socket_button);
        switch_to_record = findViewById(R.id.button_switch_record);
        half_duplex_button = findViewById(R.id.half_duplex_button);

        pcmRadioGroup1 = findViewById(R.id.pcmRadioGroup1);
        pcmRadioGroup2 = findViewById(R.id.pcmRadioGroup2);

        //设置监听函数
        audio_button.setOnClickListener(myOnClick);
        char_to_morse_button.setOnClickListener(myOnClick);

        switch_to_longCoder.setOnClickListener(myOnClick);
        switch_to_socket.setOnClickListener(myOnClick);
        switch_to_record.setOnClickListener(myOnClick);
        half_duplex_button.setOnClickListener(myOnClick);

        pcmRadioGroup1.setOnCheckedChangeListener(new MyPcm());
        pcmRadioGroup2.setOnCheckedChangeListener(new MyPcm());
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
    }


    /**
     * WPM控件响应
     */
    class MyPcm implements RadioGroup.OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId){
                case R.id.wpmButton_15:
                    wpm=15;
                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_20:
                    wpm=20;
                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_25:
                    wpm=25;
                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_30:
                    wpm=30;
                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_35:
                    wpm=35;
                    pcmRadioGroup1.clearCheck();
                    break;
                case R.id.wpmButton_40:
                    wpm=40;
                    pcmRadioGroup1.clearCheck();
                    break;
                case R.id.wpmButton_45:
                    wpm=45;
                    pcmRadioGroup1.clearCheck();
                    break;
                case R.id.wpmButton_50:
                    wpm=50;
                    pcmRadioGroup1.clearCheck();
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * 1.字符转摩尔斯码
     * 2.生成摩尔斯音频
     * 3.切换界面
     */
    class MyOnClick implements View.OnClickListener  {
        EditText input_text=findViewById(R.id.input_text);
        TextView output_text =findViewById(R.id.output);
        MorseShortCoder morseShortCoder = new MorseShortCoder();
        MorseAudio morseAudio=new MorseAudio();
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                //字符转摩尔斯码
                case R.id.char_to_morse_button:

                    str_char=input_text.getText().toString();
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
                        output_text.setText(str_morse);
                    }
                    shortMorseContentForPtt = str_morse;

                    break;
                case R.id.audio_button:
                    try {
                        if(str_morse==null || str_char.equals("")){
                            Toast.makeText(ShortCoder.this,"输入内容为空，无法进行编码！",Toast.LENGTH_LONG).show();
                            break;
                        }
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
                                    //初始化mediaPlayer
                                    MediaPlayer mediaPlayer=new MediaPlayer();
                                    try {
                                        mediaPlayer.setDataSource(Path_WAV);
                                        mediaPlayer.prepare();
                                        mediaPlayer.setLooping(false);  // 设置非循环播放
                                        //开始播放
                                        mediaPlayer.start();
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