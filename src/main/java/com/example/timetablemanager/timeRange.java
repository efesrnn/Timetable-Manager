package com.example.timetablemanager;

import java.time.LocalTime;

public class timeRange {
    private LocalTime start;
    private LocalTime end;

    public timeRange(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public boolean overlapsWith(timeRange other) {
        return !this.end.isBefore(other.start) && !this.start.isAfter(other.end);
    }

    // Getters
    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }
}
