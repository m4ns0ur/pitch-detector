package com.audio.demo.fft;

import java.util.Arrays;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.log10;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;

// Short Time Fourier Transform
public class Stft {

    public double maxAmpFreq = Double.NaN;
    public double maxAmpDb = Double.NaN;

    private RealDoubleFFT ampFft;
    private double[] ampOutCum;
    private double[] ampOutTmp;
    private double[] ampOut;
    private double[] ampOutDb;
    private double[] ampIn;
    private double[] ampInTmp;
    private double[] window;
    private double[] dBaFactor;
    private int sampleRate;
    private int fftLen;
    private int hopLen;
    private int ampPt;
    private int analysedNum = 0;
    private boolean aWeighting = false;

    public Stft(Setting setting) {
        init(setting.fftLen, setting.hopLen, setting.sampleRate, setting.nFFTAverage);
    }

    public void setAWeighting(boolean aWeighting) {
        this.aWeighting = aWeighting;
    }

    public int getAnalysedNum() {
        return this.analysedNum;
    }

    public void calculatePeak() {
        getAmpDb();

        maxAmpDb = 20 * log10(0.125 / 32768);
        maxAmpFreq = 0;
        for (int i = 1; i < ampOutDb.length; i++) {
            if (ampOutDb[i] > maxAmpDb) {
                maxAmpDb = ampOutDb[i];
                maxAmpFreq = i;
            }
        }
        maxAmpFreq = maxAmpFreq * sampleRate / fftLen;

        if (sampleRate / fftLen < maxAmpFreq && maxAmpFreq < sampleRate / 2 - sampleRate / fftLen) {
            final int id = (int) (round(maxAmpFreq / sampleRate * fftLen));
            final double x1 = ampOutDb[id - 1];
            final double x2 = ampOutDb[id];
            final double x3 = ampOutDb[id + 1];
            final double a = (x3 + x1) / 2 - x2;
            final double b = (x3 - x1) / 2;
            if (a < 0) {
                final double peak = -b / (2 * a);
                if (abs(peak) < 1) {
                    maxAmpFreq += peak * sampleRate / fftLen;
                    maxAmpDb = (4 * a * x2 - b * b) / (4 * a);
                }
            }
        }
    }

    public void feedData(short[] ds, int dsLen) {
        if (dsLen > ds.length) {
            dsLen = ds.length;
        }

        final int inLen = ampIn.length;
        final int outLen = ampOut.length;
        int dsPt = 0;

        while (dsPt < dsLen) {
            while (ampPt < 0 && dsPt < dsLen) {
                dsPt++;
                ampPt++;
            }

            while (ampPt < inLen && dsPt < dsLen) {
                double s = ds[dsPt++] / 32768.0;
                ampIn[ampPt++] = s;
            }

            if (ampPt == inLen) {
                for (int i = 0; i < inLen; i++) {
                    ampInTmp[i] = ampIn[i] * window[i];
                }

                ampFft.ft(ampInTmp);
                fftToAmp(ampOutTmp, ampInTmp);

                for (int i = 0; i < outLen; i++) {
                    ampOutCum[i] += ampOutTmp[i];
                }

                analysedNum++;

                if (hopLen < fftLen) {
                    System.arraycopy(ampIn, hopLen, ampIn, 0, fftLen - hopLen);
                }

                ampPt = fftLen - hopLen;
            }
        }
    }

    private double sqr(double x) {
        return x * x;
    }

    private void init(int fftLen, int hopLen, int sampleRate, int minFeedSize) {
        if (minFeedSize <= 0) {
            throw new IllegalArgumentException("Min feed size should be >= 1");
        }

        if (((-fftLen) & fftLen) != fftLen) {
            throw new IllegalArgumentException("Only power of 2 are supported in fftLen");
        }

        this.sampleRate = sampleRate;
        this.fftLen = fftLen;
        this.hopLen = hopLen;

        ampOutCum = new double[fftLen / 2 + 1];
        ampOutTmp = new double[fftLen / 2 + 1];
        ampOut = new double[fftLen / 2 + 1];
        ampOutDb = new double[fftLen / 2 + 1];
        ampIn = new double[fftLen];
        ampInTmp = new double[fftLen];
        ampFft = new RealDoubleFFT(ampIn.length);

        initWindowFunction(fftLen);
        initDbaFactor(fftLen, sampleRate);
        clear();
        aWeighting = false;
    }

    private void initWindowFunction(int fftlen) {
        window = new double[fftlen];
        for (int i = 0; i < window.length; i++) {
            window[i] = 0.5 * (1 - cos(2 * PI * i / (window.length - 1.))) * 2;
        }

        double normalizeFactor = 0;
        for (double v : window) {
            normalizeFactor += v;
        }
        normalizeFactor = window.length / normalizeFactor;

        for (int i = 0; i < window.length; i++) {
            window[i] *= normalizeFactor;
        }
    }

    private void initDbaFactor(int fftlen, double sampleRate) {
        dBaFactor = new double[fftlen / 2 + 1];
        for (int i = 0; i < fftlen / 2 + 1; i++) {
            final double f = (double) i / fftlen * sampleRate;
            final double r = sqr(12200) * sqr(sqr(f)) / ((f * f + sqr(20.6)) * sqrt((f * f + sqr(107.7)) * (f * f + sqr(737.9))) * (f * f + sqr(12200)));
            dBaFactor[i] = r * r * 1.58489319246111;
        }
    }

    private void clear() {
        ampPt = 0;
        Arrays.fill(ampOut, 0.0);
        Arrays.fill(ampOutDb, log10(0));
        Arrays.fill(ampOutCum, 0.0);
    }

    private void fftToAmp(double[] dataOut, double[] data) {
        final double scale = 2.0 * 2.0 / (data.length * data.length);
        dataOut[0] = data[0] * data[0] * scale / 4.0;
        int j = 1;

        for (int i = 1; i < data.length - 1; i += 2, j++) {
            dataOut[j] = (data[i] * data[i] + data[i + 1] * data[i + 1]) * scale;
        }

        dataOut[j] = data[data.length - 1] * data[data.length - 1] * scale / 4.0;
    }

    private double[] getAmp() {
        if (analysedNum != 0) {
            final int outLen = ampOut.length;
            final double[] aoc = ampOutCum;
            for (int j = 0; j < outLen; j++) {
                aoc[j] /= analysedNum;
            }

            if (aWeighting) {
                for (int j = 0; j < outLen; j++) {
                    aoc[j] *= dBaFactor[j];
                }
            }

            System.arraycopy(aoc, 0, ampOut, 0, outLen);
            Arrays.fill(aoc, 0.0);

            analysedNum = 0;
            for (int i = 0; i < outLen; i++) {
                ampOutDb[i] = 10.0 * log10(ampOut[i]);
            }
        }
        return ampOut;
    }

    private double[] getAmpDb() {
        getAmp();
        return ampOutDb;
    }

}
