package com.lzh.study.line;

import com.lzh.study.IMyReader;
import com.lzh.study.IMyReaderFactory;

public class MyLineReaderFactory implements IMyReaderFactory {

    @Override
    public IMyReader create() {
        return new MyLineReader();
    }
}
