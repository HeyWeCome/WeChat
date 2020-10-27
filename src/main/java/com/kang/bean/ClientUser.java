package com.kang.bean;

import java.io.Serializable;

/**
 *@Author heywecome
 *@ceeate 2020/10/21
 *@description  客户端中的用户模型，用于界面上用户的控制
 */
public class ClientUser implements Serializable{
    private String userName;  // 用户的名字作为ID
    private String status;    // 用户的状态：在线，离线
    private boolean notify;   // 是否有消息发送过来

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isNotify() {
        return notify;
    }

    public void setNotify(boolean notify) {
        this.notify = notify;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
