package com.zyj.morseapp.utils;

import java.nio.charset.StandardCharsets;

public class CrcUtils {
    private static final int POLYNOMIAL = 0x1021; // CRC-16-CCITT多项式
    private static final int[] table = new int[256];

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
}