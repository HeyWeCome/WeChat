package com.kang.Server;

public class LocalServer {

    /**
     * 使用了比较简单的worker-master架构。
     * 由masterserve进行事件的分发
     * 由workserver具体的管理单个用户的消息请求
     *
     * Master-Worker模式是常用的并行模式之一，
     * 它的核心思想是：系统由两类进程协同工作，即Master进程和Worker进程，
     * Master负责接收和分配任务，Wroker负责处理子任务。
     * 当各个Worker进程将子任务处理完成后，将结果返回给Master进程，由Master进程进行汇总，从而得到最终的结果
     *
     * Master-Worker 模式的好处，它能够将一个大任务分解成若干个小任务并行执行，从而提高系统的吞吐量。
     * 而对于系统请求者 Client 来说，任务一旦提交，Master进程会分配任务并立即返回，并不会等待系统全部处理完成后再返回，其处理过程是异步的。
     * 因此，Client 不会出现等待现象。
     * @param args
     */
    public static void main(String[] args){
        MasterServer masterServer = new MasterServer();
        masterServer.start();
    }
}
