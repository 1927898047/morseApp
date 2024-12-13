package com.zyj.morseapp.audio;


import com.zyj.morseapp.pages.HalfDuplex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

/**
 * 摩尔斯音频的生成类
 */
public class MorseAudio {
    //点：1t
    private Integer ditRatio = 1;

    //划：3t
    private Integer dahRatio = 3;

    //大间隔
    private Integer intervalRatio = 3;

    //相应的字符：'.'、'-'、'/'
    private final String ditString = ".";

    private final String dahString = "-";

    private final String intervalString = "/";

    private boolean changeSnr = false;

    public void setChangeSnr(boolean changeSnr) {
        this.changeSnr = changeSnr;
    }

    /**
     * 将摩尔斯电码转成音频
     * @param codeString 摩尔斯电码
     * @return 音频流
     */
    public short[] codeConvert2Sound(String codeString,int wpm){
        System.out.println("wpm:"+wpm);
        int dit = ditRatio;
        int dah = dahRatio;
        int interval = intervalRatio;
        //存放byte
        ArrayList<Short> pcmAudio = new ArrayList<>();
        // 时长
        int soundLength = 0;
        // 频率:1k Hz 或 0 Hz
        long frequency = 0;
        // 采样率
        float SampleRate=8000;
        // 开始加一段空的音频
        System.out.println("开始加一段空的音频:"+pcmAudio.size());
        soundLength = 10;
        frequency = 0;
        addPcmWave(SampleRate,wpm,soundLength,frequency,pcmAudio);
        System.out.println("开始加一段空的音频:"+pcmAudio.size());
        //计算每一个字符的音波
        for(int i=0; i<codeString.length(); i++){
            String code = Character.toString(codeString.charAt(i));
            switch (code){
                case dahString:
                    frequency = 1000;
                    soundLength = dah;
                    addPcmWave(SampleRate,wpm,soundLength,frequency,pcmAudio);
                    //每个字符后面
                    frequency = 0;
                    soundLength = dit;
                    addPcmWave(SampleRate,wpm,soundLength,frequency,pcmAudio);
                    break;
                case ditString:
                    frequency = 1000;
                    soundLength = dit;
                    addPcmWave(SampleRate,wpm,soundLength,frequency,pcmAudio);
                    frequency = 0;
                    soundLength = dit;
                    addPcmWave(SampleRate,wpm,soundLength,frequency,pcmAudio);
                    break;
                case intervalString:
                    frequency = 0;
                    soundLength = 2;
                    addPcmWave(SampleRate,wpm,soundLength,frequency,pcmAudio);
                    break;
                default:
                    break;
            }
        }

        // 结尾加一段空的音频
        soundLength = 10;
        frequency = 0;
        addPcmWave(SampleRate,wpm,soundLength,frequency,pcmAudio);

        System.out.println("结尾加一段空的音频:"+pcmAudio.size());

        //将list转array
        short[] audio = new short[pcmAudio.size()];
        for (int i=0; i<pcmAudio.size(); i++){
            audio[i] = pcmAudio.get(i);
        }

        pcmAudio.clear();

        return audio;
    }


    public void addPcmWave(float SampleRate,int wpm,int soundLength,long frequency,ArrayList<Short> pcmAudio){
        // 生成PCM波
        soundLength = 1200/wpm * soundLength;
        for ( int k = 0; k < soundLength * SampleRate / 1000; k++ ) {
            double angle = k / ( SampleRate / frequency ) * 2.0 * Math.PI;

            Random random = new Random();
            double randomNum = random.nextDouble();
            // 判断是否需要调整信噪比
            if (changeSnr) {
                pcmAudio.add( (short)( Math.sin( angle ) *8000 + Math.sin(randomNum) * HalfDuplex.gussianNoise));
            } else {
                pcmAudio.add( (short)( Math.sin( angle ) *8000 + Math.sin(randomNum) *4000));
            }
        }


//        // 生成PCM波
//        for ( int k = 0; k < soundLength * SampleRate / frequency; k++ ) {
//            double angle = k / ( SampleRate / frequency ) * 2.0 * Math.PI;
//
//            Random random = new Random();
//            double randomNum = random.nextDouble();
//            // 判断是否需要调整信噪比
//            if (changeSnr) {
//                pcmAudio.add( (short)( Math.sin( angle ) *8000 + Math.sin(randomNum) * HalfDuplex.gussianNoise));
//            } else {
//                pcmAudio.add( (short)( Math.sin( angle ) *8000 + Math.sin(randomNum) *4000));
//            }
//        }
    }





