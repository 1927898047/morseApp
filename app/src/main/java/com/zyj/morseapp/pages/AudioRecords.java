package com.zyj.morseapp.pages;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zyj.morseapp.R;
import com.zyj.morseapp.Utils.AudioRecordUtils;
import com.zyj.morseapp.application.MyApplication;

/**
 * 录音并存储
 */
public class AudioRecords extends AppCompatActivity {
    private static Button startRecord=null;
    private static Button stopRecord=null;
    private static AudioRecordUtils audioRecordUtils;
    private static TextView serverReturn =null;
    private static Context context=null;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        startRecord = findViewById(R.id.button_start_record);
        stopRecord = findViewById(R.id.button_stop_record);
        serverReturn = findViewById(R.id.serverReturn);
        startRecord.setOnClickListener(new MyOnClick());
        stopRecord.setOnClickListener(new MyOnClick());

        context= MyApplication.getContext();

        audioRecordUtils=new AudioRecordUtils();
    }


    public static void show(String str){
        new Thread(){
            @Override
            public void run() {
                serverReturn.setText(str);
            }
        }.start();
    }

    class MyOnClick implements View.OnClickListener  {
        @Override
        public void onClick(View view) {
            switch (view.getId()){
                case R.id.button_start_record:
                    audioRecordUtils.start();
                    startRecord.setText("正在录音...");
                    Toast.makeText(AudioRecords.this,"正在录音...",Toast.LENGTH_LONG).show();
                    break;
                case R.id.button_stop_record:
                    audioRecordUtils.stop();
                    startRecord.setText("开始录音");
                    Toast.makeText(AudioRecords.this,"录音结束！",Toast.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        }
    }
}
