package com.zyj.morseapp.utils.ptt;

import java.util.Arrays;

public class CharEncoder
{
    private char chr;
    private byte[] values;

    public char getChr() {
        return chr;
    }

    public void setChr(char value)
    {
        this.chr = value;
    }

    public byte[] getValues(){
        return values;
    }

    public void setValues(byte[] value)
    {
        if (value == null || value.length == 0)
            values = null;
        else
        {
            values = Arrays.copyOf(value, value.length);
        }
    }

    public boolean compare(char ch)
    {
        return this.chr == ch;
    }

    public CharEncoder(char ch, byte[] value)
    {
        this.chr = ch;
        if (value == null || value.length == 0)
            values = null;
        else
            values = Arrays.copyOf(value, value.length);
    }
}