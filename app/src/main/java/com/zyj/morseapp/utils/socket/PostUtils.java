package com.zyj.morseapp.utils.socket;

import com.zyj.morseapp.pages.Sockets;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Post请求，在子线程中进行
 */
public class PostUtils extends Thread{
    public static String msg="";
    private String str = "";
    public static int code;
    public PostUtils(String str){
        this.str=str;
    }

    @Override
    public void run() {
        try {
            // 1. 获取访问地址URL
            URL url = new URL("http://"+Sockets.ip+":"+Sockets.port+"/upload_json");
            System.out.println(url);
            // 2. 创建HttpURLConnection对象
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            /* 3. 设置请求参数等 */
            // 请求方式
            connection.setRequestMethod("POST");
            // 设置连接超时时间
            connection.setConnectTimeout(4000);// 设置是否向 HttpUrlConnection 输出，对于post请求，参数要放在 http 正文内，因此需要设为true，默认为false。
            connection.setDoOutput(true);
            // 设置是否从 HttpUrlConnection读入，默认为true
            connection.setDoInput(true);
            // 设置是否使用缓存
            connection.setUseCaches(false);
            // 设置此 HttpURLConnection 实例是否应该自动执行 HTTP 重定向
            connection.setInstanceFollowRedirects(true);
            // 设置使用标准编码格式编码参数的名-值对
            connection.setRequestProperty("Content-Type", "application/json");
            // JDK8中，HttpURLConnection默认开启Keep-Alive
            // 连接
            connection.connect();
            /* 4. 处理输入输出 */
            // 写入参数到请求中
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
            writer.write(str);
            writer.flush();
            writer.close();
            // 从连接中读取响应信息
            code=0;
            code = connection.getResponseCode();
            msg="";
            if (code == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    msg += line + "\n";
                }
                reader.close();
            }
            // 5. 断开连接
            connection.disconnect();
            // 处理结果
            System.out.println("响应码："+code);
            System.out.println("响应内容：");
            System.out.println(msg);
        }
        catch (IOException e ){
            e.printStackTrace();
        }


    }

};