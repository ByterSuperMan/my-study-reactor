package com.lzh.study;

/**
 * 字节流读取解析器工厂
 */
public interface IMyReaderFactory {
    /**
     * 创建一个新的字节流读取解析器
     * @return
     */
    IMyReader create();
}
