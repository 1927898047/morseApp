package com.zyj.morseapp.subcontract;

import com.zyj.morseapp.pages.HalfDuplex;
import com.zyj.morseapp.utils.MessageUtils;

import java.util.List;

public class LongCodeMessage {
    private static String head = "RRR DE";
    private static String tail = "KKK";
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

    // 短码报文码速
    private int shortCodeWpm;

    // 通信ID
    private int communicationId;

    // 附注
    private String other;

    public LongCodeMessage(String senderId, String receiverId, String gLen, String gLenSum,
                           String groupNum, List<String> shortCrcList, int shortCodeWpm, int communicationId,
                           String other){
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.gLen = gLen;
        this.gLenSum = gLenSum;
        this.groupNum = groupNum;
        this.shortCrcList = shortCrcList;
        this.shortCodeWpm = shortCodeWpm;
        this.communicationId = communicationId;
        this.other = other;
    }


    // 获取长码报文
    public String getLongCodeMessage(){
        String longCodeMessage = head + " "
                + senderId + " "
                + "CQ "
                + receiverId + " "
                + gLen + " "
                + gLenSum + " "
                + groupNum + " "
                + shortCodeWpm + " "
                + communicationId + " "
                + other + " ";

        for (String str : shortCrcList){
            longCodeMessage = longCodeMessage + str + " ";
        }

        String temp = "DE "
                + senderId + " "
                + "CQ "
                + receiverId + " "
                + gLen + " "
                + gLenSum + " "
                + groupNum + " "
                + shortCodeWpm + " "
                + communicationId + " "
                + other + " ";
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
