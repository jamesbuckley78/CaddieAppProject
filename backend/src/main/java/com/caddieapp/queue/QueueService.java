package com.caddieapp.queue;

import org.springframework.stereotype.Service;

/**
 * The only place list-ordering rules live. Each rule in docs/RULES.md gets a method here
 * and a test in QueueServiceTest.
 *
 * Empty for now: the ordering rules are waiting on answers from the caddie master
 * (docs/RULES.md, "Waiting on the caddie master").
 */
@Service
public class QueueService {

    /** Position for a caddie who has just pressed "I'm here": the end of today's list. */
    public int positionForNewCheckIn(int currentListSize) {
        return currentListSize + 1;
    }
}
