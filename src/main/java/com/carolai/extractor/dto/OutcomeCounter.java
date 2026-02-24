package com.carolai.extractor.dto;

import com.carolai.extractor.enums.Outcome;

public class OutcomeCounter {
    private int inserted;
    private int updated;
    private int ignored;
    private int skipped;
    private int size;

    public void increment(Outcome outcome) {
        switch (outcome) {
            case INSERTED -> inserted++;
            case UPDATED -> updated++;
            case IGNORED -> ignored++;
            case SKIPPED -> skipped++;
        }
    }

    public int getInserted() {
        return inserted;
    }

    public int getUpdated() {
        return updated;
    }

    public int getIgnored() {
        return ignored;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size += size;
    }

}