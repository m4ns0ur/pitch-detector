package com.audio.demo.fft;

import android.media.MediaRecorder;

public class Setting {

    public final int RECORDER_AGC_OFF = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    public final int BYTE_OF_SAMPLE = 2;

    public int audioSourceId = RECORDER_AGC_OFF;
    public int sampleRate = 16000;
    public int fftLen = 2048;
    public int hopLen = 1024;
    public int nFFTAverage = 2;
    public double overlapPercent = 50;
    public boolean isAWeighting = false;

    public Setting() {
    }

}