    /**
     2      * @param totalAudioLen  不包括header的音频数据总长度
     3      * @param longSampleRate 采样率,也就是录制时使用的频率、音频采样级别 8000 = 8KHz
     4      * @param channels       audioRecord的声道数1/2
     5      * @param audioFormat    采样精度; 譬如 16bit
     6      * @throws IOException 写文件错误
     7      */
    public static byte[] writeWavFileHeader(long totalAudioLen, long longSampleRate,
                                            int channels, int audioFormat) throws IOException {
        byte[] header = generateWavFileHeader(totalAudioLen, longSampleRate, channels, audioFormat);
        return header;
    }

    /**
     8      * @param totalAudioLen  不包括header的音频数据总长度
     9      * @param longSampleRate 采样率,也就是录制时使用的频率
     10      * @param channels       audioRecord的频道数量
     11      * @param audioFormat    采样精度; 譬如 16bit
     12      */
    private static byte[] generateWavFileHeader(long totalAudioLen, long longSampleRate, int channels,int audioFormat) {

        long totalDataLen = totalAudioLen + 36;

        long byteRate = longSampleRate  * channels *2;

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF

        header[1] = 'I';

        header[2] = 'F';

        header[3] = 'F';

        //文件长度  4字节文件长度，这个长度不包括"RIFF"标志(4字节)和文件长度本身所占字节(4字节),即该长度等于整个文件长度 - 8

        header[4] = (byte) (totalDataLen & 0xff);

        header[5] = (byte) ((totalDataLen >> 8) & 0xff);

        header[6] = (byte) ((totalDataLen >> 16) & 0xff);

        header[7] = (byte) ((totalDataLen >> 24) & 0xff);

        //fcc type：4字节 "WAVE" 类型块标识, 大写

        header[8] = 'W';

        header[9] = 'A';

        header[10] = 'V';

        header[11] = 'E';

        //FMT Chunk   4字节 表示"fmt" chunk的开始,此块中包括文件内部格式信息，小写, 最后一个字符是空格

        header[12] = 'f'; // 'fmt '

        header[13] = 'm';

        header[14] = 't';

        header[15] = ' ';//过渡字节

        //数据大小  4字节，文件内部格式信息数据的大小，过滤字节（一般为00000010H）

        header[16] = 16;

        header[17] = 0;

        header[18] = 0;

        header[19] = 0;

        //编码方式 10H为PCM编码格式   FormatTag：2字节，音频数据的编码方式，1：表示是PCM 编码

        header[20] = 1; // format = 1

        header[21] = 0;

        //通道数  Channels：2字节，声道数，单声道为1，双声道为2

        header[22] = (byte) channels;

        header[23] = 0;

        //采样率，每个通道的播放速度

        header[24] = (byte) (longSampleRate & 0xff);

        header[25] = (byte) ((longSampleRate >> 8) & 0xff);

        header[26] = (byte) ((longSampleRate >> 16) & 0xff);

        header[27] = (byte) ((longSampleRate >> 24) & 0xff);

        //音频数据传送速率,采样率*通道数*采样深度/8

        //4字节，音频数据传送速率, 单位是字节。其值为采样率×每次采样大小。播放软件利用此值可以估计缓冲区的大小

        //byteRate = sampleRate * (bitsPerSample / 8) * channels

        header[28] = (byte) (byteRate & 0xff);

        header[29] = (byte) ((byteRate >> 8) & 0xff);

        header[30] = (byte) ((byteRate >> 16) & 0xff);

        header[31] = (byte) ((byteRate >> 24) & 0xff);

        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数

        header[32] = (byte) ( 2*channels);

        header[33] = 0;

        //每个样本的数据位数

        //2字节，每个声道的采样精度; 譬如 16bit 在这里的值就是16。如果有多个声道，则每个声道的采样精度大小都一样的；

        header[34] = (byte) audioFormat;

        header[35] = 0;

        //Data chunk

        //ckid：4字节，数据标志符（data），表示 "data" chunk的开始。此块中包含音频数据，小写；

        header[36] = 'd';

        header[37] = 'a';

        header[38] = 't';

        header[39] = 'a';

        //音频数据的长度，4字节，audioDataLen = totalDataLen - 36 = fileLenIncludeHeader - 44

        header[40] = (byte) (totalAudioLen & 0xff);

        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);

        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);

        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        return header;

    }


    public static byte[] pcmToWav(byte[] pcmData) throws IOException {
        byte[] header = MorseAudio.writeWavFileHeader(pcmData.length, 8000, 1, 16);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(header);
        byteArrayOutputStream.write(pcmData);
        return byteArrayOutputStream.toByteArray();
    }

}
