package com.audio.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.audio.demo.fft.Setting;

import java.util.Locale;

import github.bewantbe.audio_analyzer_for_android.R;

public class MainActivity extends Activity implements OnItemClickListener {

    private static final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private Sampler samplingThread = null;
    private Setting analyzerParam = null;
    private int permissionExplanationCount = 0;
    private int permissionRequestCount = 0;
    private boolean samplerPrepared = false;

    MainView mainView;
    double maxAmpDB;
    double maxAmpFreq;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        analyzerParam = new Setting();

        loadPreferenceForView();
        mainView = new MainView(this);

        final ToggleButton runButton = findViewById(R.id.run);
        runButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (samplingThread != null && samplingThread.getPause() == isChecked) {
                    samplingThread.setPause(!isChecked);
                    mainView.invalidateView();
                }
            }
        });
        runButton.toggle();

        final ToggleButton dbAButton = findViewById(R.id.dbA);
        dbAButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                analyzerParam.isAWeighting = isChecked;
                if (samplingThread != null) {
                    samplingThread.setAWeighting(analyzerParam.isAWeighting);
                }
                final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                final SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("dbA", analyzerParam.isAWeighting);
                editor.apply();
                resetSampler(analyzerParam);
                mainView.invalidateView();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        LoadPreferences();
        samplerPrepared = true;
        resetSampler(analyzerParam);
    }

    @Override
    protected void onPause() {
        samplerPrepared = false;
        if (samplingThread != null) {
            samplingThread.finish();
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putDouble("maxAmpDB", maxAmpDB);
        savedInstanceState.putDouble("maxAmpFreq", maxAmpFreq);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        maxAmpDB = savedInstanceState.getDouble("maxAmpDB");
        maxAmpFreq = savedInstanceState.getDouble("maxAmpFreq");
    }

    @Override
    public void onItemClick(AdapterView<?> parent, android.view.View view, int position, long id) {
        final String selectedItemTag = view.getTag().toString();
        if (selectedItemTag.equals("0")) {
            return;
        }

        final int buttonId = Integer.parseInt((parent.getTag().toString()));
        final Button buttonView = findViewById(buttonId);
        final String selectedItemText = ((TextView) view).getText().toString();
        buttonView.setText(selectedItemText);

        final boolean shouldResetSampler;
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final SharedPreferences.Editor editor = sharedPref.edit();

        switch (buttonId) {
            case R.id.button_sample_rate:
                mainView.popupMenuSampleRate.dismiss();
                analyzerParam.sampleRate = Integer.parseInt(selectedItemTag);
                shouldResetSampler = true;
                editor.putInt("button_sample_rate", analyzerParam.sampleRate);
                break;
            case R.id.button_fftlen:
                mainView.popupMenuFftLen.dismiss();
                analyzerParam.fftLen = Integer.parseInt(selectedItemTag);
                analyzerParam.hopLen = (int) (analyzerParam.fftLen * (1 - analyzerParam.overlapPercent / 100) + 0.5);
                shouldResetSampler = true;
                editor.putInt("button_fftlen", analyzerParam.fftLen);
                break;
            case R.id.button_average:
                mainView.popupMenuAverage.dismiss();
                analyzerParam.nFFTAverage = Integer.parseInt(selectedItemTag);
                shouldResetSampler = false;
                editor.putInt("button_average", analyzerParam.nFFTAverage);
                break;
            default:
                shouldResetSampler = false;
        }

        editor.apply();

        if (shouldResetSampler) {
            resetSampler(analyzerParam);
        }
    }

    private void loadPreferenceForView() {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        analyzerParam.sampleRate = sharedPref.getInt("button_sample_rate", 8000);
        analyzerParam.fftLen = sharedPref.getInt("button_fftlen", 1024);
        analyzerParam.nFFTAverage = sharedPref.getInt("button_average", 1);
        analyzerParam.isAWeighting = sharedPref.getBoolean("dbA", false);
        if (analyzerParam.isAWeighting) {
            ((ToggleButton) findViewById(R.id.dbA)).toggle();
        }

        ((Button) findViewById(R.id.button_sample_rate)).setText(String.format(Locale.ENGLISH, "%d", analyzerParam.sampleRate));
        ((Button) findViewById(R.id.button_fftlen)).setText(String.format(Locale.ENGLISH, "%d", analyzerParam.fftLen));
        ((Button) findViewById(R.id.button_average)).setText(String.format(Locale.ENGLISH, "%d", analyzerParam.nFFTAverage));
    }

    private void LoadPreferences() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        analyzerParam.audioSourceId = analyzerParam.RECORDER_AGC_OFF;

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        analyzerParam.overlapPercent = Double.parseDouble(sharedPref.getString("fft_overlap_percent", "50.0"));
        analyzerParam.hopLen = (int) (analyzerParam.fftLen * (1 - analyzerParam.overlapPercent / 100) + 0.5);
    }

    public void showPopupMenu(android.view.View view) {
        mainView.showPopupMenu(view);
    }

    private void resetSampler(Setting setting) {
        if (samplingThread != null) {
            samplingThread.finish();
            try {
                samplingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            samplingThread = null;
        }

        if (!checkAndRequestPermissions()) {
            return;
        }

        if (!samplerPrepared) {
            return;
        }

        samplingThread = new Sampler(this, setting);
        samplingThread.start();
    }

    private boolean checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO) && permissionExplanationCount < 1) {
            mainView.showPermissionExplanation(R.string.permission_explanation_recorder);
            permissionExplanationCount++;
        } else {
            if (permissionRequestCount < 3) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
                permissionExplanationCount = 0;
                permissionRequestCount++;
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final Context context = getApplicationContext();
                        final String text = "Permission denied";
                        final Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
                        toast.show();
                    }
                });
            }
        }
        return false;
    }

}
