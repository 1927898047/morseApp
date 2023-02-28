package com.zyj.morseapp.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.zyj.morseapp.R;
import com.zyj.morseapp.fragment.AudiorecordFragment;
import com.zyj.morseapp.fragment.LongcodeFragment;
import com.zyj.morseapp.fragment.SettingsFragment;
import com.zyj.morseapp.fragment.ShortcodeFragment;
import com.zyj.morseapp.fragment.SocketFragment;
import com.zyj.morseapp.permission.Permission;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements RadioGroup.OnCheckedChangeListener {

    private RadioButton rb_shortCode,rb_longCode,rb_audio,rb_socket,rb_settings;
    private RadioGroup rg_group;
    private List<Fragment> fragments;
    private int position=0;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rb_shortCode=findViewById(R.id.rb_shortCode);
        rb_longCode=findViewById(R.id.rb_longCode);
        rb_audio=findViewById(R.id.rb_audio);
        rb_socket=findViewById(R.id.rb_socket);
        rb_settings=findViewById(R.id.rb_settings);
        rg_group=findViewById(R.id.rg_group);

        //默认选中第一个
        rb_shortCode.setSelected(true);
        rg_group.setOnCheckedChangeListener(this);

        //初始化fragment
        initFragment();

        //默认布局，选第一个
        defaultFragment();

        //权限初始化
        Permission.checkPermission(this);
    }

    private void defaultFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_layout,fragments.get(0));
        transaction.commit();
    }

    private void setSelected() {
        rb_shortCode.setSelected(false);
        rb_longCode.setSelected(false);
        rb_audio.setSelected(false);
        rb_socket.setSelected(false);
        rb_settings.setSelected(false);
    }

    private void initFragment() {
        fragments = new ArrayList<>();
        fragments.add(0,new ShortcodeFragment());
        fragments.add(1,new LongcodeFragment());
        fragments.add(2,new AudiorecordFragment());
        fragments.add(3,new SocketFragment());
        fragments.add(4,new SettingsFragment());
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int i) {
        //获取fragment管理类对象
        FragmentManager fragmentManager = getSupportFragmentManager();
        //拿到fragmentManager的触发器
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        switch (i){
            case R.id.rb_shortCode:
                position=0;
                //调用replace方法，将fragment,替换到fragment_layout这个id所在UI，或者这个控件上面来
                //这是创建replace这个事件，如果想要这个事件执行，需要把这个事件提交给触发器
                //用commit()方法
                transaction.replace(R.id.fragment_layout,fragments.get(0));
                //将所有导航栏设成默认色
                setSelected();
                rb_shortCode.setSelected(true);
                break;
            case R.id.rb_longCode:
                position=1;
                transaction.replace(R.id.fragment_layout,fragments.get(1));
                //将所有导航栏设成默认色
                setSelected();
                rb_longCode.setSelected(true);
                break;
            case R.id.rb_audio:
                position=2;
                transaction.replace(R.id.fragment_layout,fragments.get(2));
                //将所有导航栏设成默认色
                setSelected();
                rb_audio.setSelected(true);
                break;
            case R.id.rb_socket:
                position=3;
                transaction.replace(R.id.fragment_layout,fragments.get(3));
                //将所有导航栏设成默认色
                setSelected();
                rb_socket.setSelected(true);
                break;
            case R.id.rb_settings:
                position=4;
                transaction.replace(R.id.fragment_layout,fragments.get(4));
                //将所有导航栏设成默认色
                setSelected();
                rb_settings.setSelected(true);
                break;
        }
        //事件的提交
        transaction.commit();
    }
}
