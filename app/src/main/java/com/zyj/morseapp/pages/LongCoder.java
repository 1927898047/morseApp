package com.zyj.morseapp.pages;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.zyj.morseapp.R;
import com.zyj.morseapp.application.MyApplication;
import com.zyj.morseapp.audio.MorseAudio;
import com.zyj.morseapp.morsecoder.MorseLongCoder;
import com.zyj.morseapp.utils.FileUtils;
import com.zyj.morseapp.utils.ptt.MyAudio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 长码编码界面
 */
public class LongCoder extends AppCompatActivity {

    private Button char_to_morse_button=null;
    private Button audio_button=null;
    private Button switch_nextPage=null;
    private RadioGroup pcmRadioGroup1=null;
    private RadioGroup pcmRadioGroup2=null;
    private Button switch_to_socket_button=null;
    private Button button_switch_record=null;
    private Button half_duplex_button = null;
    private Context context=null;
    public static String longMorseContentForPtt = "";
    String str_char="";
    String str_morse="";


    // 码速（words per minute）
    public int wpm=15;

    // 是否在播放PTT的同时播放音频
    private boolean isPlayAudio;

    MediaPlayer mediaPlayer;

    Intent intent=null;

    // 信噪比
    public static int gussianNoise = 4000;

    public void setWpm(int val){
        this.wpm=val;
        System.out.println("wpm:"+wpm);
    }

    public int getWpm(){
        System.out.println("wpm:"+this.wpm);
        return this.wpm;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_long_encoder);
        intent = new Intent();

        MyOnClick myOnClick=new MyOnClick();
        MyPcm myPcm = new MyPcm();

        //控件初始化
        char_to_morse_button=findViewById(R.id.char_to_morse_button);
        audio_button=findViewById(R.id.audio_button);
        switch_nextPage = findViewById(R.id.switch_to_shortCode_button);
        half_duplex_button = findViewById(R.id.half_duplex_button);

        pcmRadioGroup1 = findViewById(R.id.pcmRadioGroup1);
        pcmRadioGroup2 = findViewById(R.id.pcmRadioGroup2);

        switch_to_socket_button=findViewById(R.id.switch_to_socket);
        button_switch_record=findViewById(R.id.switch_to_record);

        //设置监听回调函数
        audio_button.setOnClickListener(myOnClick);
        char_to_morse_button.setOnClickListener(myOnClick);
        switch_nextPage.setOnClickListener(myOnClick);
        half_duplex_button.setOnClickListener(myOnClick);

        pcmRadioGroup1.setOnCheckedChangeListener(new MyPcm());
        pcmRadioGroup2.setOnCheckedChangeListener(new MyPcm());

        switch_to_socket_button.setOnClickListener(myOnClick);
        button_switch_record.setOnClickListener(myOnClick);
        context= MyApplication.getContext();

        mediaPlayer = new MediaPlayer();

