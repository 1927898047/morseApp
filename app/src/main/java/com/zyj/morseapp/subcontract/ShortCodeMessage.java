package com.zyj.morseapp.subcontract;

import com.zyj.morseapp.pages.HalfDuplex;
import com.zyj.morseapp.utils.MessageUtils;

public class ShortCodeMessage {
    private static String head = "===";
    private static String tail = "+++";

    // 短码报文包ID
    private String gid;

    // 短码报文的长度
    private String gLen;

    // 短码报文内容
    private String shortCodeContent;

    // 本次通信ID
    private int communicationId;

    public ShortCodeMessage(String gid, String gLen, String shortCodeContent, int communicationId){
        this.gid = gid;
        this.gLen = gLen;
        this.shortCodeContent = shortCodeContent;
        this.communicationId = communicationId;
    }

    // 获取短码正文
    public String getShortCodeText(){
        String shortCodeText =
                communicationId + " "
                + gid + " "
                + gLen + " "
                + shortCodeContent;

        return shortCodeText;
    }

    // 获取短码报文
    public String getShortCodeMessage(){
        String shortCodeMessage = head + " "
                + communicationId + " "
                + gid + " "
                + gLen + " "
                + shortCodeContent + " "
                + tail;

        shortCodeMessage = MessageUtils.addPreamble(shortCodeMessage, HalfDuplex.preambleNum);

        System.out.println("构建短码报文：" + shortCodeMessage);
        return shortCodeMessage;
    }
}
