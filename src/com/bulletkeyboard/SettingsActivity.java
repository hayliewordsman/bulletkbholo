package com.bulletkeyboard;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private static final int MIN_DP = 40;
    private static final int MAX_DP = 80;

    private SeekBar    heightBar;
    private TextView   heightLabel;
    private RadioGroup colorGroup;
    private SharedPreferences prefs;
    private boolean skipListener = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        heightBar   = (SeekBar)    findViewById(R.id.key_height_seekbar);
        heightLabel = (TextView)   findViewById(R.id.key_height_label);
        colorGroup  = (RadioGroup) findViewById(R.id.color_radio_group);

        if (heightBar == null || heightLabel == null || colorGroup == null) return;

        int saved = prefs.getInt("key_height", 55);
        heightBar.setMax(MAX_DP - MIN_DP);
        heightBar.setProgress(saved - MIN_DP);
        updateLabel(saved);

        heightBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                int dp = MIN_DP + progress;
                updateLabel(dp);
                prefs.edit().putInt("key_height", dp).commit();
            }
            @Override public void onStartTrackingTouch(SeekBar b) {}
            @Override public void onStopTrackingTouch(SeekBar b) {}
        });

        skipListener = true;
        selectRadio(prefs.getString("color_theme", "black"));
        skipListener = false;

        colorGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup g, int id) {
                if (skipListener) return;
                prefs.edit().putString("color_theme", themeFor(id)).commit();
            }
        });
    }

    private void updateLabel(int dp) { heightLabel.setText(dp + " dp"); }

    private void selectRadio(String theme) {
        int id = R.id.radio_black;
        if ("brown".equals(theme))    id = R.id.radio_brown;
        if ("burgundy".equals(theme)) id = R.id.radio_burgundy;
        if ("gray".equals(theme))     id = R.id.radio_gray;
        RadioButton rb = (RadioButton) findViewById(id);
        if (rb != null) rb.setChecked(true);
    }

    private String themeFor(int id) {
        if (id == R.id.radio_brown)    return "brown";
        if (id == R.id.radio_burgundy) return "burgundy";
        if (id == R.id.radio_gray)     return "gray";
        return "black";
    }
}
