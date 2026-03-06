package com.carolai.extractor.dto;

import java.util.concurrent.atomic.AtomicInteger;

import com.carolai.extractor.enums.Outcome;

public class OutcomeCounter {
    private final AtomicInteger inserted = new AtomicInteger();
    private final AtomicInteger updated = new AtomicInteger();
    private final AtomicInteger ignored = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();
    private final AtomicInteger apiErrors = new AtomicInteger();
    private final AtomicInteger size = new AtomicInteger();

    public void increment(Outcome outcome) {
        switch (outcome) {
            case INSERTED -> inserted.incrementAndGet();
            case UPDATED -> updated.incrementAndGet();
            case IGNORED -> ignored.incrementAndGet();
            case SKIPPED -> skipped.incrementAndGet();
            case API_ERROR -> apiErrors.incrementAndGet();
        }
    }

    public int getInserted() {
        return inserted.get();
    }

    public int getUpdated() {
        return updated.get();
    }

    public int getIgnored() {
        return ignored.get();
    }

    public int getSkipped() {
        return skipped.get();
    }

    public int getApiErrors() {
        return apiErrors.get();
    }

    public int getSize() {
        return size.get();
    }

    public void setSize(int value) {
        size.addAndGet(value);
    }
}
