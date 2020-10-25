package com.kang.client.controller;

import com.kang.client.model.ClientModel;
import com.kang.client.MainApp;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


import java.net.URL;
import java.util.ResourceBundle;

public class LoginViewController implements ControlledStage, Initializable {

    @FXML
    TextField textPassword;
    @FXML
    TextField txtUsername;
    @FXML
    TextField txtHostName;
    @FXML
    Button btn_login;
    @FXML
    ImageView imageView;
    @FXML
    Button btn_signIn;

    StageController myController;
    ClientModel model;

    public LoginViewController() {
        super();
    }

    /**
     * 初始化舞台的控制类
     * @param stageController
     */
    public void setStageController(StageController stageController) {
        this.myController = stageController;
        model = ClientModel.getInstance();
    }

    /**
     * 初始化
     * @param location
     * @param resources
     */
    public void initialize(URL location, ResourceBundle resources) {

    }

    /**
     * 加载主界面
     */
    public void goToMain() {
        myController.loadStage(MainApp.mainViewID,MainApp.mainViewRes);
        myController.setStage(MainApp.mainViewID,MainApp.loginViewID);
        myController.getStage(MainApp.mainViewID).setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                model.disConnect();
                myController.unloadStage(MainApp.EmojiSelectorID);
            }
        });
    }

    /**
     * 登录聊天
     */
    public void logIn() {
        StringBuffer result = new StringBuffer();
        if (model.CheckLogin(txtUsername.getText(), txtHostName.getText(),textPassword.getText(), result, 0)) {
            goToMain();
        } else {
            showError(result.toString());
        }
    }

    /**
     * 最小化窗口
     * @param event
     */
    @FXML public void minBtnAction(ActionEvent event){
        Stage stage = myController.getStage(MainApp.loginViewID);
        stage.setIconified(true);
    }
    /**
     * 关闭窗口，关闭程序
     * @param event
     */
    @FXML public void closeBtnAction(ActionEvent event){
        Platform.exit();
        System.exit(0);
    }

    /**
     * 展示错误信息
     * @param error
     */
    public void showError(String error) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("QQ");
        alert.setContentText("登录失败 " + error);
        alert.show();
    }

    /**
     * 注册信息填写
     * @param actionEvent
     */
    public void signUp(ActionEvent actionEvent) {
        StringBuffer result = new StringBuffer();
        if (model.CheckLogin(txtUsername.getText(), txtHostName.getText(),textPassword.getText(), result, 1)) {
            goToMain();
        } else {
            showError(result.toString());
        }
    }
}