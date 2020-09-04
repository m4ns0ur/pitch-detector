package com.audio.demo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.audio.demo.util.AudioUtil;
import com.audio.demo.util.NumberUtil;

import github.bewantbe.audio_analyzer_for_android.R;

public class MainView {

    private MainActivity activity;
    private StringBuilder textPeak = new StringBuilder();
    private char[] textPeakChar;
    private float dpRatio;
    private float listItemTextSize;
    private float listItemTitleTextSize;
    private long lastTimeNotifyOverrun = 0;
    private long timeToUpdate = SystemClock.uptimeMillis();

    private volatile boolean isInvalidating = false;

    PopupWindow popupMenuSampleRate;
    PopupWindow popupMenuFftLen;
    PopupWindow popupMenuAverage;

    public MainView(MainActivity activity) {
        this.activity = activity;

        final Resources res = this.activity.getResources();
        listItemTextSize = res.getDimension(R.dimen.button_text_font_size);
        listItemTitleTextSize = res.getDimension(R.dimen.button_text_small_font_size);
        dpRatio = res.getDisplayMetrics().density;
        textPeakChar = new char[res.getString(R.string.text_view_peak_text).length()];
        popupMenuSampleRate = popupMenuCreate(AudioUtil.validateAudioRates(res.getStringArray(R.array.sample_rates)), R.id.button_sample_rate);
        popupMenuFftLen = popupMenuCreate(res.getStringArray(R.array.fft_len), R.id.button_fftlen);
        popupMenuAverage = popupMenuCreate(res.getStringArray(R.array.fft_ave_num), R.id.button_average);
    }

    public void update() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                invalidateView();
            }
        });
    }

    public void notifyToast(final String text) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Context context = activity.getApplicationContext();
                final Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    public void notifyOverrun() {
        final long t = SystemClock.uptimeMillis();
        if (t - lastTimeNotifyOverrun > 6000) {
            lastTimeNotifyOverrun = t;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final Context context = activity.getApplicationContext();
                    final String text = "Recorder buffer overrun!\nYour cell phone is too slow.\nTry lower sampling rate or higher average number.";
                    final Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                    toast.show();
                }
            });
        }
    }

    public void showPermissionExplanation(int resId) {
        final TextView tv = new TextView(activity);
        tv.setMovementMethod(new ScrollingMovementMethod());
        tv.setText(fromHtml(activity.getString(resId)));
        new AlertDialog.Builder(activity)
                .setTitle(R.string.permission_explanation_title)
                .setView(tv)
                .setNegativeButton(R.string.dismiss, null)
                .create()
                .show();
    }

    @SuppressWarnings("deprecation")
    public static android.text.Spanned fromHtml(String source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(source, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(source);
        }
    }

    public void showPopupMenu(android.view.View view) {
        final int[] wl = new int[2];
        view.getLocationInWindow(wl);

        final int xLeft = wl[0];
        final int yBottom = activity.getWindowManager().getDefaultDisplay().getHeight() - wl[1];
        final int gravity = android.view.Gravity.START | android.view.Gravity.BOTTOM;

        switch (view.getId()) {
            case R.id.button_sample_rate:
                popupMenuSampleRate.showAtLocation(view, gravity, xLeft, yBottom);
                break;
            case R.id.button_fftlen:
                popupMenuFftLen.showAtLocation(view, gravity, xLeft, yBottom);
                break;
            case R.id.button_average:
                popupMenuAverage.showAtLocation(view, gravity, xLeft, yBottom);
                break;
        }
    }

    private PopupWindow popupMenuCreate(String[] popUpContents, int resId) {
        final ListView lv = new ListView(activity);
        final ArrayAdapter<String> aa = popupMenuAdapter(popUpContents);
        lv.setAdapter(aa);
        lv.setOnItemClickListener(activity);
        lv.setTag(resId);

        final Paint paint = new Paint();
        paint.setTextSize(listItemTextSize);

        float w = 0;
        float wi;

        for (String popUpContent : popUpContents) {
            final String[] sts = popUpContent.split("::");
            if (sts.length == 0) {
                continue;
            }

            final String st = sts[0];
            if (sts.length == 2 && sts[1].equals("0")) {
                paint.setTextSize(listItemTitleTextSize);
                wi = paint.measureText(st);
                paint.setTextSize(listItemTextSize);
            } else {
                wi = paint.measureText(st);
            }

            if (w < wi) {
                w = wi;
            }
        }

        w = w + 23 * dpRatio;
        if (w < 40 * dpRatio) {
            w = 40 * dpRatio;
        }

        final PopupWindow popupWindow = new PopupWindow(activity);
        popupWindow.setFocusable(true);
        popupWindow.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
        popupWindow.setWidth((int) w);
        popupWindow.setContentView(lv);

        ((Button) activity.findViewById(resId)).setWidth((int) (w + 5 * dpRatio));

        return popupWindow;
    }

    private ArrayAdapter<String> popupMenuAdapter(String[] itemTagArray) {
        return new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1, itemTagArray) {
            @NonNull
            @Override
            public android.view.View getView(int position, android.view.View convertView, @NonNull ViewGroup parent) {
                final String item = getItem(position);
                assert item != null;
                final String[] items = item.split("::");
                final String text = items[0];
                final String id = items[1];

                final TextView tv = new TextView(activity);
                tv.setText(text);
                tv.setTag(id);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setPadding(5, 5, 5, 5);

                if (id.equals("0")) {
                    tv.setTextSize(listItemTitleTextSize / dpRatio);
                    tv.setTextColor(Color.GREEN);
                } else {
                    tv.setTextSize(listItemTextSize / dpRatio);
                    tv.setTextColor(Color.WHITE);
                }

                return tv;
            }
        };
    }

    private void refreshPeakLabel(double maxAmpFreq, double maxAmpDB) {
        textPeak.setLength(0);
        textPeak.append(activity.getString(R.string.text_peak));
        NumberUtil.fillInNumFixedWidthPositive(textPeak, maxAmpFreq, 5, 1);
        textPeak.append("Hz(");
        AudioUtil.freq2Cent(textPeak, maxAmpFreq, " ");
        textPeak.append(") ");
        NumberUtil.fillInNumFixedWidth(textPeak, maxAmpDB, 3, 1);
        textPeak.append("dB");
        textPeak.getChars(0, Math.min(textPeak.length(), textPeakChar.length), textPeakChar, 0);

        final TextView tv = activity.findViewById(R.id.textview_peak);
        tv.setText(textPeakChar, 0, textPeakChar.length);
        tv.invalidate();
    }

    public void invalidateView() {
        if (isInvalidating) {
            return;
        }

        isInvalidating = true;
        final long frameTime = 1000 / 60;
        final long t = SystemClock.uptimeMillis();

        if (t >= timeToUpdate) {
            timeToUpdate += frameTime;
            if (timeToUpdate < t) {
                timeToUpdate = t + frameTime;
            }
            refreshPeakLabel(activity.maxAmpFreq, activity.maxAmpDB);
        }

        isInvalidating = false;
    }

}
