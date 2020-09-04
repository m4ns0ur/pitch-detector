package com.audio.demo;

import android.os.SystemClock;

public class Recorder {

    private long timeUpdateOld;
    private long timeUpdateInterval;
    private long timeStarted;
    private long lastOverrunTime;
    private long samplesReadNum;
    private int sampleRate;
    private int bufferSampleSize;
    private double sampleRateReal;
    private boolean lastCheckOverrun = false;

    public Recorder(int sampleRate, int bufferSampleSize) {
        this.sampleRate = sampleRate;
        this.bufferSampleSize = bufferSampleSize;
        this.timeUpdateInterval = 2000;
    }

    public void start() {
        samplesReadNum = 0;
        lastOverrunTime = 0;
        timeStarted = SystemClock.uptimeMillis();
        timeUpdateOld = timeStarted;
        sampleRateReal = sampleRate;
    }

    public boolean updateState(int numOfReadShort) {
        final long now = SystemClock.uptimeMillis();
        if (samplesReadNum == 0) {
            timeStarted = now - numOfReadShort * 1000 / sampleRate;
        }
        samplesReadNum += numOfReadShort;

        if (timeUpdateOld + timeUpdateInterval > now) {
            return false;
        }
        timeUpdateOld += timeUpdateInterval;
        if (timeUpdateOld + timeUpdateInterval <= now) {
            timeUpdateOld = now;
        }

        final long samplesFromTimeNum = (long) ((now - timeStarted) * sampleRateReal / 1000);
        if (samplesFromTimeNum > bufferSampleSize + samplesReadNum) {
            lastOverrunTime = now;
            samplesReadNum = 0;
        }

        if (samplesReadNum > 10 * sampleRate) {
            sampleRateReal = 0.9 * sampleRateReal + 0.1 * (samplesReadNum * 1000.0 / (now - timeStarted));
            if (Math.abs(sampleRateReal - sampleRate) > 0.0145 * sampleRate) {
                samplesReadNum = 0;
            }
        }

        lastCheckOverrun = lastOverrunTime == now;
        return true;
    }

    public boolean getLastCheckOverrun() {
        return lastCheckOverrun;
    }

}
