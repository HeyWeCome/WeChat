package com.kang.Server;

public class LocalServer {

    public static void main(String[] args){
        MasterServer masterServer = new MasterServer();
        masterServer.start();
    }
}