        // 信噪比下拉框
        // 在你的Activity或Fragment中
//        Spinner spinner = findViewById(R.id.snr_spinner2);
//        // 定义下拉列表的选项
//        String[] items = new String[] {"无噪声", "1dB", "-5dB", "-7dB","-11dB", "-12dB"};
//        // 创建适配器
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
//
//        // 设置下拉列表的下拉样式（可选）
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        // 将适配器设置给Spinner
//        spinner.setAdapter(adapter);
//        spinner.setSelection(1);
//        // 设置监听器
//        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                // 获取选中项的文本
//                String selectedItemText = (String) parent.getItemAtPosition(position);
//                // 根据选中项做进一步处理
//                System.out.println(selectedItemText);
//                switch (selectedItemText) {
//                    case "无噪声":
//                        gussianNoise = 0;
//                        break;
//                    case "1dB":
//                        gussianNoise = 7130;
//                        break;
//                    case "-5dB":
//                        gussianNoise = 14266;
//                        break;
//                    case "-7dB":
//                        gussianNoise = 17910;
//                        break;
//                    case "-11dB":
//                        gussianNoise = 28385;
//                        break;
//                    case "-12dB":
//                        gussianNoise = 31849;
//                        break;
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // 可以在没有选项被选中时做一些处理
//            }
//        });
    }

    /**
     * WPM控件响应
     */
    class MyPcm implements RadioGroup.OnCheckedChangeListener{
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (checkedId){
                case R.id.wpmButton_15:
                    setWpm(15);
                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_20:
                    setWpm(20);

                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_25:
                    setWpm(25);


                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_30:
                    setWpm(30);

                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_35:
                    setWpm(35);


                    pcmRadioGroup1.clearCheck();
                    break;
                case R.id.wpmButton_40:
                    setWpm(40);

                    pcmRadioGroup1.clearCheck();
                    break;
                case R.id.wpmButton_45:
                    setWpm(45);
                    pcmRadioGroup1.clearCheck();
                    break;
                case R.id.wpmButton_50:
                    setWpm(50);

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
        MorseLongCoder morseLongCoder = new MorseLongCoder();
        MorseAudio morseAudio=new MorseAudio();
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.char_to_morse_button:
                    str_morse="";
                    str_char="";
                    str_char=input_text.getText().toString();
                    if(str_char.equals("")){
                        Toast.makeText(LongCoder.this,"输入内容不能为空！",Toast.LENGTH_LONG).show();
                    }
                    else{
                        str_morse= morseLongCoder.encode(str_char);
                        output_text.setText(str_morse);
                    }
                    longMorseContentForPtt = str_morse;
                    break;
                case R.id.audio_button:
                    try {
                        if(str_morse.equals("") || str_morse==null){
                            Toast.makeText(LongCoder.this,"输入内容为空，无法进行编码！",Toast.LENGTH_LONG).show();
                            break;
                        }
                        morseAudio.setChangeSnr(true);
                        morseAudio.setGussianNoise(gussianNoise);
                        short[] shorts = morseAudio.codeConvert2Sound(str_morse,getWpm());
                        byte[] bytes=new byte[shorts.length*2];
                        //大端序转小端序
                        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
                        byte[] header = morseAudio.writeWavFileHeader(shorts.length*2, 8000, 1, 16);
                        //生成wav文件
                        ByteArrayOutputStream byteArrayOutputStream_WAV = new ByteArrayOutputStream();
                        byteArrayOutputStream_WAV.write(header);
                        byteArrayOutputStream_WAV.write(bytes);
                        byte[] byteArray = byteArrayOutputStream_WAV.toByteArray();
                        //创建文件
                        String Path_WAV=getExternalFilesDir("").getAbsolutePath()+"/morse_longCode.wav";
                        System.out.println(Path_WAV);
                        File file_WAV = new File(Path_WAV);
                        if(file_WAV.exists()){
                            System.out.println("删除morse_longCode.wav");
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
                        String Path_PCM=getExternalFilesDir("").getAbsolutePath()+"/morse_longCode.pcm";
                        System.out.println(Path_PCM);
                        File file_PCM = new File(Path_PCM);
                        if(file_PCM.exists()){
                            System.out.println("删除morse_longCode.pcm");
                            file_PCM.delete();
                        }
                        file_PCM.createNewFile();//创建MorseCode.wav文件
                        //覆盖写入
                        OutputStream os_PCM = new FileOutputStream(file_PCM);
                        os_PCM.write(byteArray);
                        os_PCM.close();

                        try {
                            isPlayAudio = FileUtils.readTxt(context.getExternalFilesDir("").getAbsolutePath()+"/isPlayAudio.txt").equals("1");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (isPlayAudio){
                            // 异步播放音频
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
                        System.out.println("LongCoder.longMorseContentForPtt:" + str_morse);
                        System.out.println("longWpm:" + wpm);
                        new Thread(){
                            @Override
                            public void run() {
                                MorseAudio morseAudioObj2 = new MorseAudio();
                                short[] longMorseStrArr = morseAudioObj2.codeConvert2Sound(str_morse, wpm);
                                MyAudio.getInstance().playMorse(str_morse, wpm);
                            }
                        }.start();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.switch_to_shortCode_button:
                    //监听按钮，如果点击，就跳转
                    //前一个（MainActivity.this）是目前页面，后面一个是要跳转的下一个页面
                    intent.setClass(LongCoder.this, ShortCoder.class);
                    startActivity(intent);
                    break;
                case R.id.switch_to_socket:
                    //前一个（MainActivity.this）是目前页面，后面一个是要跳转的下一个页面
                    intent.setClass(LongCoder.this, Sockets.class);
                    startActivity(intent);
                    break;
                case R.id.switch_to_record:
//                    前一个（MainActivity.this）是目前页面，后面一个是要跳转的下一个页面
                    intent.setClass(LongCoder.this, AudioRecords.class);
                    startActivity(intent);
                    break;
                case R.id.half_duplex_button:
                    intent.setClass(LongCoder.this, HalfDuplex.class);
                    startActivity(intent);
                default:
                    break;
            }
        }
    }
}

