package com.kang.utils;

import javax.media.CannotRealizeException;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.kang.utils.Constants.MESSAGE_NOFITY;
import static com.kang.utils.Constants.ONLINE_NOTIFY;

/**
 * @description: 收到消息的时候 播放声音
 * @author: HeyWeCome
 * @createDate: 2020/10/27 9:44
 * @version: 1.0
 */
public class PlaySound {
    private Player audioPlayer = null;                     // 建立一个播放接口
    private String audioPath = "E:/Workspace/IDEAWorkspace/wechat/src/main/resources/audio/";
    private String messageNotify = audioPath+"msg.wav";    // 发送消息的提示音
    private String onlineNotify = audioPath+"system.wav";  // 好友上线的提示音

    /**
     * 默认构造方法
     */
    public PlaySound(Integer type){
        try{
            if (type == MESSAGE_NOFITY){               // 如果匹配的是消息通知，那么就新建一个发送消息的提示音
                File msgNotify = new File(messageNotify);
                URL msgNotifyURL = fileToURL(msgNotify);
                audioPlayer = Manager.createRealizedPlayer(msgNotifyURL);
            } else if(type == ONLINE_NOTIFY){          // 如果匹配的是上线通知，那么就读取上线通知的提示音
                File olNotify = new File(onlineNotify);
                URL olNotifyURL = fileToURL(olNotify);
                audioPlayer = Manager.createRealizedPlayer(olNotifyURL);
            }
        }catch (IOException e) {
            e.printStackTrace();
        } catch (NoPlayerException e) {
            e.printStackTrace();
        } catch (CannotRealizeException e) {
            e.printStackTrace();
        }
        
    }

    /**
     * 创建一个准备Player,准备好播放
     * @param url
     */
    public PlaySound(URL url){
        try {
            audioPlayer = Manager.createRealizedPlayer(url);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoPlayerException e) {
            e.printStackTrace();
        } catch (CannotRealizeException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将本地文件改为URL
     * @param file
     * @throws MalformedURLException
     * @throws Exception
     */
    public URL fileToURL(File file){
        URL url = null;
        try {
            url = file.toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return url;
    }

    /**
     * 直接调用播放方法就可以
     */
    public void play(){
        audioPlayer.start();
    }

    /**
     * 停止的时候一定要释放资源
     */
    public void stop(){
        audioPlayer.stop();
        audioPlayer.close();
    }


    public static void main(String [] args){
        PlaySound playSound = new PlaySound(MESSAGE_NOFITY);
        playSound.play();
        try {
            TimeUnit.MILLISECONDS.sleep(1000);//毫秒
            playSound.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
