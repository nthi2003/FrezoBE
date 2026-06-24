package com.frezo.common.service;

public interface CodeSequenceService {
    String nextCode(String prefix);
    String nextCode(String prefix, int digits);
    void initSequence(String prefix, Long startValue, String description);
}
