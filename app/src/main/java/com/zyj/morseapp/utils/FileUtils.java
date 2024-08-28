package com.zyj.morseapp.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 读取文件到byte数组
 */
public class FileUtils {
    public static byte[] fileToByteArr(String path){
        try {
            File f = new File(path);
            int length = (int) f.length();
            byte[] data = new byte[length];
            new FileInputStream(f).read(data);
            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }



    public static void writeTxt(String path,String str) throws IOException {
        File file = new File(path);
        if(!file.exists()){
            file.createNewFile();
        }
        BufferedWriter out = new BufferedWriter(new FileWriter(file));
        out.write(str);
        out.flush();
        out.close();
    }

    public static String readTxt(String path) throws IOException {
        File file = new File(path);
        InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file));
        BufferedReader br = new BufferedReader(reader);
        String line="";
        line=br.readLine();
        return line;
    }

    public static boolean fileExist(String path){
        File file=new File(path);
        return file.exists();
    }
}
