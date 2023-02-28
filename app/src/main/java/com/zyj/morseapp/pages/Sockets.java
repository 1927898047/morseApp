package com.zyj.morseapp.pages;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zyj.morseapp.R;
import com.zyj.morseapp.Utils.ArraysUtils;
import com.zyj.morseapp.Utils.FileUtils;
import com.zyj.morseapp.Utils.PostUtils;
import com.zyj.morseapp.application.MyApplication;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Sockets extends AppCompatActivity {
    private TextView output_text =null;
    private Button send_toServer=null;
    private Context context=null;
//    public static String ip="http://morse.jinmensuyin.com:80/upload_json";
//    public static String port="80";
    public static String port="5000";
    public static String ip="127.0.0.1";


    private Button bt_settings;
    private EditText et_port;
    private EditText et_ip;
    private static String ipTxt;
    private static String portTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //控件初始化
        setContentView(R.layout.activity_socket);

        send_toServer = findViewById(R.id.send_wav_toServer_button);
        bt_settings=findViewById(R.id.bt_settings);
        et_port=findViewById(R.id.et_port);
        et_ip=findViewById(R.id.et_ip);


        send_toServer.setOnClickListener(new MyOnClick());
        et_port.setOnClickListener(new MyOnClick());
        et_ip.setOnClickListener(new MyOnClick());
        bt_settings.setOnClickListener(new MyOnClick());

        context= MyApplication.getContext();

        ipTxt=context.getExternalFilesDir("").getAbsolutePath()+"/ip.txt";
        portTxt=context.getExternalFilesDir("").getAbsolutePath()+"/port.txt";


        //持久化ip设置
        try {
            if(!FileUtils.fileExist(ipTxt)){
                FileUtils.writeTxt(ipTxt,ip);
            }
            if(!FileUtils.fileExist(portTxt)){
                FileUtils.writeTxt(portTxt,port);
            }
            ip=FileUtils.readTxt(ipTxt);
            port=FileUtils.readTxt(portTxt);
            System.out.println(ip);
            System.out.println(port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        et_ip.setText(ip);
        et_port.setText(port);
    }

    class MyOnClick implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            output_text =findViewById(R.id.serverReturn);
            String path= context.getExternalFilesDir("").getAbsolutePath()+"/morse_shortCode.wav";
            System.out.println(path);
            ExecutorService exec = Executors.newSingleThreadScheduledExecutor();
            switch (view.getId()){
                //网络通信：发送wav文件，显示返回内容
                case R.id.send_wav_toServer_button:
                    if(!new File(path).exists()){
                        Toast.makeText(Sockets.this,"音频文件不存在，发送失败！",Toast.LENGTH_LONG).show();
                        break;
                    }
                    send_toServer.setText("正在发送文件......");
                    Toast.makeText(Sockets.this,"正在发送文件......",Toast.LENGTH_LONG).show();
                    byte[] bytes1 = FileUtils.fileToByteArr(path);
                    short[] shorts1 = ArraysUtils.byteToShortInBigEnd(bytes1);
                    String str="";
                    int i=0;
                    while(i*32000<shorts1.length) {
                        short[] shorts2 = Arrays.copyOfRange(shorts1, i * 32000, (i + 1) * 32000);
                        str = "{\"data\":\"" + Arrays.toString(shorts2) + "\"}";
                        exec.submit(new PostUtils(str));
                        exec.submit(new Thread(){
                            @Override
                            public void run() {
                                output_text.setText(PostUtils.msg);
                            }
                        });
                        i++;
                    }
                    exec.submit(new Thread(){
                        @Override
                        public void run() {
                            send_toServer.setText("点击发送wav文件");
                        }
                    });
                    exec.shutdown();
                    break;
                case R.id.bt_settings:
                    ip=et_ip.getText().toString();
                    port=et_port.getText().toString();
                    et_ip.setText(ip);
                    et_port.setText(port);

                    try {
                        FileUtils.writeTxt(ipTxt,ip);
                        FileUtils.writeTxt(portTxt,port);
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
