package com.kang.client.net;

import com.kang.client.view.MainView;
import com.kang.utils.GsonUtils;
import com.kang.bean.ClientUser;
import com.kang.bean.Message;
import com.google.gson.Gson;
import com.kang.utils.PlaySound;
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
import java.util.concurrent.TimeUnit;

import static com.kang.utils.Constants.*;

public class Client {

    private BufferedReader reader;
    private PrintWriter writer;
    private Socket client;                              // 客户端
    private final int port = 8888;                      // 连接的端口号
    private String IP;                                  // IP地址（尚未用到）
    private boolean isConnect = false;                  // 连接标志，是否已经连接
    private boolean chatChange = false;
    private String chatUser = "[group]";                // 用户左侧选择的用户，默认为群聊
    private String thisUser;                            // 当前用户
    private Gson gson;                                  // Gson是Google提供的用来在 Java 对象和 JSON 数据之间进行映射的 Java 类库。可以将一个 JSON 字符串转成一个 Java 对象，或者反过来。

    // 不过HashMap有一个问题，就是迭代HashMap的顺序并不是HashMap放置的顺序，也就是无序。
    // 通过维护一个运行于所有条目的双向链表，LinkedHashMap保证了元素迭代的顺序。该迭代顺序可以是插入顺序或者是访问顺序。
    private LinkedHashMap<String, ArrayList<Message>> userSession;   // 用户消息队列存储用
    private Thread keepalive = new Thread(new KeepAliveWatchDog());
    private Thread keepreceive = new Thread(new ReceiveWatchDog());
    private static Client instance;

    //允许侦听器跟踪更改发生的列表。
    private ObservableList<ClientUser> userList;            // 用户列表
    private ObservableList<Message> chatRecoder;            // 聊天信息的记录

    private Client() {
        super();
        gson = new Gson();
        ClientUser user = new ClientUser();                 // 新建一个群聊对象，因为要加到左侧界面
        user.setUserName("[group]");
        user.setStatus("");
        userSession = new LinkedHashMap<>();                // 存放的是消息，他和谁聊了天，封装的是 JSON 格式的 Message数据
        userSession.put("[group]", new ArrayList<>());      // 先把群聊的存起来，[group]为群聊的标识符

        // FXCollections 一比一包含了 java.util.Collections中的方法
        userList = FXCollections.observableArrayList();     // 用户列表
        chatRecoder = FXCollections.observableArrayList();  // 界面上的聊天框
        userList.add(user);                                 // 把群聊的对象加入至左侧的界面上的用户列表中
    }

