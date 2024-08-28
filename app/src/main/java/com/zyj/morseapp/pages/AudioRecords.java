package com.zyj.morseapp.pages;

import android.content.Context;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zyj.morseapp.R;
import com.zyj.morseapp.utils.AudioRecordUtils;
import com.zyj.morseapp.application.MyApplication;

import com.zyj.morseapp.utils.UploadLongUtils;
import com.zyj.morseapp.utils.UploadShortUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 录音并存储
 */
public class AudioRecords extends AppCompatActivity {
    private static Button startRecord=null;
    private static Button stopRecord=null;
    private static AudioRecordUtils audioRecordUtils;
    public static TextView serverReturn =null;
    private static Context context=null;
    private static Button button_upload_short=null;
    private static Button button_upload_long=null;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records);

        startRecord = findViewById(R.id.button_start_record);
        stopRecord = findViewById(R.id.button_stop_record);
        serverReturn = findViewById(R.id.serverReturn);
        button_upload_short = findViewById(R.id.button_upload_short);
        button_upload_long = findViewById(R.id.button_upload_long);

        startRecord.setOnClickListener(new MyOnClick());
        stopRecord.setOnClickListener(new MyOnClick());
        button_upload_short.setOnClickListener(new MyOnClick());
        button_upload_long.setOnClickListener(new MyOnClick());

        context= MyApplication.getContext();
        audioRecordUtils=new AudioRecordUtils();
        serverReturn.setMovementMethod(ScrollingMovementMethod.getInstance());

    }



    public static void show(String str) {
        serverReturn.post(new Runnable() {
            @Override
            public void run() {
                serverReturn.setText(str);
            }
        });
    }


    class MyOnClick implements View.OnClickListener  {
        @Override
        public void onClick(View view) {
            ExecutorService exec = Executors.newSingleThreadScheduledExecutor();

            switch (view.getId()){
                case R.id.button_start_record:
                    audioRecordUtils.start();
                    startRecord.setText("正在录音...");
                    serverReturn.setText("");
                    Toast.makeText(AudioRecords.this,"正在录音...",Toast.LENGTH_LONG).show();
                    break;
                case R.id.button_stop_record:
                    audioRecordUtils.stop();
                    startRecord.setText("开始录音");
                    Toast.makeText(AudioRecords.this,"录音结束！",Toast.LENGTH_LONG).show();
                    break;
                case R.id.button_upload_short:
                    exec.submit(new UploadShortUtils());
                    exec.submit(new Thread(){
                        @Override
                        public void run() {
                            AudioRecords.show(UploadShortUtils.msg);
                        }
                    });
                    break;
                case R.id.button_upload_long:
                    exec.submit(new UploadLongUtils());
                    exec.submit(new Thread(){
                        @Override
                        public void run() {
                            AudioRecords.show(UploadLongUtils.msg);
                        }
                    });

                    break;
                default:
                    break;
            }
        }
    }
}
