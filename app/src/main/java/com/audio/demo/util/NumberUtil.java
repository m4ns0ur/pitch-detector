package com.audio.demo.util;

public class NumberUtil {

    private static final char[] charDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    public static void fillInNumFixedWidthPositive(StringBuilder sb, double d, int n, int fraction, char padChar) {
        if (d < 0) {
            for (int i = 0; i < n + fraction + (fraction > 0 ? 1 : 0); i++) {
                sb.append(padChar);
            }
            return;
        }

        if (d >= Math.pow(10, n)) {
            sb.append("OFL");
            for (int i = 3; i < n + fraction + (fraction > 0 ? 1 : 0); i++) {
                sb.append(' ');
            }
            return;
        }

        if (Double.isNaN(d)) {
            sb.append("NaN");
            for (int i = 3; i < n + fraction + (fraction > 0 ? 1 : 0); i++) {
                sb.append(' ');
            }
            return;
        }

        while (n > 0) {
            n--;
            if (d < Math.pow(10, n) && n > 0) {
                if (padChar != '\0') {
                    sb.append(padChar);
                }
            } else {
                sb.append(charDigits[(int) (d / Math.pow(10, n) % 10.0)]);
            }
        }

        if (fraction > 0) {
            sb.append('.');
            for (int i = 1; i <= fraction; i++) {
                sb.append(charDigits[(int) (d * Math.pow(10, i) % 10.0)]);
            }
        }
    }

    public static void fillInNumFixedWidthPositive(StringBuilder sb, double d, int n, int fraction) {
        fillInNumFixedWidthPositive(sb, d, n, fraction, ' ');
    }

    public static void fillInNumFixedWidth(StringBuilder sb, double d, int n, int fraction) {
        final int l = sb.length();
        sb.append(' ');

        if (d < 0) {
            fillInNumFixedWidthPositive(sb, -d, n, fraction);
            for (int i = l; i < sb.length(); i++) {
                if (sb.charAt(i + 1) != ' ') {
                    sb.setCharAt(i, '-');
                    return;
                }
            }
        }

        fillInNumFixedWidthPositive(sb, d, n, fraction);
    }

    public static void fillInNumFixedWidthSignedFirst(StringBuilder sb, double d, int n, int fraction) {
        if (d < 0) {
            sb.append('-');
        } else {
            sb.append('+');
        }

        fillInNumFixedWidthPositive(sb, Math.abs(d), n, fraction);
    }

    public static void fillInInt(StringBuilder sb, int in) {
        if (in == 0) {
            sb.append('0');
            return;
        }

        if (in < 0) {
            sb.append('-');
            in = -in;
        }

        final int l = sb.length();

        while (in > 0) {
            sb.insert(l, in % 10);
            in /= 10;
        }
    }

}
