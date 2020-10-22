package com.kang.Server;

import com.kang.Dao.UserDaoImpl;
import com.kang.bean.ServerUser;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.util.ArrayList;

public class MasterServer {

    /**
     * 用户列表
     */
    private ArrayList<ServerUser> users;

    public ServerSocket masterServer;
    public WorkServer workServer;

    private int port = 8888;

    public void start() {
        users = new ArrayList<ServerUser>();
        try {
            masterServer = new ServerSocket(port);
            try {
                // 加载所有的用户列表
                users = (ArrayList<ServerUser>) UserDaoImpl.getInstance().findAll();
                // 首先全部设置为离线
                for (ServerUser u:users) {
                    u.setStatus("offline");
                }
                System.out.println("共读取数据库中用户数："+users.size()+"人");
            } catch (SQLException e) {
                System.out.println("用户列表初始化失败");
                e.printStackTrace();
            }
            System.out.println("服务器等待连接中...");
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            try {
                workServer = new WorkServer(masterServer.accept(), users);
                workServer.start();
                System.out.println("工作端启动一个线程");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
