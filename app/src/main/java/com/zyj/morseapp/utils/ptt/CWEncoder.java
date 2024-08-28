package com.zyj.morseapp.utils.ptt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class CWEncoder {
    private static final String PTT_NODE_PATH = "/sys/class/attr-gpio/mptt/value";
    /**
     * 编码存储数组
     */
    private CharEncoder[] encoders = null;

    /**
     * 初始化长码表
     */
    private void initStandEncoder()
    {
        encoders = new CharEncoder[] {
                new CharEncoder(' ', new byte[]{0,0,0,0}),
                new CharEncoder('A', new byte[]{1,0,1,1,1,0,0,0}),
                new CharEncoder('B', new byte[]{1,1,1,0,1,0,1,0,1,0,0,0}),
                new CharEncoder('C', new byte[]{1,1,1,0,1,0,1,1,1,0,1,0,0,0}),
                new CharEncoder('D', new byte[]{1,1,1,0,1,0,1,0,0,0}),
                new CharEncoder('E', new byte[]{1,0,0,0}),
                new CharEncoder('F', new byte[]{1,0,1,0,1,1,1,0,1,0,0,0}),
                new CharEncoder('G', new byte[]{1,1,1,0,1,1,1,0,1,0,0,0}),
                new CharEncoder('H', new byte[]{1,0,1,0,1,0,1,0,0,0}),
                new CharEncoder('I', new byte[]{1,0,1,0,0,0}),
                new CharEncoder('J', new byte[]{1,0,1,1,1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('K', new byte[]{1,1,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('L', new byte[]{1,0,1,1,1,0,1,0,1,0,0,0}),
                new CharEncoder('M', new byte[]{1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('N', new byte[]{1,1,1,0,1,0,0,0}),
                new CharEncoder('O', new byte[]{1,1,1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('P', new byte[]{1,0,1,1,1,0,1,1,1,0,1,0,0,0}),
                new CharEncoder('Q', new byte[]{1,1,1,0,1,1,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('R', new byte[]{1,0,1,1,1,0,1,0,0,0}),
                new CharEncoder('S', new byte[]{1,0,1,0,1,0,0,0}),
                new CharEncoder('T', new byte[]{1,1,1,0,0,0}),
                new CharEncoder('U', new byte[]{1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('V', new byte[]{1,0,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('W', new byte[]{1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('X', new byte[]{1,1,1,0,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('Y', new byte[]{1,1,1,0,1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('Z', new byte[]{1,1,1,0,1,1,1,0,1,0,1,0,0,0}),

                new CharEncoder('0', new byte[]{1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('1', new byte[]{1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('2', new byte[]{1,0,1,0,1,1,1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('3', new byte[]{1,0,1,0,1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('4', new byte[]{1,0,1,0,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('5', new byte[]{1,0,1,0,1,0,1,0,1,0,0,0}),
                new CharEncoder('6', new byte[]{1,1,1,0,1,0,1,0,1,0,1,0,0,0}),
                new CharEncoder('7', new byte[]{1,1,1,0,1,1,1,0,1,0,1,0,1,0,0,0}),
                new CharEncoder('8', new byte[]{1,1,1,0,1,1,1,0,1,1,1,0,1,0,1,0,0,0}),
                new CharEncoder('9', new byte[]{1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,0,0,0}),

                new CharEncoder('$', new byte[]{1,0,1,0,1,1,1,0,1,0,1,0,0,0}),
                new CharEncoder('[', new byte[]{1,1,1,0,1,0,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('/', new byte[]{1,1,1,0,1,0,1,0,1,1,1,0,1,0,0,0}),
                new CharEncoder('(', new byte[]{1,1,1,0,1,0,1,1,1,0,1,1,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('?', new byte[]{1,0,1,0,1,1,1,0,1,1,1,0,1,0,1,0,0,0}),
                new CharEncoder('!', new byte[]{1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,0,0}),
                new CharEncoder('.', new byte[]{1,0,1,1,1,0,1,0,1,1,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder(']', new byte[]{1,0,1,1,1,0,1,0,1,1,1,0,1,0,0,0}),
                new CharEncoder('=', new byte[]{1,0,1,1,1,0,1,0,1,0,1,0,0,0}),
                new CharEncoder('#', new byte[]{1,0,1,0,1,0,1,1,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('@', new byte[]{1,1,1,0,1,1,1,0,1,0,1,0,1,0,1,1,1,0,1,1,1,0,1,0,0,0}),
                new CharEncoder('*', new byte[]{1,1,1,0,1,0,1,0,1,1,1,0,1,1,1,0,1,0,1,0,1,1,1,0,1,1,1,0,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('&', new byte[]{1,1,1,0,1,0,1,1,1,0,1,0,1,1,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('+', new byte[]{1,1,1,0,1,1,1,0,1,0,1,1,1,0,1,0,1,0,1,0,1,0,1,1,1,0,1,0,1,0,0,0}),
                new CharEncoder('%', new byte[]{1,0,1,1,1,0,1,0,1,0,1,1,1,0,0,0})
        };
    }

    /**
     * 初始化短码表
     */
    private void initShortEncoder()
    {
        encoders = new CharEncoder[] {
                new CharEncoder(' ', new byte[]{0,0,0,0}),
                new CharEncoder('0', new byte[]{1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('1', new byte[]{1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('2', new byte[]{1,0,1,0,1,1,1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('3', new byte[]{1,0,1,0,1,0,1,1,1,0,1,1,1,0,0,0}),
                new CharEncoder('4', new byte[]{1,0,1,0,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('5', new byte[]{1,0,1,0,1,0,1,0,1,0,0,0}),
                new CharEncoder('6', new byte[]{1,1,1,0,1,0,1,0,1,0,1,0,0,0}),
                new CharEncoder('7', new byte[]{1,1,1,0,1,1,1,0,1,0,1,0,1,0,0,0}),
                new CharEncoder('8', new byte[]{1,1,1,0,1,1,1,0,1,1,1,0,1,0,1,0,0,0}),
                new CharEncoder('9', new byte[]{1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,0,1,0,0,0}),

                new CharEncoder('T', new byte[]{1,1,1,0,0,0}),
                new CharEncoder('A', new byte[]{1,0,1,1,1,0,0,0}),
                new CharEncoder('U', new byte[]{1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('D', new byte[]{1,1,1,0,1,0,1,0,0,0}),
                new CharEncoder('N', new byte[]{1,1,1,0,1,0,0,0}),

                new CharEncoder('[', new byte[]{1,1,1,0,1,0,1,0,1,0,1,1,1,0,0,0}),
                new CharEncoder('/', new byte[]{1,1,1,0,1,0,1,0,1,1,1,0,1,0,0,0}),
                new CharEncoder('?', new byte[]{1,0,1,0,1,1,1,0,1,1,1,0,1,0,1,0,0,0}),
                new CharEncoder(']', new byte[]{1,0,1,1,1,0,1,0,1,1,1,0,1,0,0,0}),
                new CharEncoder('@', new byte[]{1,1,1,0,1,1,1,0,1,0,1,0,1,0,1,1,1,0,1,1,1,0,1,0,0,0}),
                new CharEncoder('&', new byte[]{1,1,1,0,1,0,1,1,1,0,1,0,1,1,1,0,1,0,1,1,1,0,0,0})
        };
    }

    /**
     * 构造函数
     * @param bShort
     */
    private CWEncoder(boolean bShort)
    {
        if (bShort)
            initShortEncoder();
        else
            initStandEncoder();
    }

    /**
     * 长码表
     */
    private static final CWEncoder standEncoder = new CWEncoder(false);

    /**
     * 短码表
     */
    private static final CWEncoder shortEncoder = new CWEncoder(true);

    /**
     * 获取长码表
     * @return 返回长码编码表对象
     */
    public static CWEncoder getStandEncoder()
    {
        return standEncoder;
    }

    /**
     * 获取短码表
     * @return 返回短码编码表对象
     */
    public static CWEncoder getShortEncoder()
    {
        return shortEncoder;
    }

    /**
     * 对字符进行编码
     * @param message 待编码的字符
     * @return 返回0，1表示的字节流
     */
    public byte[] encode(char message)
    {
        for (CharEncoder encoder: encoders) {
            if (encoder.getChr() == message)
            {
                return Arrays.copyOf(encoder.getValues(), encoder.getValues().length);
            }
        }
        return null;
    }

    /**
     * 对字符数组进行编码
     * @param message 字符数组
     * @return 返回0，1表示的字节流
     */
    public void encode(final char[] message)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] result = null;
                for (char ch : message)
                {
                    result = encode(ch);
                    if (result == null)
                        continue;
                    for (int i = 0;i<result.length;i++)
                    {
                        //PTT控制接收
                        if (result[i] == 1)
                        {
                            //PTT控制发送
                            setPower(true,PTT_NODE_PATH);
                            MyAudio.getInstance().playVoice(true);
                        }
                        else
                        {
                            setPower(false,PTT_NODE_PATH);
                            MyAudio.getInstance().playVoice(false);
                        }
                    }
                }
                setPower(false,PTT_NODE_PATH);
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

    /**
     * 对字符串进行编码
     * @param message 待编码的字符串
     * @return 返回0，1表示的字节流
     */
    public void encode(String message)
    {
        char[] chars = message.toCharArray();
        encode(chars);
    }
}
