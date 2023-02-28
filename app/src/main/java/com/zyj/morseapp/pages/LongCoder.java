package com.zyj.morseapp.pages;
import androidx.appcompat.app.AppCompatActivity;

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

import com.zyj.morseapp.R;
import com.zyj.morseapp.application.MyApplication;
import com.zyj.morseapp.audio.MorseAudio;
import com.zyj.morseapp.morsecoder.MorseLongCoder;

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
    private Context context=null;

    String str_char="";
    String str_morse="";

    // 码速（words per minute）
    private int wpm=20;

    Intent intent=null;

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

        pcmRadioGroup1 = findViewById(R.id.pcmRadioGroup1);
        pcmRadioGroup2 = findViewById(R.id.pcmRadioGroup2);

        switch_to_socket_button=findViewById(R.id.switch_to_socket);
        button_switch_record=findViewById(R.id.switch_to_record);

        //设置监听回调函数
        audio_button.setOnClickListener(myOnClick);
        char_to_morse_button.setOnClickListener(myOnClick);
        switch_nextPage.setOnClickListener(myOnClick);

        pcmRadioGroup1.setOnCheckedChangeListener(new MyPcm());
        pcmRadioGroup2.setOnCheckedChangeListener(new MyPcm());

        switch_to_socket_button.setOnClickListener(myOnClick);
        button_switch_record.setOnClickListener(myOnClick);
        context= MyApplication.getContext();

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
                    System.out.println(wpm);
                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_20:
                    wpm=20;
                    System.out.println(wpm);
                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_25:
                    wpm=25;
                    System.out.println(wpm);

                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_30:
                    wpm=30;
                    System.out.println(wpm);

                    pcmRadioGroup2.clearCheck();
                    break;
                case R.id.wpmButton_35:
                    wpm=35;
                    System.out.println(wpm);

                    pcmRadioGroup1.clearCheck();
                    break;
                case R.id.wpmButton_40:
                    wpm=40;
                    System.out.println(wpm);

                    pcmRadioGroup1.clearCheck();
                    break;
                case R.id.wpmButton_45:
                    wpm=45;
                    System.out.println(wpm);

                    pcmRadioGroup1.clearCheck();
                    break;
                case R.id.wpmButton_50:
                    wpm=50;
                    System.out.println(wpm);

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
                    break;
                case R.id.audio_button:
                    try {
                        if(str_morse.equals("") || str_morse==null){
                            Toast.makeText(LongCoder.this,"输入内容为空，无法进行编码！",Toast.LENGTH_LONG).show();
                            break;
                        }
                        short[] shorts = morseAudio.codeConvert2Sound(str_morse,wpm);
                        byte[] bytes=new byte[shorts.length*2];
                        //大端序转小端序
                        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
                        byte[] header = morseAudio.writeWavFileHeader(shorts.length*2, 8000, 1, 16);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        byteArrayOutputStream.write(header);
                        byteArrayOutputStream.write(bytes);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        //创建文件
                        String Path=getExternalFilesDir("").getAbsolutePath()+"/morse_longCode.wav";
                        File file = new File(Path);

                        if(file.exists()){
                            file.delete();
                        }
                        file.createNewFile();//创建MorseCode.wav文件
                        OutputStream os = new FileOutputStream(file);
                        os.write(byteArray);
                        os.close();
                        Toast.makeText(LongCoder.this,"音频已生成",Toast.LENGTH_LONG).show();


                        //初始化mediaPlayer
                        MediaPlayer mediaPlayer=new MediaPlayer();
                        mediaPlayer.setDataSource(Path);
                        mediaPlayer.prepare();
                        mediaPlayer.setLooping(false);  // 设置非循环播放

                        //开始播放
                        mediaPlayer.start();
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
                default:
                    break;
            }
        }
    }
}

