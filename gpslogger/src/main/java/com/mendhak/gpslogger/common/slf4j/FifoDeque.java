package com.mendhak.gpslogger.common.slf4j;

import java.util.concurrent.LinkedBlockingDeque;

public class FifoDeque<T> extends LinkedBlockingDeque<T> {

    private final int maxSize;

    public FifoDeque(int maxSize){
        this.maxSize = maxSize;
    }

    @Override
    public boolean add(T object) {

        if(size()>maxSize){
            removeFirst();
        }

        return super.add(object);
    }
}
