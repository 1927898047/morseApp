package com.zyj.morseapp.utils;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.zyj.morseapp.application.MyApplication;
import com.zyj.morseapp.pages.AudioRecords;
import com.zyj.morseapp.utils.socket.PostUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AudioRecordUtils{
    //全局context
    private static Context context=MyApplication.getContext();
    //用于子线程更改UI
//    final TextView txtTime1 = inflater.inflate(R.layout.fragment_audio_record,container,false);


    private static AudioRecord audioRecord=null;
    private int recordBufSize = 0;
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
    private final int time=4;

    //short转data
    private short getShort(byte[] data, int start)
    {
        return (short)((data[start] & 0xFF) | (data[start+1] << 8));
    }

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
    public AudioRecordUtils() {
        recordBufSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);  //audioRecord能接受的最小的buffer大小
        //缓冲区大小,16000是存储1s的数据需要的缓冲区字节大小：一秒8000样本，每个样本2byte大小，一秒16000byte字节大小
        recordBufSize = 16000*time;
        //16bit，一秒8000个样本，即16000个byte
        data = new byte[recordBufSize];
        dataNew = new byte[recordBufSize];
        tool = new PcmToWavUtils(AUDIO_SAMPLE_RATE,AUDIO_CHANNEL,AUDIO_ENCODING);
        try{
            if(audioRecord==null){
                System.out.println("生成audioRecord对象");
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
        MyThread myThread = new MyThread();
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
        //利用自定义工具类将pcm格式的文件转换为wav格式文件才能进行播放
        tool.pcmToWav(pcmFileName,wavFileName);
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

            ExecutorService exec = Executors.newSingleThreadScheduledExecutor();

            if (null != os) {
                String str ="";
                while (isRecording) {
                    // 从音频硬件读取音频数据，以便记录到字节数组中。
                    int read = audioRecord.read(data, 0, recordBufSize);
                    //调整音量大小
                    amplifyPCMData(data, data.length, dataNew,16,(float) Math.pow(10, (double)5 / 20));
                    short[] shortData = ArraysUtils.byteToShortInBigEnd(dataNew);
                    str = "{\"data\":\"" + Arrays.toString(shortData) + "\"}";
                    exec.submit(new PostUtils(str));
                    exec.submit(new Thread(){
                        @Override
                        public void run() {
                            if(PostUtils.code!=200) {
                                AudioRecords.show("网络通信失败！");
                            }
                            else if(PostUtils.code==200){
                                JSONArray content;
                                String talkContent;
                                try {
                                    //处理json对象
                                    JSONObject object=new JSONObject(PostUtils.msg);
                                    //判断是否具有content
                                    if(object.has("content")){
                                        content=object.getJSONArray("content");
                                        if(content.length()!=0){
                                            AudioRecords.show(PostUtils.msg);
                                        }
                                        else if(content.length()==0 && AudioRecords.serverReturn.getText().toString().equals("")){
                                            AudioRecords.show("收到空报文");
                                        }
                                    }
                                    //判断是否具有talkContent
                                    else if(object.has("talkContent")){
                                        talkContent=object.getString("talkContent");
                                        if(!talkContent.equals("")){
                                            AudioRecords.show(PostUtils.msg);
                                        }
                                        else if(talkContent.equals("") && (AudioRecords.serverReturn.getText().toString().equals(""))){
                                            AudioRecords.show("收到空报文");
                                        }
                                    }
                                    else{
                                        AudioRecords.show("收到空报文");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                            PostUtils.code=0;
                        }
                    });
                    // 如果读取音频数据没有出现错误，就将数据写入到文件
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        try {
                            os.write(data);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
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

