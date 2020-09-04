package com.audio.demo;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.SystemClock;
import android.widget.ToggleButton;

import com.audio.demo.fft.Setting;
import com.audio.demo.fft.Stft;

import github.bewantbe.audio_analyzer_for_android.R;

public class Sampler extends Thread {

    private volatile boolean isRunning = true;
    private volatile boolean isPaused;
    private Stft stft;
    private final Setting setting;
    private final MainActivity activity;

    public Sampler(MainActivity activity, Setting setting) {
        this.activity = activity;
        this.setting = setting;

        isPaused = !((ToggleButton) this.activity.findViewById(R.id.run)).isChecked();
    }

    @Override
    public void run() {
        final AudioRecord record;

        final long startTime = SystemClock.uptimeMillis();
        final long endTime = SystemClock.uptimeMillis();
        if (endTime - startTime < 500) {
            SleepWithoutInterrupt(500 - (endTime - startTime));
        }

        final int minBytes = AudioRecord.getMinBufferSize(setting.sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBytes == AudioRecord.ERROR_BAD_VALUE) {
            return;
        }

        int readChunkSize = setting.hopLen;
        readChunkSize = Math.min(readChunkSize, 2048);

        int bufferSampleSize = Math.max(minBytes / setting.BYTE_OF_SAMPLE, setting.fftLen / 2) * 2;
        bufferSampleSize = (int) Math.ceil(1.0 * setting.sampleRate / bufferSampleSize) * bufferSampleSize;

        try {
            if (setting.audioSourceId < 1000) {
                record = new AudioRecord(setting.audioSourceId, setting.sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, setting.BYTE_OF_SAMPLE * bufferSampleSize);
            } else {
                record = new AudioRecord(setting.RECORDER_AGC_OFF, setting.sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, setting.BYTE_OF_SAMPLE * bufferSampleSize);
            }
        } catch (IllegalArgumentException e) {
            activity.mainView.notifyToast("Illegal recorder argument, change audio source");
            return;
        }

        setting.sampleRate = record.getSampleRate();

        if (record.getState() == AudioRecord.STATE_UNINITIALIZED) {
            activity.mainView.notifyToast("Fail to initialize recorder");
            return;
        }

        final short[] audioSamples = new short[readChunkSize];
        int readShortNum;

        stft = new Stft(setting);
        stft.setAWeighting(setting.isAWeighting);

        final Recorder recorder = new Recorder(setting.sampleRate, bufferSampleSize);
        recorder.start();

        try {
            record.startRecording();
        } catch (IllegalStateException e) {
            activity.mainView.notifyToast("Fail to start recording");
            return;
        }

        while (isRunning) {
            readShortNum = record.read(audioSamples, 0, readChunkSize);
            if (recorder.updateState(readShortNum)) {
                if (recorder.getLastCheckOverrun())
                    activity.mainView.notifyOverrun();
            }

            if (isPaused) {
                continue;
            }

            stft.feedData(audioSamples, readShortNum);
            if (stft.getAnalysedNum() >= setting.nFFTAverage) {
                activity.mainView.update();
                stft.calculatePeak();
                activity.maxAmpFreq = stft.maxAmpFreq;
                activity.maxAmpDB = stft.maxAmpDb;
            }
        }

        record.stop();
        record.release();
    }

    private void SleepWithoutInterrupt(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setAWeighting(boolean isAWeighting) {
        if (stft != null) {
            stft.setAWeighting(isAWeighting);
        }
    }

    public void setPause(boolean pause) {
        this.isPaused = pause;
    }

    public boolean getPause() {
        return this.isPaused;
    }

    public void finish() {
        isRunning = false;
        interrupt();
    }

}
