package com.lzh.study;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class MyReactor implements Runnable {

    /**
     * reactor服务端绑定端口
     */
    private int port;

    /**
     * 服务端通道
     */
    private ServerSocketChannel serverSocketChannel;

    /**
     * 服务端就绪IO通道选择器
     */
    private Selector selector;

    /**
     * 与服务端通道关联的选择键
     */
    private SelectionKey serverSK;
    /**
     * 读取解析器工厂
     */
    private IMyReaderFactory myReaderFactory;


    public MyReactor(int port, IMyReaderFactory myReaderFactory) throws IOException {
        this.port = port;
        this.myReaderFactory = myReaderFactory;
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.bind(new InetSocketAddress(this.port));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();
        //监听服务端通道，让选择器查看是否有连接事件
        this.serverSK = this.serverSocketChannel.register(this.selector, 0);
        this.serverSK.attach(new MyAcceptor());
        this.serverSK.interestOps(SelectionKey.OP_ACCEPT);
    }


    protected class MyAcceptor implements Runnable {

        @Override
        public void run() {
            //新连接
            try {
                SocketChannel socketChannel = MyReactor.this.serverSocketChannel.accept();
                if (socketChannel != null) {
                    socketChannel.configureBlocking(false);
                    //对新连接的读入事件感兴趣，并为其分配专属数据缓冲区
                    //实质上，clientSK是指该新连接的通道与选择器的关联关系
                    new MyHandler(socketChannel, MyReactor.this.selector, MyReactor.this.myReaderFactory.create());

                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void run() {
        int select;
        Set<SelectionKey> selectionKeys;
        Iterator<SelectionKey> iterator;
        SelectionKey readySK;
        while (Thread.currentThread().isAlive()) {
            try {
                select = this.selector.select();
                if (select > 0) {
                    //有就绪通道，处理
                    selectionKeys = this.selector.selectedKeys();
                    iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        readySK = iterator.next();
                        dispatch(readySK);
                    }
                    //需要进行移除，举个例子：如果发生了OP_ACCEPT，在这一次处理了这个事件，如果不移除，下一次还会得到这个事件的selectionKey，并且得到一个空的SocketChannel
                    selectionKeys.clear();
                } else {
                    //此时可以去处理一些客户端连接中没有处理完的数据
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 分发IO事件
     * @param readySK
     * @throws IOException
     */
    private void dispatch(SelectionKey readySK) throws IOException {
        if (readySK.isValid()) {
            Runnable r = (Runnable) readySK.attachment();
            r.run();
        }
    }
}
