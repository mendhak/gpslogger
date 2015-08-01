package com.mendhak.gpslogger.common;

import java.util.concurrent.LinkedBlockingDeque;

public class FifoDeque extends LinkedBlockingDeque<String> {

    private final int maxSize;

    public FifoDeque(int maxSize){
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(String object) {

        if(size()>maxSize){
            removeFirst();
        }

        return super.add(object);
    }
}
