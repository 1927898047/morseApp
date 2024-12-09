package com.zyj.morseapp.subcontract;

public enum HalfDuplexMode {
    RECEIVE_SHORT_CODE("接收短码报文"),
    SEND_SHORT_CODE_RESPONSE("发送短码应答");

    private final String mode;
    HalfDuplexMode(String mode) {
        this.mode = mode;
    }

}