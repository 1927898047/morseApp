package com.zyj.morseapp.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.zyj.morseapp.R;
import com.zyj.morseapp.Utils.AudioRecordUtils;
import com.zyj.morseapp.application.MyApplication;
import com.zyj.morseapp.pages.AudioRecords;

public class AudiorecordFragment extends Fragment {
    //全局上下文context
    private static Context context=null;
    //视图
    private View view=null;
    //录音按钮
    private Button bt_record=null;
    //停止录音按钮
    private Button bt_stop=null;
    //输出框
    private static TextView tv_output=null;
    //录音工具类
    private static AudioRecordUtils audioRecordUtils;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(view==null){
            view = inflater.inflate(R.layout.fragment_audio_record,container,false);
        }
        //获取上下文对象
        context= MyApplication.getContext();

        //获取控件对象
        tv_output = view.findViewById(R.id.tv_output);
        bt_record = view.findViewById(R.id.bt_record);
        bt_stop = view.findViewById(R.id.bt_stop);

        //设置监听
        bt_record.setOnClickListener(new MyOnClick());
        bt_stop.setOnClickListener(new MyOnClick());

        //获取录音对象
        audioRecordUtils=new AudioRecordUtils();

        return view;
    }

    public static void show(String str){
//        new Thread(){
//            @Override
//            public void run() {
//                tv_output.setText(str);
//            }
//        }.start();
        tv_output.setText(str);
    }

    class MyOnClick implements View.OnClickListener  {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.bt_record:
                    audioRecordUtils.start();
                    bt_record.setText("正在录音...");
                    Toast.makeText(context,"正在录音...",Toast.LENGTH_LONG).show();
                    break;
                case R.id.bt_stop:
                    audioRecordUtils.stop();
                    bt_record.setText("开始录音");
                    Toast.makeText(context,"录音结束！",Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    }
}
