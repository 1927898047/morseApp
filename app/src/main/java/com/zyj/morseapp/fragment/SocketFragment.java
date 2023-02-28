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
import com.zyj.morseapp.Utils.ArraysUtils;
import com.zyj.morseapp.Utils.FileUtils;
import com.zyj.morseapp.Utils.PostUtils;
import com.zyj.morseapp.application.MyApplication;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketFragment extends Fragment {
    private Context context;
    private View view;
    private TextView tv_output;
    private Button bt_send;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(view==null){
            view = inflater.inflate(R.layout.fragment_socket,container,false);
        }

        //获取上下文对象
        context= MyApplication.getContext();

        //获取控件对象
        tv_output = view.findViewById(R.id.tv_output);
        bt_send = view.findViewById(R.id.bt_send);

        //按钮设置监听
        bt_send.setOnClickListener(new MyOnClick());
        return view;
    }

    class MyOnClick implements View.OnClickListener{
        @Override
        public void onClick(View view) {
            String path= context.getFilesDir()+"/morse_shortCode.wav";
            System.out.println(path);
            ExecutorService exec = Executors.newSingleThreadScheduledExecutor();
            switch (view.getId()){
                //网络通信：发送wav文件，显示返回内容
                case R.id.bt_send:
                    bt_send.setText("正在发送文件......");
                    Toast.makeText(context,"正在发送文件......",Toast.LENGTH_LONG).show();
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
                                tv_output.setText(PostUtils.msg);
                            }
                        });
                        i++;
                    }
                    exec.submit(new Thread(){
                        @Override
                        public void run() {
                            bt_send.setText("点击发送wav文件");
                        }
                    });
                    exec.shutdown();
                    break;
                default:
                    break;
            }

        }
    }
}
