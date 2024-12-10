package com.zyj.morseapp.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {
    private static final int POLYNOMIAL = 0x1021; // CRC-16-CCITT多项式
    private static final int[] table = new int[256];
    // 每包的短码报文长度
    private static int gLen = 3;

    public static void setGLen(int num){
        gLen = num;
    }

    public static int getGLen(){
        return gLen;
    }

    // 初始化CRC16查找表
    static {
        for (int i = 0; i < table.length; i++) {
            int value = i;
            for (int j = 0; j < 8; j++) {
                if ((value & 0x0001) != 0) {
                    value = (value >> 1) ^ POLYNOMIAL;
                } else {
                    value >>= 1;
                }
            }
            table[i] = value;
        }
    }

    // 计算CRC16校验和
    public static int computeChecksum(byte[] bytes) {
        int crc = 0xFFFF;
        for (byte b : bytes) {
            crc = (crc >> 8) ^ table[(crc ^ b) & 0xFF];
        }
        crc &= 0xFFFF;
        return crc;
    }

    // 辅助方法，将字节数组转换为十六进制字符串
    public static String toHexString(int crc) {
        return String.format("%04X", crc);
    }

    public static String getCRC16(String input){
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        int crcValue = computeChecksum(bytes);
        return toHexString(crcValue);
    }

    /**
     * 增加前导码
     * @param str
     * @return
     */
    public static String addPreamble(String str, int n){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < n; i++){
            sb.append("7777 ");
        }
        sb.append(str);
        return sb.toString();
    }


    /**
     * 返回分组后的短码报文
     * @param shortInput
     * @return
     */
    public static List<String> createShortCodeGroup(String shortInput){
        String[] parts = shortInput.split(" ");
        List<String> resultList = new ArrayList<>();
        for (String part : parts) {
            // 去除每个元素前后的空格后再添加到结果列表中
            resultList.add(part.trim());

        }

        List<String> res = new ArrayList<>();
        int temp = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < resultList.size(); i++){
            sb.append(resultList.get(i) + " ");
            temp++;
            if (temp == gLen){
                sb.delete(sb.length()-1, sb.length());
                res.add(sb.toString());
                sb.delete(0, sb.length());
                temp = 0;
            }
        }

        if (temp != 0){
            sb.delete(sb.length()-1, sb.length());

            res.add(sb.toString());
        }
        return res;
    }

    /**
     * 返回去除空格后的数组
     * @param shortInput
     * @return
     */
    public static List<String> getNoTrimCode(String shortInput){
        String[] parts = shortInput.split(" ");
        List<String> resultList = new ArrayList<>();
        for (String part : parts) {
            // 去除每个元素前后的空格后再添加到结果列表中
            resultList.add(part.trim());
        }


        return resultList;
    }

    /**
     * 返回去除空格后的数组
     * @param input
     * @return
     */
    public static List<String> getIDListFromLongCodeRec2(String input){
        List<String> resultList = new ArrayList<>();
        int startIndex = input.indexOf("DE");
        int endIndex = input.indexOf("READY");

        try {
            List<String> noTrimCode = getNoTrimCode(input.substring(startIndex, endIndex));
            for (int i = 2; i < noTrimCode.size() - 1; i++){
                resultList.add(noTrimCode.get(i));
            }
        } catch (Exception e){
            return new ArrayList<>();
        }

        return resultList;
    }

    /**
     * 获取短码的正文内容
     * @param input
     * @return
     */
    public static String getShortCodeText(String input){
        int startIndex = input.indexOf("===") + "===".length();
        int endIndex = input.indexOf("+++");
        if (startIndex >= 0 && endIndex > startIndex) {
            String result = input.substring(startIndex, endIndex).trim();
            return result;
        }

        return null;
    }

}