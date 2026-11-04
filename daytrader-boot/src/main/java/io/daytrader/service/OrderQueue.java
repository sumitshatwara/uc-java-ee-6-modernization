package io.daytrader.service;

public interface OrderQueue {
    void submit(Integer orderID, boolean twoPhase);
}