    /**
     * 单例模式，保持唯一的对象
     * @return
     */
    public static Client getInstance() {
        if (instance == null) {
            synchronized (Client.class) {
                if (instance == null) {
                    instance = new Client();
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

        this.chatUser = chatUser;           // 设置当前的聊天对象

        // 消除未读信息状态
        for (int i = 0; i < userList.size(); i++) {
            ClientUser user = userList.get(i);
            if (user.getUserName().equals(chatUser)) {
                if (user.isNotify()) {      // 显示有消息提示，用户点击之后取消消息提示
                    System.out.println("更改消息目录"+user.getUserName()+"有信息");
                    userList.remove(i);     // 从面板中移除对象，不然面板是不会更新的
                    userList.add(i, user);  // 重新添加对象
                    user.setNotify(false);  // 设置用户的通知提示为false
                }
                chatRecoder.clear();        // 清空当前的面板
                chatRecoder.addAll(userSession.get(user.getUserName()));    // 将session中的聊天记录，通过用户的名字获取出来
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

    /**
     * 不断监听，每隔500ms都给服务器发送个数据
     */
    class KeepAliveWatchDog implements Runnable {
        @Override
        public void run() {
            HashMap<Integer, Integer> map = new HashMap<>();
            map.put(COMMAND, COM_KEEP);
            try {
                System.out.println("keep alive start" + Thread.currentThread());
                // 心跳检测
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

    /**
     * 不断监听，从服务器读取数据
     */
    class ReceiveWatchDog implements Runnable {
            @Override
            public void run() {
                try {
                    String message;
                    while (isConnect) {
                        // Thread.currentThread表示当前代码段正在被哪个线程调用的相关信息。
                        System.out.println("接受信息走的是线程：" + Thread.currentThread());
                        message = reader.readLine();
                        // System.out.println("读取服务器信息" + message);
                        handleMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    /**
     * 断开连接
     * stop方法不做推荐，最好使用 interrupt
     * 就是有一个热水器（对象），本来调的是热水（就是指对象的属性），
     * 但是这个洗澡的人临时突然有事就出去了，没有继续占用这个热水器，接着下一个进来的人就不想洗热水，
     * 把热水调成了冷水，这时这个对象属性就发生了改变，就是这种改变，就是所谓其他线程修改了对象
     */
    public void disConnect() {
        isConnect = false;
//        keepalive.stop();
//        keepreceive.stop();
        keepalive.interrupt();
        keepreceive.interrupt();

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

    /**
     * 处理从服务端发送来的请求
     * @param message
     */
    private void handleMessage(String message) {
        Map<Integer, Object> gsonMap = GsonUtils.GsonToMap(message);                    // 提取服务端的数据，解析json格式
        Integer command = GsonUtils.Double2Integer((Double) gsonMap.get(COMMAND));      // 获取操作
        Message m;                                                                      // 存放信息
        switch (command) {
            case COM_GROUP:     // 群聊
                HashSet<String> recoder = new HashSet<>();  // 记录这个人有没有给我发消息，发了的话就加进去

                for (ClientUser u : userList) {             // 遍历
                    if (u.isNotify()) {                     // 如果有通知信息，就加入到这个里面
                        recoder.add(u.getUserName());       //
                    }
                }

                ArrayList<String> userData = (ArrayList<String>) gsonMap.get(COM_GROUP);    // 获取所有的用户信息
                userList.remove(1, userList.size());             // 清空当前的用户列表
                int onlineUserNum = 0;                           // 现在的在线用户

                for (int i = 0; i < userData.size(); i++) {
                    ClientUser user = new ClientUser();
                    user.setUserName(userData.get(i));
                    user.setStatus(userData.get(++i));
                    if (user.getStatus().equals("online"))
                        onlineUserNum++;                         // 如果在线，在线人数加一
                    if (recoder.contains(user.getUserName())) {  // 给我发了消息的，改变下状态
                        user.setNotify(true);
                        user.setStatus(user.getStatus() + "(*)");
                    }
                    userList.add(user);
                    userSession.put(user.getUserName(), new ArrayList<>()); // 消息记录存储
                }

                int finalOnlineUserNum = onlineUserNum;         // 不可更改的在线人数
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        MainView.getInstance().getLabUserCoumter().setText("服务器在线人数为" + finalOnlineUserNum);
                    }
                });
                break;
            case COM_CHATALL:   // 群聊，服务器会不断的发送信息回来，然后添加至聊天信息存储池中
                m = new Message();
                m.setTimer((String) gsonMap.get(TIME));
                m.setSpeaker((String) gsonMap.get(SPEAKER));
                m.setContent((String) gsonMap.get(CONTENT));
                if (chatUser.equals("[group]")) {
                    chatRecoder.add(m);
                }
                userSession.get("[group]").add(m);
                break;
            case COM_CHATWITH:  // 单聊
                String speaker = (String) gsonMap.get(SPEAKER);         // 读取发送信息的人
                String receiver = (String) gsonMap.get(RECEIVER);       // 读取接受信息的人
                String time = (String) gsonMap.get(TIME);               // 读取发送的时间
                String content = (String) gsonMap.get(CONTENT);         // 读取发送的内容

                m = new Message();
                m.setSpeaker(speaker);
                m.setContent(content);
                m.setTimer(time);

                if (thisUser.equals(receiver)) {                        // 如果这个信息就是发给我的
                    if (!chatUser.equals(speaker)) {                    // 如果左侧选的用户不是发送的人
                        for (int i = 0; i < userList.size(); i++) {     // 寻找这个人，然后设置提示信息，小喇叭
                            if (userList.get(i).getUserName().equals(speaker)) {
                                ClientUser user = userList.get(i);
                                if (!user.isNotify()) {
                                    user.setNotify(true);
                                }
                                userList.remove(i);
                                userList.add(i, user);

                                // 设置播放提示音
                                PlaySound playSound = new PlaySound(MESSAGE_NOFITY);
                                playSound.play();
                                try {
                                    TimeUnit.MILLISECONDS.sleep(1000);//毫秒
                                    playSound.stop();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                        System.out.println("标记"+speaker+"发送的信息未读");
                    }else{  // 如果发言双方都是目前窗口已经选择好的用户，那么可以直接添加信息
                        chatRecoder.add(m);
                    }
                    userSession.get(speaker).add(m);       // 去消息存储中找到发言人，然后把信息存储起来
                }else{  // 如果当前用户不是接收人，那就是发送人
                    if(chatUser.equals(receiver))       // 左侧选择的用户是接收人
                        chatRecoder.add(m);             // 直接在界面上显示
                    userSession.get(receiver).add(m);   // 找到接收人，把信息放进去
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
     * 当我们再把这个Map用gson.toJson转换成json字符串时，奇葩的事情就发生了，
     * 不再和我们最开始传进来的json字符串一致了，变成了{"id":20.0,"name":"test"}
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
