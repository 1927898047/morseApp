package com.zyj.morseapp.fragment;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

public class LongcodeFragment extends Fragment {
    //视图
    private View view;
    //全局context
    private Context context=null;
    //输出框
    private TextView tv_output;
    //输出框
    private TextView tv_input;
    //编码按钮
    public Button bt_charToMorse;
    //音频按钮
    public Button bt_createAudio;
    //复选框
    private RadioGroup pcmRadioGroup1=null;
    private RadioGroup pcmRadioGroup2=null;

    // 码速（words per minute）
    private int wpm=20;

    private String str_char="";
    private String str_morse="";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(view==null){
            view = inflater.inflate(R.layout.fragment_longcode,container,false);
        }

        //获取上下文对象
        context= MyApplication.getContext();

        //获取控件对象
        tv_output = view.findViewById(R.id.tv_output);
        tv_input = view.findViewById(R.id.tv_input);
        bt_charToMorse = view.findViewById(R.id.bt_charToMorse);
        bt_createAudio = view.findViewById(R.id.bt_createAudio);
        pcmRadioGroup1 = view.findViewById(R.id.pcmRadioGroup1);
        pcmRadioGroup2 = view.findViewById(R.id.pcmRadioGroup2);

        //按钮响应类
        MyOnClick myOnClick=new MyOnClick();
        MyPcm myPcm = new MyPcm();

        //设置监听
        bt_charToMorse.setOnClickListener(myOnClick);
        bt_createAudio.setOnClickListener(myOnClick);
        pcmRadioGroup1.setOnCheckedChangeListener(myPcm);
        pcmRadioGroup2.setOnCheckedChangeListener(myPcm);

        return view;
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
        MorseLongCoder morseLongCoder = new MorseLongCoder();
        MorseAudio morseAudio=new MorseAudio();
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                //字符转摩尔斯码
                case R.id.bt_charToMorse:
                    str_char=tv_input.getText().toString();
                    str_morse= morseLongCoder.encode(str_char);
                    tv_output.setText(str_morse);
                    break;
                case R.id.bt_createAudio:
                    try {
                        short[] shorts = morseAudio.codeConvert2Sound(str_morse,wpm);
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
                        File filesDir = context.getFilesDir();
                        String Path_WAV=filesDir.toString()+"/morse_longCode.wav";
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
                        String Path_PCM=filesDir.toString()+"/morse_longCode.pcm";
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

                        Toast.makeText(context,"音频已生成",Toast.LENGTH_LONG).show();
                        //初始化mediaPlayer
                        MediaPlayer mediaPlayer=new MediaPlayer();
                        mediaPlayer.setDataSource(Path_WAV);
                        mediaPlayer.prepare();
                        mediaPlayer.setLooping(false);  // 设置非循环播放

                        //开始播放
                        mediaPlayer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }

        }

    }

}