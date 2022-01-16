package com.lzh.study.line;

import com.lzh.study.IMyReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * \r\n 换行解析读取器
 */
public class MyLineReader implements IMyReader {
    /**
     * 以 \r\n 作为一行数据的结束标志
     */
    private static final char R = '\r';
    private static final char N = '\n';

    /**
     * 读入缓冲区
     */
    private ByteBuffer inBuf;
    /**
     * 行缓冲，处理在读数据时，没有在本次读到完整行的情况
     */
    private ByteBuffer lineBuf;
    /**
     * 是否完整行
     */
    private boolean fullLine = true;
    /**
     * 内部消息队列
     */
    private Queue<String> queue = new ArrayDeque<>(100);

    public MyLineReader() {
        this(20, 10);
    }

    public MyLineReader(int capacity, int lineCapacity) {
        this.inBuf = ByteBuffer.allocate(capacity);
        this.lineBuf = ByteBuffer.allocate(lineCapacity);
    }

    @Override
    public int read(SocketChannel socketChannel) throws IOException {
        int read = socketChannel.read(this.inBuf);
        byte b;
        if (fullLine) {
            //如果在上次读IO结束后，是完整行，就清空行缓冲区
            this.lineBuf.clear();
        }
        while (read > 0) {
            this.inBuf.flip();
            while (this.inBuf.hasRemaining()) {
                b = this.inBuf.get();
                if (this.lineBuf.hasRemaining()) {
                    //如果行缓冲区未满，继续填入
                    putIfNotFullLine(b);
                } else {
                    //如果行缓冲区满了还不足一行，那么扩充行缓冲区为原来的两倍
                    resizeLineBuf();
                    putIfNotFullLine(b);
                }
            }
            //再次从通道中读取数据
            this.inBuf.clear();
            read = socketChannel.read(this.inBuf);
        }
        return read;
    }

    /**
     *
     * @param b
     */
    private void putIfNotFullLine(byte b) {
        if (b == N) {
            if (this.lineBuf.get(this.lineBuf.position() - 1) == R) {
                // \r\n，读取到了完整的一行，存入内部消息队列中
                this.fullLine = true;
                this.lineBuf.flip();
                String line = new String(this.lineBuf.array(), 0, this.lineBuf.limit(), StandardCharsets.UTF_8);
                this.queue.offer(line);
                this.lineBuf.clear();
            } else {
                this.fullLine = false;
                this.lineBuf.put(b);
            }
        } else {
            this.fullLine = false;
            this.lineBuf.put(b);
        }
    }

    /**
     * 扩容行缓冲区
     */
    private void resizeLineBuf() {
        ByteBuffer buf = ByteBuffer.allocate(this.lineBuf.capacity() * 2);
        this.lineBuf.flip();
        buf.put(this.lineBuf);
        this.lineBuf.clear();
        this.lineBuf = buf;
    }

    @Override
    public String poll() {
        return this.queue.poll();
    }

    @Override
    public boolean isEmpty() {
        return this.queue.isEmpty();
    }
}
