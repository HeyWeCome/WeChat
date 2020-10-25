package com.kang.server;

import com.kang.dao.UserDaoImpl;
import com.kang.utils.GsonUtils;
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

import static com.kang.utils.Constants.*;

/**
 * @Author heywecome
 * @ceeate 2020/10/22
 * @description 主要的工作服务端，处理具体的业务
 */
public class WorkServer extends Thread {

    private ServerUser workUser;            // 当前已经连接的用户
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
                sentMessageToClient();              // 发送信息给客户端
                if (isLogOut) {                     // 关闭I/O流
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
//        System.out.println("处理的信息为：" + readLine);
        Map<Integer, Object> gsonMap = GsonUtils.GsonToMap(readLine);               // 将传来的数据读取出来
        Integer command = GsonUtils.Double2Integer((Double) gsonMap.get(COMMAND));  // 获取指令

        HashMap map = new HashMap();    //
        String userName;                // 用户名
        String password;                // 密码

        switch (command) {
            case COM_GROUP:         // 获取在线用户列表
                writer.println(getGroup());
                System.out.println(workUser.getUserName() + "请求获得在线用户详情");
                break;
            case COM_SIGNUP:        // 注册
                userName = (String) gsonMap.get(USERNAME);
                password = (String) gsonMap.get(PASSWORD);
                map.put(COMMAND, COM_RESULT);
                if (createUser(userName, password)) {
                    // 需要马上变更心跳
                    currentTime = new Date().getTime();
                    // 存储信息
                    map.put(COM_RESULT, SUCCESS);
                    map.put(COM_DESCRIPTION, "success");
                    writer.println(gson.toJson(map));
                    broadcast(getGroup(),COM_SIGNUP);
                    System.out.println("用户" + userName + "注册上线了");
                } else {
                    // 注册失败，也就是这个用户已经被注册了
                    map.put(COM_RESULT, FAILED);
                    map.put(COM_DESCRIPTION, userName + "已经被注册");
                    writer.println(gson.toJson(map)); //返回消息给客户端
                    System.out.println(userName + "该用户已经被注册");
                }
                break;
            case COM_LOGIN:         // 登录
                userName = (String) gsonMap.get(USERNAME);
                password = (String) gsonMap.get(PASSWORD);
                boolean find = false;
                for (ServerUser user : users) {
                    if (user.getUserName().equals(userName)) {
                        if (!user.getPassword().equals(password)) {
                            map.put(COM_DESCRIPTION, "账号密码输入有误");
                            break;
                        }
                        if (user.getStatus().equals("online")) {
                            map.put(COM_DESCRIPTION, "该用户已经登录");
                            break;
                        }
                        currentTime = new Date().getTime();                 // 变更心跳时间
                        map.put(COM_RESULT, SUCCESS);                       // 设置操作结果为成功
                        map.put(COM_DESCRIPTION, userName + "success");     // 添加操作成功的描述
                        user.setStatus("online");                           // 设置用户的状态为在线
                        writer.println(gson.toJson(map));                   // 发送给客户端
                        workUser = user;                                    // 绑定当前的账号
                        broadcast(getGroup(), COM_SIGNUP);                  // 广播给所有的用户，当前账号注册成功并登录
                        find = true;                                        // 找到了该用户的注册信息
                        System.out.println("用户" + userName + "上线了");   // 控制台打印信息，记录
                        break;
                    }
                }
                if (!find) {                                                // 如果没有找到这个用户
                    map.put(COM_RESULT, FAILED);                            // 设置操作的结果为failed
                    if (!map.containsKey(COM_DESCRIPTION))
                        map.put(COM_DESCRIPTION, userName + "未注册");
                    writer.println(gson.toJson(map));                       // 返回消息给服务器
                }
                break;
            case COM_CHATWITH:          // 单聊
                String receiver = (String) gsonMap.get(RECEIVER);           // 获取想要聊天的对象
                map = new HashMap();                                        // 存放一些必要的信息
                map.put(COMMAND, COM_CHATWITH);                             // 当前的操作命令为：单聊
                map.put(SPEAKER, gsonMap.get(SPEAKER));                     // 存放当前的发言人
                map.put(RECEIVER, gsonMap.get(RECEIVER));                   // 存放想要聊天的对象
                map.put(CONTENT, gsonMap.get(CONTENT));                     // 存放聊天发送的内容
                map.put(TIME, getFormatDate());                             // 存放发送聊天信息的时间

                for (ServerUser u : users) {                                // 查找想要聊天的对象
                    if (u.getUserName().equals(receiver)) {
                        u.addMsg(gson.toJson(map));                         // 把信息发送过去
                        break;
                    }
                }
                workUser.addMsg(gson.toJson(map));                          // 自己也记录下刚才自己发言内容
                break;
            case COM_CHATALL:           // 群聊
                map = new HashMap();
                map.put(COMMAND, COM_CHATALL);                              // 存放操作命令：群聊
                map.put(SPEAKER, workUser.getUserName());                   // 存放当前的发言人：即当前用户
                map.put(TIME, getFormatDate());                             // 获取当前的时间
                map.put(CONTENT, gsonMap.get(CONTENT));                     // 存放发言的内容
                broadcast(gson.toJson(map), COM_MESSAGEALL);                // 开始广播
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
     * @param type    that contain "message", "logOut", "signUp"
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
                System.out.println("============================================");
                System.out.println("发送给:"+workUser.getUserName() + " 的信息为: " + message + "剩余 size：" + workUser.session.size());
                System.out.println("============================================");
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
     * 创建用户名并且绑定用户名，如果失败就会返回failed
     * 如果成功，会添加到用户中
     * @param userName
     * @return
     */
    private boolean createUser(String userName, String password) {
        // 先检查是否已经存在这个用户，如果已经存在，则直接返回false
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
     * 格式为：用户名，状态
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
