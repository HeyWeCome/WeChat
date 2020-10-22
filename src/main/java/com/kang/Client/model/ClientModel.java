package com.kang.Client.model;

import com.kang.Client.chatroom.MainView;
import com.kang.Utils.GsonUtils;
import com.kang.bean.ClientUser;
import com.kang.bean.Message;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.*;

import static com.kang.Utils.Constants.*;

public class ClientModel {

    private BufferedReader reader;
    private PrintWriter writer;
    private Socket client;                              // 客户端
    private final int port = 8888;                      // 连接的端口号
    private String IP;                                  // IP地址（尚未用到）
    private boolean isConnect = false;                  // 连接标志，是否已经连接
    private boolean chatChange = false;
    private String chatUser = "[group]";
    private String thisUser;                            // 当前用户
    private Gson gson;                                  // Gson是Google提供的用来在 Java 对象和 JSON 数据之间进行映射的 Java 类库。可以将一个 JSON 字符串转成一个 Java 对象，或者反过来。

    // 不过HashMap有一个问题，就是迭代HashMap的顺序并不是HashMap放置的顺序，也就是无序。
    // 通过维护一个运行于所有条目的双向链表，LinkedHashMap保证了元素迭代的顺序。该迭代顺序可以是插入顺序或者是访问顺序。
    private LinkedHashMap<String, ArrayList<Message>> userSession;   // 用户消息队列存储用
    private Thread keepalive = new Thread(new KeepAliveWatchDog());
    private Thread keepreceive = new Thread(new ReceiveWatchDog());
    private static ClientModel instance;

    //允许侦听器跟踪更改发生的列表。
    private ObservableList<ClientUser> userList;        // 用户列表
    private ObservableList<Message> chatRecoder;        // 聊天信息的记录

    private ClientModel() {
        super();
        gson = new Gson();
        ClientUser user = new ClientUser();
        user.setUserName("[group]");
        user.setStatus("");
        userSession = new LinkedHashMap<>();
        userSession.put("[group]", new ArrayList<>());

        // FXCollections 一比一包含了 java.util.Collections中的方法
        userList = FXCollections.observableArrayList();
        chatRecoder = FXCollections.observableArrayList();
        userList.add(user);
    }

    /**
     * 单例模式，保持唯一的对象
     * @return
     */
    public static ClientModel getInstance() {
        if (instance == null) {
            synchronized (ClientModel.class) {
                if (instance == null) {
                    instance = new ClientModel();
                }
            }
        }
        return instance;
    }

    /**
     * 设置当前的聊天对象
     * @param chatUser
     */
    public void setChatUser(String chatUser) {
        if (!this.chatUser.equals(chatUser))
            chatChange = true;
        this.chatUser = chatUser;

        // 消除未读信息状态
        for (int i = 0; i < userList.size(); i++) {
            ClientUser user = userList.get(i);
            if (user.getUserName().equals(chatUser)) {
                if (user.isNotify()) {  // 已读
                    System.out.println("更改消息目录"+user.getUserName()+"有信息");
                    userList.remove(i);
                    userList.add(i, user);
                    user.setNotify(false);
                }
                chatRecoder.clear();
                chatRecoder.addAll(userSession.get(user.getUserName()));
                break;
            }
        }
    }

    public ObservableList<Message> getChatRecoder() {
        return chatRecoder;
    }

    public ObservableList<ClientUser> getUserList() {
        return userList;
    }

    public String getThisUser() {
        return thisUser;
    }

