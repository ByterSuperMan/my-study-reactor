package com.lzh.study;

import com.lzh.study.line.MyLineReaderFactory;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        MyReactor reactor = new MyReactor(8900, new MyLineReaderFactory());
        Thread t = new Thread(reactor, "myReactor");
        t.start();
    }
}
