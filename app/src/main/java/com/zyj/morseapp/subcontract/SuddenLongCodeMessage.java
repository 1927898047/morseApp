package com.zyj.morseapp.subcontract;

import com.zyj.morseapp.pages.HalfDuplex;
import com.zyj.morseapp.utils.MessageUtils;

import java.util.List;

public class SuddenLongCodeMessage {
    private static String head = "RRR DE";
    private static String tail = "KKK";
    private String senderId;
    private String receiverId;

    // 一包短码报文的最大长度
    private String gLen;

    // 短码报文的总长度
    private String gLenSum;

    // 长码报文的CRC
    private String longCrc;

    // 通信ID
    private int communicationId;

    // 短码报文
    private String shortCodeContent;

    // 附注信息
    private String other;

    public SuddenLongCodeMessage(String senderId, String receiverId, String gLen, String gLenSum,
                                 int communicationId, String shortCodeContent, String other){
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.gLen = gLen;
        this.gLenSum = gLenSum;
        this.communicationId = communicationId;
        this.shortCodeContent = shortCodeContent;
        this.other = other;
    }


    // 获取长码报文
    public String getLongCodeMessage(){
        String longCodeMessage = head + " "
                + senderId + " "
                + "TS "
                + receiverId + " "
                + gLen + " "
                + gLenSum + " "
                + communicationId + " "
                + shortCodeContent + " "
                + other + " ";

        String temp = "DE" + " "
                + senderId + " "
                + "TS "
                + receiverId + " "
                + gLen + " "
                + gLenSum + " "
                + communicationId + " "
                + shortCodeContent + " "
                + other + " ";

        longCrc = MessageUtils.getCRC16(temp.trim());
        longCodeMessage = longCodeMessage + longCrc + " " + tail;

        // 增加前导码
        longCodeMessage = MessageUtils.addPreamble(longCodeMessage, HalfDuplex.preambleNum);

        System.out.println("构建长码报文：" + longCodeMessage);
        return longCodeMessage;
    }
}