    class KeepAliveWatchDog implements Runnable {
        @Override
        public void run() {
            HashMap<Integer, Integer> map = new HashMap<>();
            map.put(COMMAND, COM_KEEP);
            try {
                System.out.println("keep alive start" + Thread.currentThread());
                //heartbeat detection
                while (isConnect) {
                    Thread.sleep(500);
//                    System.out.println("500ms keep");
                    writer.println(gson.toJson(map));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    class ReceiveWatchDog implements Runnable {
        @Override
        public void run() {
            try {
                System.out.println(" Receieve start" + Thread.currentThread());
                String message;
                while (isConnect) {
                    message = reader.readLine();
                    // System.out.println("读取服务器信息" + message);
                    handleMessage(message);
                }
            } catch (IOException e) {

            }
        }
    }

    /**
     * 断开连接
     */
    public void disConnect() {
        isConnect = false;
        keepalive.stop();
        keepreceive.stop();
        if (writer != null) {
            writer.close();
            writer = null;
        }
        if (client != null) {
            try {
                client.close();
                client = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void handleMessage(String message) {
        Map<Integer, Object> gsonMap = GsonUtils.GsonToMap(message);
        Integer command = GsonUtils.Double2Integer((Double) gsonMap.get(COMMAND));
        Message m;
        switch (command) {
            case COM_GROUP:
                HashSet<String> recoder = new HashSet<>();
                for (ClientUser u : userList) {
                    if (u.isNotify()) {
                        recoder.add(u.getUserName());
                    }
                }
                ArrayList<String> userData = (ArrayList<String>) gsonMap.get(COM_GROUP);
                userList.remove(1, userList.size());
                int onlineUserNum = 0;
                for (int i = 0; i < userData.size(); i++) {
                    ClientUser user = new ClientUser();
                    user.setUserName(userData.get(i));
                    user.setStatus(userData.get(++i));
                    if (user.getStatus().equals("online"))
                        onlineUserNum++;
                    if (recoder.contains(user.getUserName())) {
                        user.setNotify(true);
                        user.setStatus(user.getStatus() + "(*)");
                    }
                    userList.add(user);
                    userSession.put(user.getUserName(), new ArrayList<>());
                }
                int finalOnlineUserNum = onlineUserNum;
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        MainView.getInstance().getLabUserCoumter().setText("服务器在线人数为" + finalOnlineUserNum);
                    }
                });
                break;
            case COM_CHATALL:
                m = new Message();
                m.setTimer((String) gsonMap.get(TIME));
                m.setSpeaker((String) gsonMap.get(SPEAKER));
                m.setContent((String) gsonMap.get(CONTENT));
                if (chatUser.equals("[group]")) {
                    chatRecoder.add(m);
                }
                userSession.get("[group]").add(m);
                break;
            case COM_CHATWITH:
                String speaker = (String) gsonMap.get(SPEAKER);
                String receiver = (String) gsonMap.get(RECEIVER);
                String time = (String) gsonMap.get(TIME);
                String content = (String) gsonMap.get(CONTENT);
                m = new Message();
                m.setSpeaker(speaker);
                m.setContent(content);
                m.setTimer(time);
                if (thisUser.equals(receiver)) {
                    if (!chatUser.equals(speaker)) {
                        for (int i = 0; i < userList.size(); i++) {
                            if (userList.get(i).getUserName().equals(speaker)) {
                                ClientUser user = userList.get(i);
                                if (!user.isNotify()) {
                                    //user.setStatus(userList.get(i).getStatus() + "(*)");
                                    user.setNotify(true);
                                }
                                userList.remove(i);
                                userList.add(i, user);
                                break;
                            }
                        }
                        System.out.println("标记未读");
                    }else{
                        chatRecoder.add(m);
                    }
                    userSession.get(speaker).add(m);
                }else{
                    if(chatUser.equals(receiver))
                        chatRecoder.add(m);
                    userSession.get(receiver).add(m);
                }
                break;
            default:
                break;
        }
        System.out.println("服务器发来消息" + message + "消息结束");
    }


    /**
     * 该方法作废
     * @param chatUser
     * @return
     */
    private LinkedList<Message> loadChatRecoder(String chatUser) {
        LinkedList<Message> messagesList = new LinkedList<>();
        if (userSession.containsKey(chatUser)) {
            ArrayList<Message> recoder = userSession.get(chatUser);
            for (Message s : recoder) {
                messagesList.add(s);
            }
        }
        return messagesList;
    }

    /**
     * 发送JSON格式的字符串信息给服务端
     * @param message 必须为JSON格式的
     */
    public void sentMessage(String message) {
        writer.println(message);
    }


    /**
     * 检测登录
     * IP可以不填写，默认是本机的IP，127.0.0.1
     * @param username
     * @param IP
     * @param buf
     * @return
     *
     * 因此我们用gson.fromJson(msg, new TypeToken<Map<String, Object>>() {}.getType())
     * 将json字符串msg例如:{"id":20,"name":"test"}转换成Map<String,Object>时，
     * 就会把数字类型的值都转换成了Double类型(此时map中key为“id”的值是一个Double类型，为20.0)
     * 当我们再把这个Map用gson.toJson转换成json字符串时，奇葩的事情就发生了，不再和我们最开始传进来的json字符串一致了，变成了{"id":20.0,"name":"test"}
     */
    public boolean CheckLogin(String username, String IP, String password, StringBuffer buf, int type) {
        this.IP = IP;           //绑定服务器IP
        Map<Integer, Object> map;
        try {
            //针对多次尝试登录
            if (client == null || client.isClosed()) {
                client = new Socket(IP, port);
                reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                writer = new PrintWriter(client.getOutputStream(), true);
            }
            map = new HashMap<>();

            if (type == 0)
                map.put(COMMAND, COM_LOGIN);        // 登录
            else
                map.put(COMMAND, COM_SIGNUP);       // 注册

            // 将用户名密码都存放进入map里面
            map.put(USERNAME, username);
            map.put(PASSWORD, password);

            // 转化为json格式的数据传送出去
            writer.println(gson.toJson(map));
            // readline是线程阻塞的，一直等待，检测当前这个账号状态，用户已经注册|用户尚未注册|密码错误|服务器连接失败等...
            String strLine = reader.readLine();
            System.out.println("登录信息校验："+strLine);
            // 将String转化为map对象
            map = GsonUtils.GsonToMap(strLine);
            // COM_RESULT两种结果，成功则1.0 失败则2.0
            Integer result = GsonUtils.Double2Integer((Double) map.get(COM_RESULT));

            // 如果成功，则允许用户登录系统
            if (result == SUCCESS) {
                isConnect = true;
                // 请求分组
                map.clear();
                // 放置请求分组的命令
                map.put(COMMAND, COM_GROUP);
                // 发送过去，服务端处理
                writer.println(gson.toJson(map));
                thisUser = username;        // 设置当前的用户名
                keepalive.start();          // 启动心跳检测
                keepreceive.start();
                return true;
            } else {
                // 将没有登录的原因展示出来，用户已经注册|用户尚未注册|密码错误|服务器连接失败等...
                String description = (String) map.get(COM_DESCRIPTION);
                buf.append(description);
                return false;
            }
        } catch (ConnectException e) {
            buf.append(e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            buf.append(e.toString());
        }
        return false;
    }


}
