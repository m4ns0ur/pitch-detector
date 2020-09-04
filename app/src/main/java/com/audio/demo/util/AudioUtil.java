package com.audio.demo.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.util.ArrayList;

public class AudioUtil {

    private static final String[] LP = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    public static void freq2Cent(StringBuilder sb, double f, String sFill) {
        if (f <= 0 || Double.isNaN(f) || Double.isInfinite(f)) {
            sb.append("      ");
            return;
        }

        final int l = sb.length();
        final double p = freq2pitch(f);
        pitch2Note(sb, p, 0, false);

        while (sb.length() - l < 6 && sFill != null && sFill.length() > 0) {
            sb.append(sFill);
        }
    }

    public static String[] validateAudioRates(String[] requested) {
        final ArrayList<String> validated = new ArrayList<>();
        for (String s : requested) {
            final int rate;
            final String[] sv = s.split("::");
            if (sv.length == 1) {
                rate = Integer.parseInt(sv[0]);
            } else {
                rate = Integer.parseInt(sv[1]);
            }

            if (rate != 0) {
                final int recBufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (recBufferSize != AudioRecord.ERROR_BAD_VALUE) {
                    if (rate > 48000) {
                        final AudioRecord record;
                        try {
                            record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, recBufferSize);
                        } catch (IllegalArgumentException e) {
                            continue;
                        }

                        if (record.getState() == AudioRecord.STATE_INITIALIZED) {
                            validated.add(s);
                        }
                    } else {
                        validated.add(s);
                    }
                }
            } else {
                validated.add(s);
            }
        }

        return validated.toArray(new String[0]);
    }

    private static double freq2pitch(double f) {
        return 69 + 12 * Math.log(f / 440.0) / Math.log(2);
    }

    private static void pitch2Note(StringBuilder a, double p, int precFrac, boolean tightMode) {
        final int pi = (int) Math.round(p);
        final int po = (int) Math.floor(pi / 12.0);
        final int pm = pi - po * 12;
        final double cent = Math.round(100 * (p - pi) * Math.pow(10, precFrac)) * Math.pow(10, -precFrac);

        if (Math.abs(cent) > 15) {
            return;
        }

        a.append(LP[pm]);
        NumberUtil.fillInInt(a, po - 1);

        if (LP[pm].length() == 1 && !tightMode) {
            a.append(' ');
        }

        if (!tightMode) {
            NumberUtil.fillInNumFixedWidthSignedFirst(a, cent, 2, precFrac);
        } else {
            if (cent != 0) {
                a.append(cent < 0 ? '-' : '+');
                NumberUtil.fillInNumFixedWidthPositive(a, Math.abs(cent), 2, precFrac, '\0');
            }
        }
    }

}
