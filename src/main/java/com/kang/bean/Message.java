package com.kang.bean;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *@Author heywecome
 *@ceeate 2020/10/21
 *@description
 */
public class Message implements Serializable{
    private String content = null;              // 聊天的内容
    private String speaker = null;              // 发言人
    private String timer = null;                // 发送时间
    private ArrayList<String>imageList = null;  // 表情

    public ArrayList<String> getImageList() {
        return imageList;
    }

    public void setImageList(ArrayList<String> imageList) {
        this.imageList = imageList;
    }

    public String getTimer() {
        return timer;
    }

    public void setTimer(String timer) {
        this.timer = timer;
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
