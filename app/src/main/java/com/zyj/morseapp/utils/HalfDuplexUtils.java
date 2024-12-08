package com.zyj.morseapp.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;

import com.zyj.morseapp.application.MyApplication;
import com.zyj.morseapp.audio.MorseAudio;
import com.zyj.morseapp.morsecoder.MorseLongCoder;
import com.zyj.morseapp.morsecoder.MorseShortCoder;
import com.zyj.morseapp.pages.HalfDuplex;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class HalfDuplexUtils {
    //全局context
    private static Context context=MyApplication.getContext();

    private static AudioRecord audioRecord = null;
    private static int recordBufSize = 16000*4;
    private static byte data[];
    private static byte dataNew[];

    private PcmToWavUtils tool;
    private volatile boolean isRecording = false;
    //录音得到的文件 的储存位置及文件名
    private  String pcmFileName = context.getExternalFilesDir("").getAbsolutePath()+"/record.pcm";
    //转换成wav文件后新文件的存储位置及文件名
    private  String wavFileName = context.getExternalFilesDir("").getAbsolutePath()+"/record.wav";
    // 音频源：音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    // 采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 8000;
    // 声道设置：android支持双声道立体声和单声道。MONO单声道，STEREO立体声
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    // 采样深度：16bit
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区存储的音频数据大小：秒
    private final int time = 4;
    // 设备id
    private static String deviceId;
    // 码速
    private int wpm = 15;
    //short转data
    private short getShort(byte[] data, int start)
    {
        return (short)((data[start] & 0xFF) | (data[start+1] << 8));
    }

    public void setWpm(int wpm) {
        this.wpm = wpm;
    }

    private MorseShortCoder morseShortCoder = new MorseShortCoder();
    private MorseLongCoder morseLongCoder = new MorseLongCoder();
    private MorseAudio morseAudio = new MorseAudio();
    // 录音和通信线程
    public MyThread myThread = null;
    // 指定线程是否执行
    public volatile boolean isStoped = false;
    // 指定是否在播放声音
    private volatile boolean isPlayingMusic = false;
    // 线程池
    public ExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    //调整pcm音量
    public int amplifyPCMData(byte[] pData, int nLen, byte[] data2, int nBitsPerSample, float multiple)
    {
        int nCur = 0;
        if (16 == nBitsPerSample)
        {
            while (nCur < nLen)
            {
                short volum = getShort(pData, nCur);

                volum = (short)(volum * multiple);

                data2[nCur]   = (byte)( volum       & 0xFF);
                data2[nCur+1] = (byte)((volum >> 8) & 0xFF);
                nCur += 2;
            }

        }
        return 0;
    }

    /**
     * 初始化录音对象
     */
    public HalfDuplexUtils() {
        recordBufSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);  //audioRecord能接受的最小的buffer大小
        //缓冲区大小,16000是存储1s的数据需要的缓冲区字节大小：一秒8000样本，每个样本2byte大小，一秒16000byte字节大小
        //16bit，一秒8000个样本，即16000个byte
        data = new byte[recordBufSize];
        dataNew = new byte[recordBufSize];
        tool = new PcmToWavUtils(AUDIO_SAMPLE_RATE,AUDIO_CHANNEL,AUDIO_ENCODING);
        try{
            if(audioRecord == null){
                System.out.println("生成HalfDuplexUtils对象");
                //构造方法，传入的参数上面在有解析
                audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, recordBufSize);
            }
        }
        catch (SecurityException e){
            e.printStackTrace();
        }
    }

    /**
     * 开始录音
     */
    public void start(){
        Log.i("AUDIO","开始录音");
        isRecording = true;
        //调用startRecording()方法开始录制
        audioRecord.startRecording();
        myThread = new MyThread();
        myThread.start();
    }

    /**
     * 停止录音
     */
    public void stop(){
        isRecording = false;
        Log.i("AUDIO","停止录音");
        //调用stop()方法停止录制
        audioRecord.stop();
        exec.shutdownNow();
        //利用自定义工具类将pcm格式的文件转换为wav格式文件才能进行播放
//        tool.pcmToWav(pcmFileName,wavFileName);
    }

    /**
     * 录音时需要运行的子线程
     */
    private class MyThread extends Thread{
        @Override
        public void run() {
                FileOutputStream os = null;
                File file=new File(pcmFileName);
                try {
                    //如果文件不存在，就创建文件
                    if(file.exists()){
                        file.delete();
                    }
                    //创建MorseCode.wav文件
                    file.createNewFile();
                    os = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (null != os) {
                    String str = "";
                    while (isRecording) {
                        // 从音频硬件读取音频数据，以便记录到字节数组中,阻塞式
                        long startTime = System.currentTimeMillis();
                        int read = audioRecord.read(data, 0, recordBufSize);
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;
                        System.out.println("录音缓冲区读取时长：" + duration);

                        //调整音量大小
                        amplifyPCMData(data, data.length, dataNew, 16, (float) Math.pow(10, (double) 5 / 20));
                        short[] shortData = ArraysUtils.byteToShortInBigEnd(dataNew);
                        str = "{\"data\":\"" + Arrays.toString(shortData) + "\"}";
                        exec.submit(new PostUtils(str));
                        exec.submit(new Thread() {
                            @Override
                            public void run() {
                                if (PostUtils.code != 200) {
                                    HalfDuplex.show("网络通信失败！");
                                } else if (PostUtils.code == 200) {
                                    JSONArray content;
                                    String talkContent;
                                    MediaPlayer mediaPlayer = HalfDuplex.mediaPlayer;
                                    // 响应内容
                                    String recv_content = "";
                                    try {
                                        //处理json对象
                                        JSONObject object = new JSONObject(PostUtils.msg);
                                        //收到短码
                                        if (object.has("content")) {
                                            content = object.getJSONArray("content");
                                            String shortContent = content.toString();
                                            HalfDuplex.rec_content = shortContent;
                                            System.out.println("content:" + content);
                                            // 对方发送结束后，判断报文内容
                                            if (shortContent.contains("+ +")) {
                                                String dest_deviceId = StringUtils.getId1FromShortCode(shortContent);
                                                String src_deviceId = StringUtils.getId2FromShortCode(shortContent);
                                                deviceId = FileUtils.readTxt(context.getExternalFilesDir("").getAbsolutePath() + "/deviceId.txt");

                                                // 设备号匹配
                                                if (!Objects.isNull(dest_deviceId) && deviceId.equals(dest_deviceId)) {
                                                    // 显示解码结果
                                                    HalfDuplex.show(PostUtils.msg);
                                                    // 呼叫接收
                                                    // 构造返回内容
                                                    recv_content = "R R R " + src_deviceId + " "
                                                            + "DE " + deviceId + " "
                                                            + "OK "
                                                            + "9801 "
                                                            + "Ready "
                                                            + "K K K";
                                                    System.out.println("recv_content:" + recv_content);
                                                    String str_char = recv_content;
                                                    String str_morse = null;
                                                    // 短码译码
                                                    str_morse = morseShortCoder.encode(str_char);
                                                    System.out.println("译码结束！");
                                                    // 播放音频
                                                    try {
                                                        System.out.println("准备音频！");
                                                        short[] shorts = morseAudio.codeConvert2Sound(str_morse, wpm);
                                                        byte[] bytes = new byte[shorts.length * 2];
                                                        //大端序转小端序
                                                        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
                                                        byte[] header = morseAudio.writeWavFileHeader(shorts.length * 2, 8000, 1, 16);

                                                        ByteArrayOutputStream byteArrayOutputStream_WAV = new ByteArrayOutputStream();
                                                        byteArrayOutputStream_WAV.write(header);
                                                        byteArrayOutputStream_WAV.write(bytes);
                                                        byte[] byteArray = byteArrayOutputStream_WAV.toByteArray();
                                                        //创建文件
                                                        String Path_WAV = context.getExternalFilesDir("").getAbsolutePath() + "/morse.wav";
                                                        System.out.println(Path_WAV);
                                                        File file_WAV = new File(Path_WAV);
                                                        if (file_WAV.exists()) {
                                                            file_WAV.delete();
                                                        }
                                                        file_WAV.createNewFile();//创建MorseCode.wav文件
                                                        //覆盖写入
                                                        OutputStream os_WAV = new FileOutputStream(file_WAV);
                                                        os_WAV.write(byteArray);
                                                        os_WAV.close();

                                                        //生成pcm文件
                                                        System.out.println("生成pcm文件！");
                                                        ByteArrayOutputStream byteArrayOutputStream_PCM = new ByteArrayOutputStream();
                                                        byteArrayOutputStream_PCM.write(bytes);
                                                        byteArray = byteArrayOutputStream_PCM.toByteArray();
                                                        //创建文件
                                                        String Path_PCM = context.getExternalFilesDir("").getAbsolutePath() + "/morse.pcm";
                                                        System.out.println(Path_PCM);
                                                        File file_PCM = new File(Path_PCM);
                                                        if (file_PCM.exists()) {
                                                            file_PCM.delete();
                                                        }
                                                        file_PCM.createNewFile();//创建MorseCode.wav文件
                                                        //覆盖写入
                                                        OutputStream os_PCM = new FileOutputStream(file_PCM);
                                                        os_PCM.write(byteArray);
                                                        os_PCM.close();
                                                        //初始化mediaPlayer
                                                        System.out.println("开始播放音频！");
                                                        mediaPlayer.reset();
                                                        mediaPlayer.setDataSource(Path_WAV);
                                                        mediaPlayer.prepare();
                                                        //开始播放
                                                        mediaPlayer.start();
                                                        // 设置播放完成后的监听器
                                                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                            @Override
                                                            public void onCompletion(MediaPlayer mp) {
                                                                // 音乐播放完成，进行轮询阻塞
                                                                mediaPlayer.reset();
                                                                System.out.println("播放完成！");
                                                            }
                                                        });
                                                        System.out.println("关闭线程池");
                                                        exec.shutdownNow();
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        }
                                        //收到长码
                                        else if (object.has("talkContent")) {
                                            talkContent = object.getString("talkContent");
                                            if (!talkContent.equals("")) {
                                                // 对方发送结束后，判断报文内容
                                                if (talkContent.contains("K K")) {
                                                    HalfDuplex.rec_content = talkContent;
                                                    String src_deviceId = StringUtils.getId1FromLongCode(talkContent);
                                                    String dest_deviceId = StringUtils.getId2FromLongCode(talkContent);
                                                    deviceId = FileUtils.readTxt(context.getExternalFilesDir("").getAbsolutePath() + "/deviceId.txt");
                                                    System.out.println("deviceId:" + deviceId);
                                                    System.out.println("dest_deviceId:" + dest_deviceId);
                                                    System.out.println("src_deviceId:" + src_deviceId);
                                                    // 设备号匹配
                                                    if (!Objects.isNull(dest_deviceId) && deviceId.equals(dest_deviceId)) {
                                                        // 显示解码结果
                                                        HalfDuplex.show(PostUtils.msg);
                                                        // 呼叫接收
                                                        // 构造返回内容
                                                        recv_content = "R R R " + src_deviceId + " "
                                                                + "DE " + deviceId + " "
                                                                + "Ready K K K";
                                                        System.out.println("recv_content:" + recv_content);
                                                        String str_char = recv_content;
                                                        String str_morse = null;
                                                        // 短码译码
                                                        str_morse = morseLongCoder.encode(str_char);
                                                        System.out.println("译码结束！");
                                                        // 播放音频
                                                        try {
                                                            System.out.println("准备音频！");
                                                            short[] shorts = morseAudio.codeConvert2Sound(str_morse, wpm);
                                                            byte[] bytes = new byte[shorts.length * 2];
                                                            //大端序转小端序
                                                            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts);
                                                            byte[] header = morseAudio.writeWavFileHeader(shorts.length * 2, 8000, 1, 16);

                                                            ByteArrayOutputStream byteArrayOutputStream_WAV = new ByteArrayOutputStream();
                                                            byteArrayOutputStream_WAV.write(header);
                                                            byteArrayOutputStream_WAV.write(bytes);
                                                            byte[] byteArray = byteArrayOutputStream_WAV.toByteArray();
                                                            //创建文件
                                                            String Path_WAV = context.getExternalFilesDir("").getAbsolutePath() + "/morse.wav";
                                                            System.out.println(Path_WAV);
                                                            File file_WAV = new File(Path_WAV);
                                                            if (file_WAV.exists()) {
                                                                file_WAV.delete();
                                                            }
                                                            file_WAV.createNewFile();//创建MorseCode.wav文件
                                                            //覆盖写入
                                                            OutputStream os_WAV = new FileOutputStream(file_WAV);
                                                            os_WAV.write(byteArray);
                                                            os_WAV.close();

                                                            //生成pcm文件
                                                            System.out.println("生成pcm文件！");
                                                            ByteArrayOutputStream byteArrayOutputStream_PCM = new ByteArrayOutputStream();
                                                            byteArrayOutputStream_PCM.write(bytes);
                                                            byteArray = byteArrayOutputStream_PCM.toByteArray();
                                                            //创建文件
                                                            String Path_PCM = context.getExternalFilesDir("").getAbsolutePath() + "/morse.pcm";
                                                            System.out.println(Path_PCM);
                                                            File file_PCM = new File(Path_PCM);
                                                            if (file_PCM.exists()) {
                                                                file_PCM.delete();
                                                            }
                                                            file_PCM.createNewFile();//创建MorseCode.wav文件
                                                            //覆盖写入
                                                            OutputStream os_PCM = new FileOutputStream(file_PCM);
                                                            os_PCM.write(byteArray);
                                                            os_PCM.close();

                                                            //初始化mediaPlayer
                                                            System.out.println("开始播放音频！");
                                                            mediaPlayer.setDataSource(Path_WAV);
                                                            mediaPlayer.prepare();
                                                            mediaPlayer.start();
                                                            // 设置播放完成后的监听器
                                                            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                                                @Override
                                                                public void onCompletion(MediaPlayer mp) {
                                                                    // 音乐播放完成，进行轮询阻塞
                                                                    mediaPlayer.reset();
                                                                    System.out.println("播放完成！");
                                                                    HalfDuplex.sendShortCodeMessage2 = true;
                                                                }
                                                            });
                                                        } catch (IOException e) {
                                                            e.printStackTrace();
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                PostUtils.code = 0;
                                System.out.println("关闭录音！");
                                audioRecord.stop();
                                System.out.println("线程执行结束！");

                            }
                        });
                    }

                    exec.shutdown();
                    try {
                        //关闭文件
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }
    }
}

