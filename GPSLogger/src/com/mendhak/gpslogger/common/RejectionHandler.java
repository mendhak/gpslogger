package com.mendhak.gpslogger.common;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class RejectionHandler implements RejectedExecutionHandler
{

    @Override
    public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor)
    {
        Utilities.LogWarning("Could not write to GPX file, there were too many queued tasks.");
    }
}

