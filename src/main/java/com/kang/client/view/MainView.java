package com.kang.client.view;


import com.kang.client.MainApp;
import com.kang.client.emojis.EmojiDisplayer;
import com.kang.client.net.Client;
import com.kang.client.controller.ControlledStage;
import com.kang.client.controller.StageController;
import com.kang.bean.ClientUser;
import com.kang.bean.Message;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;

import static com.kang.utils.Constants.*;
import static com.kang.utils.Constants.CONTENT;

public class MainView implements ControlledStage, Initializable {

    @FXML
    public Button btnEmoji;
    @FXML
    public TextArea textSend;
    @FXML
    public Button btnSend;
    @FXML
    public ListView chatWindow;
    @FXML
    public ListView userGroup;
    @FXML
    public Label labUserName;
    @FXML
    public Label labChatTip;
    @FXML
    public Label labUserCoumter;

    private Gson gson = new Gson();
    private StageController stageController;
    private Client model;
    private static MainView instance;
    private boolean pattern = GROUP;                    // 聊天的模式，默认不是群聊
    private String seletUser = "[group]";               // 选择的聊天对象，默认是群聊，也可以改成点对点聊天
    private static String thisUser;
    private ObservableList<ClientUser> uselist;         // 用户列表
    private ObservableList<Message> chatReccder;        // 聊天信息记录

    public MainView() {
        super();
        instance = this;
    }

    public static MainView getInstance() {
        return instance;
    }

    @Override
    public void setStageController(StageController stageController) {
        this.stageController = stageController;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        model = Client.getInstance();                              // 获取到唯一的那个对象
        uselist = model.getUserList();                                  // 加载用户列表
        chatReccder = model.getChatRecoder();                           // 获取所有的聊天记录
        userGroup.setItems(uselist);
        chatWindow.setItems(chatReccder);
        thisUser = model.getThisUser();                                 // 设置当前用户
        labUserName.setText("欢迎 " + model.getThisUser() + "!");
        btnSend.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (pattern == GROUP) {
                    HashMap map = new HashMap();
                    map.put(COMMAND, COM_CHATALL);                      // 操作：群聊
                    map.put(CONTENT, textSend.getText().trim());        // 存放内容，去掉首尾的空白
                    model.sentMessage(gson.toJson(map));                // 客户端发送信息
                } else if (pattern == SINGLE) {
                    HashMap map = new HashMap();
                    map.put(COMMAND, COM_CHATWITH);                     // 操作：点对点聊天
                    map.put(RECEIVER, seletUser);                       // 存放：接收者
                    map.put(SPEAKER, model.getThisUser());              // 存放：发送者
                    map.put(CONTENT, textSend.getText().trim());        // 存放：聊天的内容
                    model.sentMessage(gson.toJson(map));                // 客户端开始发送
                }
                textSend.setText("");                                   // 清空发送框
            }
        });

        /**
         * 选择用户列表聊天
         */
        userGroup.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            ClientUser user = (ClientUser) newValue;
            System.out.println("你选择了 " + user.getUserName());
            if (user.getUserName().equals("[group]")) {
                pattern = GROUP;    // 群聊
                if (!seletUser.equals("[group]")) {
                    model.setChatUser("[group]");
                    seletUser = "[group]";
                    labChatTip.setText("Group Chat");
                }
            } else {
                pattern = SINGLE;   // 单聊
                if (!seletUser.equals(user.getUserName())) {
                    model.setChatUser(user.getUserName());              // 设置当前的聊天对象
                    seletUser = user.getUserName();                     // 获取当前对象
                    labChatTip.setText("正在和 " + seletUser+"聊天");
                }
            }
        });

        /**
         * 返回聊天的框
         */
        chatWindow.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView param) {
                return new ChatCell();
            }
        });

        /**
         * 返回用户列表
         */
        userGroup.setCellFactory(new Callback<ListView, ListCell>() {
            @Override
            public ListCell call(ListView param) {
                return new UserCell();
            }
        });
    }


    @FXML
    public void onEmojiBtnClcked() {
        stageController.loadStage(MainApp.EmojiSelectorID, MainApp.EmojiSelectorRes);
        stageController.setStage(MainApp.EmojiSelectorID);
    }

    public TextArea getMessageBoxTextArea() {
        return textSend;
    }

    public Label getLabUserCoumter() {
        return labUserCoumter;
    }

    public static class UserCell extends ListCell<ClientUser> {
        @Override
        protected void updateItem(ClientUser item, boolean empty) {
            super.updateItem(item, empty);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    if (item != null) {
                        HBox hbox = new HBox();
                        ImageView imageHead = new ImageView(new Image("image/head.png"));
                        imageHead.setFitHeight(20);
                        imageHead.setFitWidth(20);
                        ClientUser user = (ClientUser) item;
                        ImageView imageStatus;
                        if(user.getUserName().equals("[group]")){ // 在线
                            imageStatus = new ImageView(new Image("image/online.png"));
                        } else if(user.isNotify()==true){ // 有没有信息发给他的
                            imageStatus = new ImageView(new Image("image/message.png"));
                        }else {
                            if(user.getStatus().equals("online")){
                                imageStatus = new ImageView(new Image("image/online.png"));
                            }else{
                                imageStatus = new ImageView(new Image("image/offline.png"));
                            }
                        }
                        imageStatus.setFitWidth(20);
                        imageStatus.setFitHeight(20);
                        Label label = new Label(user.getUserName());
                        hbox.getChildren().addAll(imageHead, label,imageStatus);
                        setGraphic(hbox);
                    } else {
                        setGraphic(null);
                    }
                }
            });
        }
    }

    public static class ChatCell extends ListCell<Message> {
        @Override
        protected void updateItem(Message item, boolean empty) {
            super.updateItem(item, empty);
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    //inorder to avoid the
                    if (item != null) {
                        VBox box = new VBox();
                        HBox hbox = new HBox();
                        TextFlow txtContent = new TextFlow(EmojiDisplayer.createEmojiAndTextNode(item.getContent()));
                        Label labUser = new Label(item.getSpeaker() + "[" + item.getTimer() + "]");
                        labUser.setStyle("-fx-background-color: #7bc5cd; -fx-text-fill: white;");
                        ImageView image = new ImageView(new Image("image/head.png"));
                        image.setFitHeight(20);
                        image.setFitWidth(20);
                        hbox.getChildren().addAll(image, labUser);
                        if (item.getSpeaker().equals(thisUser)) {
                            txtContent.setTextAlignment(TextAlignment.RIGHT);
                            hbox.setAlignment(Pos.CENTER_RIGHT);
                            box.setAlignment(Pos.CENTER_RIGHT);
                        }
                        box.getChildren().addAll(hbox, txtContent);
                        setGraphic(box);
                    } else {
                        setGraphic(null);
                    }
                }
            });
        }
    }

}
