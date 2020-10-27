package com.kang.utils;

/**
 * 一些常用的变量集合到一个地方
 * 消息机制
 * 采用json作为通信载体，后期功能性的更改较为简单
 * 通信命令字设计如下
 */
public class Constants {
    public final static boolean SINGLE = true;
    public final static boolean GROUP = false;

    // 状态
    public final static int SUCCESS = 0x01;
    public final static int FAILED = 0x02;

    // 信息
    public static Integer  COMMAND = 0x10;          // 16
    public static Integer  TIME = 0x11;             // 17
    public static Integer  USERNAME = 0x12;         // 18
    public static Integer  PASSWORD = 0x13;         // 19
    public static Integer  SPEAKER = 0x14;          // 20
    public static Integer  RECEIVER = 0x15;         // 21
    public static Integer  CONTENT= 0x16;           // 22
    public static Integer  ACCOUNT= 0x17;           // 23


    // 命令
    public final static int COM_LOGIN = 0x20;       // 32
    public final static int COM_SIGNUP = 0x21;      // 33
    public final static int COM_RESULT = 0x22;      // 34
    public final static int COM_DESCRIPTION = 0x23; // 35
    public final static int COM_LOGOUT =0x24;       // 36
    public final static int COM_CHATWITH = 0x25;    // 37
    public final static int COM_GROUP = 0x26;       // 38
    public final static int COM_CHATALL = 0x27;     // 39
    public final static int COM_KEEP = 0x28;        // 40
    public final static int COM_MESSAGEALL = 0X29;  // 41
}
