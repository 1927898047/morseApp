package com.zyj.morseapp.utils;

/**
 * short和 byte数组的互转
 */
public class ArraysUtils {
    /**
     * 大端序存储
     * @param arr
     * @return
     */
    public static short[] byteToShortInBigEnd(byte[] arr){
        int length=arr.length;
        short[] shortArr=new short[length/2];
        for (int i = 0; i < length/2; i++) {
            shortArr[i] = (short) ((arr[i * 2] & 0xff) | (( arr[i * 2 + 1] & 0xff) << 8));
        }
        return shortArr;
    }

    /**
     * 小端序存储
     * @param arr
     * @return
     */
    public static short[] byteToShortInLittleEnd(byte[] arr){
        int length=arr.length;
        short[] shortArr=new short[length/2];
        for (int i = 0; i < length/2; i++) {
            shortArr[i] = (short) ((arr[i * 2 + 1] & 0xff) | (( arr[i * 2 ] & 0xff) << 8));
        }
        return shortArr;
    }




}
