package com.kang.Server;

import com.kang.Dao.UserDaoImpl;
import com.kang.Utils.GsonUtils;
import com.kang.bean.ServerUser;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.kang.Utils.Constants.*;

/**
 * @Author heywecome
 * @ceeate 2020/10/22
 * @description 主要的工作服务端，处理具体的业务
 */
public class WorkServer extends Thread {

    private ServerUser workUser;            // 已经连接的用户
    private Socket socket;                  // 用户的端口信息
    private ArrayList<ServerUser> users;    // 存放所有的用户信息
    private BufferedReader reader;
    private PrintWriter writer;
    private boolean isLogOut = false;
    private long currentTime = 0;
    private Gson gson;

    /**
     * 构造方法
     * @param socket
     * @param users
     */
    public WorkServer(Socket socket, ArrayList users) {
        super();
        gson = new Gson();
        this.socket = socket; // 绑定socket
        this.users = users;   // 获取所有的用户资源
    }

    @Override
    public void run() {
        // 完成服务端的工作
        try {
            currentTime = new Date().getTime();                                             // 读取当前的时间
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            String readLine;
            while (true) {
                // 心跳检测
                // 之前考虑上线下线方式的时候想到的一个办法是，下线的时候给服务器发送下线通知，后面考虑了出现断网等突发情况时这样的设计将出现问题。所以采用了心跳连接的方式。
                // server端采用了以时间差为判断方式的连接判断方式，通过具体的实践server端的时间差为2000ms较为合适.
                // client维持了500ms的心跳
                long newTime = new Date().getTime();
                if (newTime - currentTime > 2000) { // 超过连接时间2000ms,就退出
                    logOut();
                } else {
                    currentTime = newTime;          // 重新设置心跳
                }
                readLine = reader.readLine();       // 读取发送过来的请求
                if (readLine == null)
                    logOut();
                handleMessage(readLine);            // 处理发送的请求
                sentMessageToClient();
                if (isLogOut) {
                    // kill the I/O stream
                    reader.close();
                    writer.close();
                    break;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            logOut();
        } catch (IOException e) {
            e.printStackTrace();
            logOut();
        }
    }


    /**
     * the message to deal with Client's command
     * 处理信息，根据不同的请求，执行不同的操作
     * @param readLine
     */
    private void handleMessage(String readLine) {
        System.out.println("处理的信息为：" + readLine);
        Map<Integer, Object> gsonMap = GsonUtils.GsonToMap(readLine);
        Integer command = GsonUtils.Double2Integer((Double) gsonMap.get(COMMAND));
        HashMap map = new HashMap();
        String username, password;
        switch (command) {
            case COM_GROUP:
                writer.println(getGroup());
                System.out.println(workUser.getUserName() + "请求获得在线用户详情");
                break;
            case COM_SIGNUP:
                username = (String) gsonMap.get(USERNAME);
                password = (String) gsonMap.get(PASSWORD);
                map.put(COMMAND, COM_RESULT);
                if (createUser(username, password)) {
                    //需要马上变更心跳
                    currentTime = new Date().getTime();
                    //存储信息
                    map.put(COM_RESULT, SUCCESS);
                    map.put(COM_DESCRIPTION, "success");
                    writer.println(gson.toJson(map));
                    broadcast(getGroup(),COM_SIGNUP);
                    System.out.println("用户" + username + "注册上线了");
                } else {
                    map.put(COM_RESULT, FAILED);
                    map.put(COM_DESCRIPTION, username + "已经被注册");
                    writer.println(gson.toJson(map)); //返回消息给服务器
                    System.out.println(username + "该用户已经被注册");
                }
                break;
            case COM_LOGIN:
                username = (String) gsonMap.get(USERNAME);
                password = (String) gsonMap.get(PASSWORD);
                boolean find = false;
                for (ServerUser u : users) {
                    if (u.getUserName().equals(username)) {
                        if (!u.getPassword().equals(password)) {
                            map.put(COM_DESCRIPTION, "账号密码输入有误");
                            break;
                        }
                        if (u.getStatus().equals("online")) {
                            map.put(COM_DESCRIPTION, "该用户已经登录");
                            break;
                        }
                        currentTime = new Date().getTime();
                        map.put(COM_RESULT, SUCCESS);
                        map.put(COM_DESCRIPTION, username + "success");
                        u.setStatus("online");
                        writer.println(gson.toJson(map));
                        workUser = u;
                        broadcast(getGroup(), COM_SIGNUP);
                        find = true;
                        System.out.println("用户" + username + "上线了");
                        break;
                    }
                }
                if (!find) {
                    map.put(COM_RESULT, FAILED);
                    if (!map.containsKey(COM_DESCRIPTION))
                        map.put(COM_DESCRIPTION, username + "未注册");
                    writer.println(gson.toJson(map)); //返回消息给服务器
                }
                break;
            case COM_CHATWITH:
                String receiver = (String) gsonMap.get(RECEIVER);
                map = new HashMap();
                map.put(COMMAND, COM_CHATWITH);
                map.put(SPEAKER, gsonMap.get(SPEAKER));
                map.put(RECEIVER, gsonMap.get(RECEIVER));
                map.put(CONTENT, gsonMap.get(CONTENT));
                map.put(TIME, getFormatDate());
                for (ServerUser u : users) {
                    if (u.getUserName().equals(receiver)) {
                        u.addMsg(gson.toJson(map));
                        break;
                    }
                }
                workUser.addMsg(gson.toJson(map));
                break;
            case COM_CHATALL:
                map = new HashMap();
                map.put(COMMAND, COM_CHATALL);
                map.put(SPEAKER, workUser.getUserName());
                map.put(TIME, getFormatDate());
                map.put(CONTENT, gsonMap.get(CONTENT));
                broadcast(gson.toJson(map), COM_MESSAGEALL);
                break;
            default:
                break;
        }
    }

    /**
     * @return
     * 当前时间的formatDate字符串
     */
    public String getFormatDate() {
        Date date = new Date();
        long times = date.getTime();        //时间戳
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = formatter.format(date);
        return dateString;
    }

    /**
     * 将信息广播给所有的用户
     * 主要就是三种信息：信息、登出、注册并登录。
     *
     * @param message the message
     * @param type    that contain "message", "logOUt", "signUp"
     */
    private void broadcast(String message, int type) {
        System.out.println(workUser.getUserName() + " 开始广播broadcast " + message);

        switch (type) {
            case COM_MESSAGEALL:
                for (ServerUser u : users) {
                    u.addMsg(message);
                }
                break;
            case COM_LOGOUT:
            case COM_SIGNUP:
                for (ServerUser u : users) {
                    if (!u.getUserName().equals(workUser.getUserName())) {
                        u.addMsg(message);
                    }
                }
                break;
        }

    }

    /**
     * 发送信息给客户端
     */
    private void sentMessageToClient() {
        String message;
        if (workUser != null)
            while ((message = workUser.getMsg()) != null) {
                writer.println(message);    // write的时候会自动刷新
                System.out.println(workUser.getUserName() + "的数据仓发送 message: " + message + "剩余 size" + workUser.session.size());
            }
    }

    /**
     * 释放socket的资源
     */
    private void logOut() {
        // 如果当前用户已经被释放了，直接返回
        if (workUser == null)
            return;
        // 提示当前用户下线
        System.out.println("用户 " + workUser.getUserName() + " 已经离线");
        // 依然保存着这个用户，改变他的状态
        workUser.setStatus("offline");
        for (ServerUser u : users) {
            if (u.getUserName().equals(workUser.getUserName()))
                u.setStatus("offline");
        }
        broadcast(getGroup(), COM_LOGOUT);
        isLogOut = true;
    }

    /**
     * 给一个随机的名字
     * @return
     */
    private String getRandomName() {
        String[] str1 = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
                "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
                "w", "x", "y", "z", "1", "2", "3", "4", "5", "6", "7", "8",
                "9", "0", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
                "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
                "W", "X", "Y", "Z"};
        StringBuilder name = new StringBuilder();
        String userName = name.toString();
        Random ran = new Random();
        boolean success = false;
        do {
            for (int i = 0; i < 6; i++) {
                int n = ran.nextInt(str1.length);
                String str = str1[n];
                name.append(str);
                System.out.println(name);
            }
            success = true;
            userName = name.toString();
            for (ServerUser user : users) {
                if (userName.equals(user.getUserName())) {
                    success = false;
                    break;
                }
            }
        } while (!success);
        return userName;
    }

    /**
     * create username and bind userName . if failed it will return failed.
     * if success it will add to users.
     *
     * @param userName
     * @return
     */
    private boolean createUser(String userName, String password) {
        for (ServerUser user : users) {
            if (user.getUserName().equals(userName)) {
                return false;
            }
        }
        // 将用户加入用户列表中
        ServerUser newUser = new ServerUser(users.size(), userName, password);
        newUser.setStatus("online");
        users.add(newUser);
        // 添加用户到数据库
        try {
            UserDaoImpl.getInstance().add(newUser);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        workUser = newUser;
        return true;
    }

    /**
     * 返回json格式的用户组
     * @return
     */
    private String getGroup() {
        String[] userlist = new String[users.size() * 2];
        int j = 0;
        for (int i = 0; i < users.size(); i++, j++) {
            userlist[j] = users.get(i).getUserName();
            userlist[++j] = users.get(i).getStatus();
        }
        HashMap map = new HashMap();
        map.put(COMMAND, COM_GROUP);
        map.put(COM_GROUP, userlist);
        return gson.toJson(map);
    }
}
