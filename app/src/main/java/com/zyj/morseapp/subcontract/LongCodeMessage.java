package com.zyj.morseapp.subcontract;

import com.zyj.morseapp.pages.HalfDuplex;
import com.zyj.morseapp.utils.MessageUtils;

import java.util.List;

public class LongCodeMessage {
    private static String head = "R R R DE";
    private static String tail = "K K K";
    private String senderId;
    private String receiverId;

    // 一包短码报文的最大长度
    private String gLen;

    // 短码报文的总长度
    private String gLenSum;

    // 短码报文的包数
    private String groupNum;

    // 短码报文的CRC
    private List<String> shortCrcList;

    // 长码报文的CRC
    private String longCrc;


    public LongCodeMessage(String senderId, String receiverId, String gLen, String gLenSum,
                           String groupNum, List<String> shortCrcList){
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.gLen = gLen;
        this.gLenSum = gLenSum;
        this.groupNum = groupNum;
        this.shortCrcList = shortCrcList;
    }


    // 获取长码报文
    public String getLongCodeMessage(){
        String longCodeMessage = head + " "
                + senderId + " "
                + "CALLING "
                + receiverId + " "
                + gLen + " "
                + gLenSum + " "
                + groupNum + " ";

        for (String str : shortCrcList){
            longCodeMessage = longCodeMessage + str + " ";
        }

        String temp = "DE "
                + senderId + " "
                + "CALLING "
                + receiverId + " "
                + gLen + " "
                + gLenSum + " "
                + groupNum + " ";
        for (String str : shortCrcList){
            temp = temp + str + " ";
        }

        longCrc = MessageUtils.getCRC16(temp.trim());
        longCodeMessage = longCodeMessage + longCrc + " " + tail;

        // 增加前导码
        longCodeMessage = MessageUtils.addPreamble(longCodeMessage, HalfDuplex.preambleNum);

        System.out.println("构建长码报文：" + longCodeMessage);
        return longCodeMessage;
    }
}
