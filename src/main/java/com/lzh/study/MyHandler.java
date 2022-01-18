package com.lzh.study;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 为了实现全双工通信，需要有两个缓冲区
 */
public class MyHandler implements Runnable {

    private static final int READING = 0;

    private static final int SENDING = 1;

    /**
     * 客户端连接通道
     */
    private SocketChannel socketChannel;
    /**
     *
     */
    private SelectionKey clientSK;
    /**
     * 数据读入解析器
     */
    private IMyReader myReader;
    /**
     * 写出缓冲区
     */
    private ByteBuffer outBuf;
    /**
     * 初始状态为读入IO数据
     */
    private int state = READING;

    public MyHandler(SocketChannel socketChannel, Selector selector, IMyReader myReader) throws ClosedChannelException {
        this.socketChannel = socketChannel;
        this.outBuf = ByteBuffer.allocate(1024);
        this.myReader = myReader;
        //连接完成后，对该通道的"读IO事件"感兴趣，将其交给selector来观察监听通道就绪状态
        this.clientSK = socketChannel.register(selector, 0);
        this.clientSK.attach(this);
        this.clientSK.interestOps(SelectionKey.OP_READ);
    }

    /**
     * 读入数据
     * @return 读到的字节数
     * @throws IOException
     */
    public int read() throws IOException {
        return this.myReader.read(this.socketChannel);
    }

    /**
     * 写出数据
     * @param msg
     * @return
     */
    public int write(String msg) {
        if (msg == null || msg.length() == 0) {
            return 0;
        }
        byte[] bytes = msg.getBytes(StandardCharsets.UTF_8);
        this.outBuf.put(bytes);
        return bytes.length;
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public int flush() throws IOException {
        this.outBuf.flip();
        int write = this.socketChannel.write(this.outBuf);
        this.outBuf.clear();
        return write;
    }

    /**
     *
     * @return
     */
    public boolean isEmpty() {
        return this.myReader.isEmpty();
    }

    /**
     * 获取消息
     * @return
     */
    public List<String> getMessage() {
        if (this.myReader.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        while (!this.myReader.isEmpty()) {
            list.add(this.myReader.poll());
        }
        return list;
    }

    @Override
    public void run() {
        if (state == READING) {
            //TCP缓冲区中有数据可读，即，发生了可读事件
            //将数据从通道读入该通道的专属缓冲区
            int read;
            try {
                read = read();
            } catch (IOException e) {
                //客户端异常关闭
                try {
                    System.out.println("exception close from " + this.socketChannel.getRemoteAddress());
                    this.clientSK.attach(null);
                    this.clientSK.cancel();
                    this.socketChannel.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                return;
            }
            if (!isEmpty()) {
                //其实应该将消息的处理交给其他线程，其他线程处理完毕后再将selector对该通道的感兴趣事件改为写IO
                List<String> msgs = getMessage();
                //处理，返回响应
                for (String msg : msgs) {
                    write("server echo " + msg + "\r\n");
                }
                this.state = SENDING;
                this.clientSK.interestOps(SelectionKey.OP_WRITE);
            }
            if (read == -1) {
                try {
                    System.out.println("normal close from " + this.socketChannel.getRemoteAddress());
                    this.clientSK.attach(null);
                    this.clientSK.cancel();
                    this.socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (this.state == SENDING) {
            int flush;
            try {
                flush = flush();
                System.out.println("flush " + flush +" bytes to :" + this.socketChannel.getRemoteAddress());
                this.state = READING;
                this.clientSK.interestOps(SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
