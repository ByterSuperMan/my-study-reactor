package com.lzh.study;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * 字节流读取解析器
 *
 */
public interface IMyReader {

    /**
     * 将socket中的数据读入到byteBuf中
     * @param socketChannel
     *
     * @return
     */
    int read(SocketChannel socketChannel) throws IOException;

    /**
     *
     * @return
     */
    String poll();

    /**
     * 是否还有数据
     * @return
     */
    boolean isEmpty();
}
