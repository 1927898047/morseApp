package com.zyj.morseapp.morsecoder;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;


/**
 * 长码编码类
 */
public final class MorseLongCoder {

    private static final Map<Integer, String> alphabets = new HashMap<>();    // code point -> morse
    private static final Map<String, Integer> dictionaries = new HashMap<>(); // morse -> code point

    private static void registerMorse(Character abc, String dict) {
        alphabets.put(Integer.valueOf(abc), dict);
        dictionaries.put(dict, Integer.valueOf(abc));
    }
    static {
        // Letters
        registerMorse('A', "01");
        registerMorse('B', "1000");
        registerMorse('C', "1010");
        registerMorse('D', "100");
        registerMorse('E', "0");
        registerMorse('F', "0010");
        registerMorse('G', "110");
        registerMorse('H', "0000");
        registerMorse('I', "00");
        registerMorse('J', "0111");
        registerMorse('K', "101");
        registerMorse('L', "0100");
        registerMorse('M', "11");
        registerMorse('N', "10");
        registerMorse('O', "111");
        registerMorse('P', "0110");
        registerMorse('Q', "1101");
        registerMorse('R', "010");
        registerMorse('S', "000");
        registerMorse('T', "1");
        registerMorse('U', "001");
        registerMorse('V', "0001");
        registerMorse('W', "011");
        registerMorse('X', "1001");
        registerMorse('Y', "1011");
        registerMorse('Z', "1100");
        // Numbers
        registerMorse('0', "11111");
        registerMorse('1', "01111");
        registerMorse('2', "00111");
        registerMorse('3', "00011");
        registerMorse('4', "00001");
        registerMorse('5', "00000");
        registerMorse('6', "10000");
        registerMorse('7', "11000");
        registerMorse('8', "11100");
        registerMorse('9', "11110");
        // Punctuation
        registerMorse('.', "010101");
        registerMorse(',', "110011");
        registerMorse('?', "001100");
        registerMorse('\'', "011110");
        registerMorse('!', "101011");
        registerMorse('/', "10010");
        registerMorse('(', "10110");
        registerMorse(')', "101101");
        registerMorse('&', "01000");
        registerMorse(':', "111000");
        registerMorse(';', "101010");
        registerMorse('=', "10001");
        registerMorse('+', "01010");
        registerMorse('-', "100001");
        registerMorse('_', "001101");
        registerMorse('"', "010010");
        registerMorse('$', "0001001");
        registerMorse('@', "011010");
        registerMorse(' ',"");
    }

    private final char dit; // short mark or dot
    private final char dah; // longer mark or dash
    private final char split;

    public MorseLongCoder() {
        this('.', '-', '/');
    }

    public MorseLongCoder(char dit, char dah, char split) {
        this.dit = dit;
        this.dah = dah;
        this.split = split;
    }
    public String encode(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text should not be null!");
        }
        StringBuilder morseBuilder = new StringBuilder();
        text = text.toUpperCase();//小写转大写
        for (int i = 0; i < text.codePointCount(0, text.length()); i++) {
            //codePointCount:计算文本的字符数
            //offsetByCodePoints()
            //codePointAt(int i):返回指定索引处的字符unicode值


            //codepoint是每一个字符的unicode编码，无论中英文，codePoint是unicode的10进制
            int codePoint = text.codePointAt(text.offsetByCodePoints(0, i));

            //unicode如果对应的是中文字符，则返回null；英文字符则返回对应的摩尔斯码（由0，1组成）
            String word = alphabets.get(codePoint);
            morseBuilder.append(word.replace('0', dit).replace('1', dah)).append(split);
            if(codePoint==(int) ' '){
                morseBuilder.append("/");
            }
        }
        System.out.println("生成长码编码");
        return morseBuilder.toString();
    }

    public String decode(String morse) {
        if (morse == null) {
            throw new IllegalArgumentException("Morse should not be null.");
        }

        StringBuilder textBuilder = new StringBuilder();

        StringTokenizer tokenizer = new StringTokenizer(morse, String.valueOf(split));
        while (tokenizer.hasMoreTokens()) {

            //nextToken():获取下一个分隔符的字符（其实就是二进制字节码）
            String word = tokenizer.nextToken().replace(dit, '0').replace(dah, '1');


            Integer codePoint = dictionaries.get(word);
            textBuilder.appendCodePoint(codePoint);
        }
        return textBuilder.toString();
    }

}


