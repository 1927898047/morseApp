package com.zyj.morseapp.utils;

public class StringUtils {

    public static String getId1FromLongCode(String inputString) {
        String[] parts = inputString.split("DE");
        if (parts.length > 1) {
            String[] subParts = parts[1].split("CALLING");
            if (subParts.length > 0) {
                String value = subParts[0];
                value = value.trim();
                return value;
            }
        }
        return "-1";
    }


    public static String getId2FromLongCode(String inputString) {
        String[] parts = inputString.split("CALLING ");
        if (parts.length > 1) {
            String[] subParts = parts[1].split(" ");
            if (subParts.length > 0) {
                String value = subParts[0];
                value = value.trim();
                return value;
            }
        }
        return "-1";
    }
    public static String getId1FromShortCode(String inputString) {
        String[] parts = inputString.split("DE");
        if (parts.length > 1) {
            String[] subParts = parts[1].split("CALLING");
            if (subParts.length > 0) {
                String value = subParts[0];
                value = value.trim();
                return value;
            }
        }
        return "-1";
    }

    public static String getId2FromShortCode(String inputString) {
        String[] parts = inputString.split("SRC=");
        if (parts.length > 1) {
            String[] subParts = parts[1].split("=SRC");
            if (subParts.length > 0) {
                String value = subParts[0];
                return value;
            }
        }
        return "-1";
    }

    // TODO 设备号获取
    public static String getId1FromRecLongCodeMessage1(String inputString) {
//        String[] parts = inputString.split("R R R");
//        if (parts.length > 1) {
//            String[] subParts = parts[1].split("DE");
//            if (subParts.length > 0) {
//                String value = subParts[0];
//                value = value.trim();
//                return value;
//            }
//        }
//        return "-1";

//         获取最后一个"R "的索引
        int lastIndexR = inputString.lastIndexOf("R ");
        // 获取第一个"K "的索引
        int firstIndexK = inputString.indexOf("DE");
        if (lastIndexR != -1 && firstIndexK != -1 && lastIndexR < firstIndexK) {
            return inputString.substring(lastIndexR + 1, firstIndexK).trim();
        }
        return "-1";
    }

    public static String getShortCrc(String inputString) {
        int firstKIndex = inputString.indexOf("K K");
        int startIndex = firstKIndex - 5;
        firstKIndex = firstKIndex - 5;
        startIndex = startIndex - 5;
        return inputString.substring(startIndex, firstKIndex).trim();
    }

    public static String getLongCrc(String inputString) {
        int firstKIndex = inputString.indexOf("K K");
        int startIndex = firstKIndex - 5;
        return inputString.substring(startIndex, firstKIndex).trim();
    }


    // TODO 设备号获取
    public static String getId2FromRecLongCodeMessage1(String inputString) {
        String[] parts = inputString.split("DE");
        if (parts.length > 1) {
            String[] subParts = parts[1].split("READY");
            if (subParts.length > 0) {
                String value = subParts[0];
                value = value.trim();
                return value;
            }
        }
        return "-1";

//        int lastIndexR = inputString.lastIndexOf("R ");
//        // 获取第一个"K "的索引
//        int firstIndexK = inputString.indexOf("DE");
//        if (lastIndexR != -1 && firstIndexK != -1) {
//            return inputString.substring(lastIndexR + 1, firstIndexK).trim();
//        }
//        return "-1";

    }


    public static String getId1FromRecLongCodeMessage2(String inputString) {
//        String[] parts = inputString.split("R R R");
//        if (parts.length > 1) {
//            String[] subParts = parts[1].split("DE");
//            if (subParts.length > 0) {
//                String value = subParts[0];
//                value = value.trim();
//                return value;
//            }
//        }
//        return "-1";

        int lastIndexR = inputString.lastIndexOf("R ");
        // 获取第一个"K "的索引
        int firstIndexK = inputString.indexOf("DE");
        if (lastIndexR != -1 && firstIndexK != -1 && lastIndexR < firstIndexK) {
            return inputString.substring(lastIndexR + 1, firstIndexK).trim();
        }
        return "-1";
    }

    public static String getId2FromRecLongCodeMessage2(String inputString) {
        String[] parts = inputString.split("DE");
        if (parts.length > 1) {
            String[] subParts = parts[1].split("OK");
            if (subParts.length > 0) {
                String value = subParts[0];
                value = value.trim();
                return value;
            }
        }
        return "-1";
    }

    public static String getContentFromLongCode(String inputString) {
        int start = inputString.indexOf("DE");
        int end = inputString.indexOf(" K K");
        return inputString.substring(start, end).trim();
    }
}

