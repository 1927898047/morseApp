package com.zyj.morseapp.utils.ptt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.os.Process;

import androidx.core.app.ActivityCompat;

import com.zyj.morseapp.R;
import com.zyj.morseapp.application.MyApplication;
import com.zyj.morseapp.utils.ArraysUtils;
import com.zyj.morseapp.utils.PostUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MyAudio
{
	private static final String PTT_NODE_PATH = "/sys/class/attr-gpio/mptt/value";
	private AudioRecord mAudioRecord = null;
	private AudioTrack mAudioPlayer = null;
	private static Context context = MyApplication.getContext();

	private final static int playMode = AudioTrack.MODE_STREAM;
	private final static int streamType = AudioManager.STREAM_MUSIC;
	private static final int FRAME_LENGTH = 16000*4;  //16bit，一秒8000个样本，即16000个byte
	private int playBufferSizeInBytes=0;
	private boolean isPlaying=false;
	private boolean isRecord = false;//设置录制的状态
	private boolean m_bIsExit = false;//是否退出
	private boolean isCalling = false;

	private int audioSource = MediaRecorder.AudioSource.MIC;
	private int sampleRateInHz = 8000;
	private int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;//单声道
	private int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	private int bufferSizeInBytes = 0;
	private Handler myHandler = null;
	static MyAudio instance=null;
	private Context mContext = null;
	private int mCPM = 200;  //发报码速200码,假设时间片对应24ms
	public static final String ACTION_RECV_MSG = "com.szea.morse.recvmsg";
	private boolean m_bIsSending = false;

	private MyAudio()
	{
		initAudioRecord();
		mAudioPlayer.play();
		new RecordAudioThread().start();
	}

	public static MyAudio getInstance()
	{
		if(null==instance)
		{
			instance = new MyAudio();
		}
		return instance;
	}

	public void setContext(Context mcontext)
	{
		mContext = mcontext;
	}

	public void setCPM(int iCPM)
	{
		mCPM = iCPM;
	}

	public void SetHandeler(Handler mHandler)
	{
		myHandler = mHandler;
	}

	private void initAudioRecord()
	{
		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
		bufferSizeInBytes = FRAME_LENGTH;
		//System.out.println("bufferSizeInBytes="+bufferSizeInBytes);
		// 初始化AudioRecord并开始录音
		if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
			mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
		}

		playBufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
		System.out.println("playBufferSizeInBytes="+playBufferSizeInBytes);
		//语音为扬声器模式
		mAudioPlayer = new AudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat, playBufferSizeInBytes, playMode);

	}
	
	public void StartRecord()
	{		
		mAudioRecord.startRecording();
		isRecord = true;
	}
	
	public void StopRecord()
	{
		if(mAudioRecord!=null)
		{
			isRecord = false;
			mAudioRecord.stop();
			//mAudioRecord.release();
		}
	}

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

	class RecordAudioThread extends Thread {
		@Override
		public void run() {
			this.setPriority(Thread.MAX_PRIORITY); //设置本线程优先级。
			Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO); //设置本线程优先级。
			byte[] pcmData = new byte[FRAME_LENGTH];
			byte[] amplifypcmData = new byte[FRAME_LENGTH];
			int readSize = 0;
			ExecutorService exec = Executors.newSingleThreadScheduledExecutor();
			while (true) {
				if (isRecord) {

					readSize = mAudioRecord.read(pcmData, 0, FRAME_LENGTH);
					if (readSize > 0) {
						//调整音量大小
						amplifyPCMData(pcmData,pcmData.length,amplifypcmData,16,(float) Math.pow(10, (double)5 / 20));
						if (m_bIsSending)
							continue;
						short[] shortData = ArraysUtils.byteToShortInBigEnd(amplifypcmData);
						String str = "{\"data\":\"" + Arrays.toString(shortData) + "\"}";
						exec.submit(new PostUtils(str));
						exec.submit(new Thread(){
							@Override
							public void run() {
								if(PostUtils.code!=200) {
									System.out.println("网络通信失败！");
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
												//System.out.println("msg="+PostUtils.msg);
												Intent mIntent = new Intent();
												mIntent.setAction(ACTION_RECV_MSG);
												StringBuffer buf = new StringBuffer();
												for (int i = 0;i<content.length();i++)
												{
													buf.append(content.getString(i));
													buf.append(" ");
												}
												mIntent.putExtra("content",buf.toString());
												mIntent.putExtra("groupcount",content.length());
												mContext.sendBroadcast(mIntent);
											}
//											else if(content.length()==0 && AudioRecords.serverReturn.getText().toString().equals("")){
//												AudioRecords.show("收到空报文");
//											}
										}
										//判断是否具有talkContent
										else if(object.has("talkContent")){
											talkContent=object.getString("talkContent");
											if(!talkContent.equals("")){
												System.out.println("msg="+PostUtils.msg);
											}
//											else if(talkContent.equals("") && (AudioRecords.serverReturn.getText().toString().equals(""))){
//												AudioRecords.show("收到空报文");
//											}
										}
										else{
											System.out.println("收到空报文");
										}
									} catch (JSONException e) {
										e.printStackTrace();
									}
								}
								PostUtils.code=0;
							}
						});

					}
				}
				if (m_bIsExit) {
					break;
				}
			}
		}
	}

	public  byte[] shortArray2ByteArray(short[] src, byte[] retVal) 
	{
        for (int i = 0; i < src.length; i++)
        {
            retVal[i*2] = (byte)(src[i]&0xff);
            retVal[i*2+1] = (byte)((src[i]>>8)&0xff);            
        }
        return retVal;
    }

    public void playVoice(boolean bValid)
	{
		int iLength = 4800*16/mCPM;
		byte []szBuffer = new byte[4096];
		InputStream is = mContext.getResources().openRawResource(R.raw.audio);
		try {
			is.read(szBuffer,0,szBuffer.length);
			is.read(szBuffer,0,iLength);
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!bValid)
		{
			szBuffer = new byte[iLength];
		}
		int iResult = mAudioPlayer.write(szBuffer,0,iLength);
		System.out.println("iResult="+iResult);
		//mAudioPlayer.stop();
	}

	public void playMorse(String morse, final int wpm)
	{
		final String strMorse = morse;
		final int iwpm = wpm;
		m_bIsSending = true;
		new Thread(new Runnable() {
			@Override
			public void run() {
				// 激活码码速
				int activeWpm = 20;
				int imSleep = 1200/activeWpm;
				String activeCode = "-/////";
				for (int i = 0; i < activeCode.codePointCount(0, activeCode.length()); i++)
				{
					String strTemp = activeCode.substring(i,i+1);
					System.out.println("strTemp="+strTemp);
					try {
						if (strTemp.compareTo(".") == 0)
						{
							setPower(true,PTT_NODE_PATH);
							Thread.sleep(imSleep);
							setPower(false,PTT_NODE_PATH);
							Thread.sleep(imSleep);
							System.out.println("ptt Mode is playing:" + strTemp);

						}
						else if(strTemp.compareTo("-") == 0)
						{
							setPower(true,PTT_NODE_PATH);
							Thread.sleep(3*imSleep);
							setPower(false,PTT_NODE_PATH);
							Thread.sleep(imSleep);
							System.out.println("ptt Mode is playing:" + strTemp);

						}
						else if(strTemp.compareTo("/") == 0)
						{
							setPower(false,PTT_NODE_PATH);
							Thread.sleep(2*imSleep);
							System.out.println("ptt Mode is playing:" + strTemp);

						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				// 正式报文
				imSleep = 1200/iwpm;
				for (int i = 0; i < strMorse.codePointCount(0, strMorse.length()); i++)
				{
					String strTemp = strMorse.substring(i,i+1);
					System.out.println("strTemp="+strTemp);
					try {
						if (strTemp.compareTo(".") == 0)
						{
							setPower(true,PTT_NODE_PATH);
							Thread.sleep(imSleep);
							setPower(false,PTT_NODE_PATH);
							Thread.sleep(imSleep);
							System.out.println("ptt Mode is playing:" + strTemp);

						}
						else if(strTemp.compareTo("-") == 0)
						{
							setPower(true,PTT_NODE_PATH);
							Thread.sleep(3*imSleep);
							setPower(false,PTT_NODE_PATH);
							Thread.sleep(imSleep);
							System.out.println("ptt Mode is playing:" + strTemp);

						}
						else if(strTemp.compareTo("/") == 0)
						{
							setPower(false,PTT_NODE_PATH);
							Thread.sleep(2*imSleep);
							System.out.println("ptt Mode is playing:" + strTemp);

						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				m_bIsSending = false;
			}
		}).start();

	}

	private void setPower(boolean isOn, String power_path)
	{
		BufferedWriter mWriter = null;
		String command;

		/* Write the command to the device. */
		if (isOn)
			command = "1";
		else
			command = "0";

		/* Open the device. */
		try
		{
			mWriter = new BufferedWriter(new FileWriter(power_path));
			mWriter.write(command, 0, command.length());
			mWriter.flush();
			mWriter.close();
		} catch (IOException e)
		{
			/* add your code here: error */
			e.printStackTrace();
			return;
		}
	}



	
}