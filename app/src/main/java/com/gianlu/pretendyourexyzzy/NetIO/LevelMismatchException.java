package com.gianlu.pretendyourexyzzy.NetIO;

public class LevelMismatchException extends Exception {
    LevelMismatchException(InstanceHolder.Level current, InstanceHolder.Level requested) {
        super("Requested level " + requested.importance + " while current is " + current.importance);
    }
}
