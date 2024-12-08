package com.zyj.morseapp.utils;

import android.content.Context;
import android.media.MediaPlayer;

import com.zyj.morseapp.application.MyApplication;
import com.zyj.morseapp.audio.MorseAudio;
import com.zyj.morseapp.pages.HalfDuplex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MorseSender {
    private Context context = MyApplication.getContext();
    private MediaPlayer mediaPlayer = new MediaPlayer();

    /**
     * 发送长码
     * @param content
     * @param wpm
     */
    public void sendLongCode(String content, int wpm) {
        try {
            MorseAudio morseAudio = new MorseAudio();
            short[] shorts = morseAudio.codeConvert2Sound(content, wpm);
            byte[] bytes=new byte[shorts.length*2];
            //大端序转小端序
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
            byte[] header = morseAudio.writeWavFileHeader(shorts.length*2, 8000, 1, 16);

            ByteArrayOutputStream byteArrayOutputStream_WAV = new ByteArrayOutputStream();
            byteArrayOutputStream_WAV.write(header);
            byteArrayOutputStream_WAV.write(bytes);
            byte[] byteArray = byteArrayOutputStream_WAV.toByteArray();
            //创建文件
            String Path_WAV = context.getExternalFilesDir("").getAbsolutePath()+"/morse.wav";
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
            String Path_PCM = context.getExternalFilesDir("").getAbsolutePath()+"/morse.pcm";
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
            //开始播放
            try {
                mediaPlayer.setDataSource(Path_WAV);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(false);  // 设置非循环播放
                mediaPlayer.start(); // 开始播放
                System.out.println("开始播放");
                // 设置播放完成后的监听器
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // 音乐播放完成，进行轮询阻塞
                        mediaPlayer.reset();
                        System.out.println("播放完成！");
                        HalfDuplex.sendLongCodeMessage1 = true;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 短码
     * @param content
     * @param wpm
     */
    public void sendShortCode(String content, int wpm) {
        try {
            MorseAudio morseAudio = new MorseAudio();
            short[] shorts = morseAudio.codeConvert2Sound(content, wpm);
            byte[] bytes=new byte[shorts.length*2];
            //大端序转小端序
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
            byte[] header = morseAudio.writeWavFileHeader(shorts.length*2, 8000, 1, 16);

            ByteArrayOutputStream byteArrayOutputStream_WAV = new ByteArrayOutputStream();
            byteArrayOutputStream_WAV.write(header);
            byteArrayOutputStream_WAV.write(bytes);
            byte[] byteArray = byteArrayOutputStream_WAV.toByteArray();
            //创建文件
            String Path_WAV = context.getExternalFilesDir("").getAbsolutePath()+"/morse.wav";
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
            String Path_PCM = context.getExternalFilesDir("").getAbsolutePath()+"/morse.pcm";
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
            //开始播放
            try {
                mediaPlayer.setDataSource(Path_WAV);
                mediaPlayer.prepare();
                mediaPlayer.setLooping(false);  // 设置非循环播放
                mediaPlayer.start(); // 开始播放
                System.out.println("开始播放");
                // 设置播放完成后的监听器
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        // 音乐播放完成，进行轮询阻塞
                        mediaPlayer.reset();
//                        mediaPlayer.release();
                        System.out.println("播放完成！");
                        HalfDuplex.sendShortCodeMessage2 = true;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


