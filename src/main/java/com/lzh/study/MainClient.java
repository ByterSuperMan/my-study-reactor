package com.lzh.study;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class MainClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        SocketChannel socket = SocketChannel.open(new InetSocketAddress("127.0.0.1", 8900));
        while (!socket.finishConnect()) {
        }
        System.out.println("connected ...");
        ByteBuffer bb = ByteBuffer.allocate(100);
        System.out.println();
        int i = 0;
        while (i++ < 10) {
            bb.put((i+"\r\n").getBytes(StandardCharsets.UTF_8));
            bb.flip();
            socket.write(bb);
            bb.clear();
            Thread.sleep(500);
        }
        ByteBuffer cc = ByteBuffer.allocate(1024);
        int read = socket.read(cc);
        while (read > 0) {
            cc.flip();
            System.out.println(new String(cc.array(), 0, cc.limit(), StandardCharsets.UTF_8));
            cc.clear();
            read = socket.read(cc);
        }
        socket.close();
    }
}
