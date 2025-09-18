package com.ass1.server;

import java.io.Serializable;

public class Result implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int value;
    private final long waitMs;
    private final long execMs;

    public Result(int value, long waitMs, long execMs) {
        this.value = value;
        this.waitMs = waitMs;
        this.execMs = execMs;
    }

    public int getValue() { return value; }
    public long getWaitMs() { return waitMs; }
    public long getExecMs() { return execMs; }
}

