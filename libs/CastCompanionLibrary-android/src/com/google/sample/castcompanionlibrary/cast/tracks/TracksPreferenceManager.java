/*
 * Copyright (C) 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sample.castcompanionlibrary.cast.tracks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.accessibility.CaptioningManager;

import com.google.android.gms.cast.TextTrackStyle;
import com.google.sample.castcompanionlibrary.R;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.exceptions.CastException;
import com.google.sample.castcompanionlibrary.utils.LogUtils;
import com.google.sample.castcompanionlibrary.utils.Utils;

import java.util.HashMap;
import java.util.Map;

import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGD;
import static com.google.sample.castcompanionlibrary.utils.LogUtils.LOGE;

/**
 * This class manages preference settings for captions for Android versions prior to KitKat and
 * provides a number of methods that would work across all supported versions of Android.
 */
public class TracksPreferenceManager implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;

    public TracksPreferenceManager(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private static final String TAG = LogUtils.makeLogTag(TracksPreferenceManager.class);
    public static final String FONT_FAMILY_SANS_SERIF = "FONT_FAMILY_SANS_SERIF";
    public static final String EDGE_TYPE_DEFAULT = "EDGE_TYPE_NONE";
    private ListPreference mCaptionFontScaleListPreference;
    private ListPreference mCaptionFontFamilyListPreference;
    private ListPreference mCaptionTextColorListPreference;
    private ListPreference mCaptionTextOpacityListPreference;
    private ListPreference mCaptionEdgeTypeListPreference;
    private ListPreference mCaptionBackgroundColorListPreference;
    private ListPreference mCaptionBackgroundOpacityListPreference;
    private CheckBoxPreference mCaptionAvailability;
    private static Map<String, String> OPACITY_MAPPING = new HashMap<String, String>();
    private static Map<String, Integer> FONT_FAMILY_MAPPING = new HashMap<String, Integer>();
    private static Map<String, Integer> EDGE_TYPE_MAPPING = new HashMap<String, Integer>();
    private boolean isInitialized = false;

    static {
        OPACITY_MAPPING.put("FF", "100");
        OPACITY_MAPPING.put("BF", "75");
        OPACITY_MAPPING.put("80", "50");
        OPACITY_MAPPING.put("3F", "25");
    }

    static {
        FONT_FAMILY_MAPPING.put("FONT_FAMILY_SANS_SERIF", TextTrackStyle.FONT_FAMILY_SANS_SERIF);
        FONT_FAMILY_MAPPING.put("FONT_FAMILY_SERIF", TextTrackStyle.FONT_FAMILY_SERIF);
        FONT_FAMILY_MAPPING.put("FONT_FAMILY_MONOSPACED_SANS_SERIF",
                TextTrackStyle.FONT_FAMILY_MONOSPACED_SANS_SERIF);
    }

    static {
        EDGE_TYPE_MAPPING.put("EDGE_TYPE_NONE", TextTrackStyle.EDGE_TYPE_NONE);
        EDGE_TYPE_MAPPING.put("EDGE_TYPE_OUTLINE", TextTrackStyle.EDGE_TYPE_OUTLINE);
        EDGE_TYPE_MAPPING.put("EDGE_TYPE_DROP_SHADOW", TextTrackStyle.EDGE_TYPE_DROP_SHADOW);
    }

    @SuppressLint("NewApi")
    public TextTrackStyle getTextTrackStyle() {
        final TextTrackStyle textTrackStyle = TextTrackStyle.fromSystemSettings(mContext);
        if (Utils.IS_KITKAT_OR_ABOVE) {
            return textTrackStyle;
        } else {
            // we need to populate all the fields ourselves
            textTrackStyle.setFontGenericFamily(FONT_FAMILY_MAPPING.get(getFontFamily()));
            textTrackStyle.setBackgroundColor(Color.parseColor(getBackgroundColor()));
            textTrackStyle.setEdgeType(EDGE_TYPE_MAPPING.get(getEdgeType()));
            textTrackStyle.setFontScale(getFontScale());
            boolean isBold = Typeface.DEFAULT.isBold();
            boolean isItalic = Typeface.DEFAULT.isItalic();
            int fontStyle = TextTrackStyle.FONT_STYLE_NORMAL;
            if (isBold && isItalic) {
                fontStyle = TextTrackStyle.FONT_STYLE_BOLD_ITALIC;
            } else if (!isBold && !isItalic) {
                fontStyle = TextTrackStyle.FONT_STYLE_NORMAL;
            } else if (isBold) {
                fontStyle = TextTrackStyle.FONT_STYLE_BOLD;
            }
            textTrackStyle.setFontStyle(fontStyle);
            textTrackStyle.setForegroundColor(
                    combineColorAndOpacity(getTextColor(), getTextOpacity()));
            LOGD(TAG, "Edge is: " + getEdgeType());
            textTrackStyle.setBackgroundColor(combineColorAndOpacity(getBackgroundColor(),
                            getBackgroundOpacity())
            );
        }

        return textTrackStyle;
    }

    @SuppressLint("NewApi")
    public boolean isCaptionEnabled() {
        if (Utils.IS_KITKAT_OR_ABOVE) {
            CaptioningManager captioningManager =
                    (CaptioningManager) mContext.getSystemService(Context.CAPTIONING_SERVICE);
            return captioningManager.isEnabled();
        } else {
            return Utils.getBooleanFromPreference(mContext,
                    mContext.getString(R.string.ccl_key_caption_enabled), false);
        }
    }

    public void setFontFamily(String fontFamily) {
        Utils.saveStringToPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_font_family), fontFamily);
    }

    public String getFontFamily() {
        return Utils.getStringFromPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_font_family), FONT_FAMILY_SANS_SERIF);
    }

    public void setFontScale(String value) {
        Utils.saveStringToPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_font_scale), value);
    }

    public float getFontScale() {
        String scaleStr = Utils.getStringFromPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_font_scale),
                TextTrackStyle.DEFAULT_FONT_SCALE + "");
        return Float.parseFloat(scaleStr);
    }

    public void setTextColor(String textColor) {
        Utils.saveStringToPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_text_color), textColor);
    }

    public String getTextColor() {
        return Utils.getStringFromPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_text_color),
                mContext.getString(R.string.prefs_caption_text_color_value_default));
    }

    public void setTextOpacity(String textColor) {
        Utils.saveStringToPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_text_opacity), textColor);
    }

    public String getTextOpacity() {
        return Utils.getStringFromPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_text_opacity),
                mContext.getString(R.string.prefs_caption_text_opacity_value_default));
    }

    public void setEdgeType(String textColor) {
        Utils.saveStringToPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_edge_type), textColor);
    }

    public String getEdgeType() {
        return Utils.getStringFromPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_edge_type), EDGE_TYPE_DEFAULT);
    }

    public void setBackgroundColor(Context mContext, String textColor) {
        Utils.saveStringToPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_background_color), textColor);
    }

    public String getBackgroundColor() {
        return Utils.getStringFromPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_background_color),
                mContext.getString(R.string.prefs_caption_background_color_value_default));
    }

    public void setBackgroundOpacity(String textColor) {
        Utils.saveStringToPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_background_opacity), textColor);
    }

    public String getBackgroundOpacity() {
        return Utils.getStringFromPreference(mContext,
                mContext.getString(R.string.ccl_key_caption_background_opacity),
                mContext.getString(R.string.prefs_caption_background_opacity_value_default));
    }

    public void setupPreferences(PreferenceScreen screen) {
        mCaptionAvailability = (CheckBoxPreference) screen.findPreference(
                mContext.getString(R.string.ccl_key_caption_enabled));

        mCaptionFontScaleListPreference = (ListPreference) screen.findPreference(
                mContext.getString(R.string.ccl_key_caption_font_scale));

        mCaptionFontFamilyListPreference = (ListPreference) screen.findPreference(
                mContext.getString(R.string.ccl_key_caption_font_family));

        mCaptionTextColorListPreference = (ListPreference) screen.findPreference(
                mContext.getString(R.string.ccl_key_caption_text_color));

        mCaptionTextOpacityListPreference = (ListPreference) screen.findPreference(
                mContext.getString(R.string.ccl_key_caption_text_opacity));

        mCaptionEdgeTypeListPreference = (ListPreference) screen.findPreference(
                mContext.getString(R.string.ccl_key_caption_edge_type));

        mCaptionBackgroundColorListPreference = (ListPreference) screen.findPreference(
                mContext.getString(R.string.ccl_key_caption_background_color));

        mCaptionBackgroundOpacityListPreference = (ListPreference) screen.findPreference(
                mContext.getString(R.string.ccl_key_caption_background_opacity));
        isInitialized = true;

        onSharedPreferenceChanged(mSharedPreferences,
                mContext.getString(R.string.ccl_key_caption_enabled), false);
        onSharedPreferenceChanged(mSharedPreferences,
                mContext.getString(R.string.ccl_key_caption_font_family), false);
        onSharedPreferenceChanged(mSharedPreferences,
                mContext.getString(R.string.ccl_key_caption_font_scale), false);
        onSharedPreferenceChanged(mSharedPreferences,
                mContext.getString(R.string.ccl_key_caption_text_color), false);
        onSharedPreferenceChanged(mSharedPreferences,
                mContext.getString(R.string.ccl_key_caption_text_opacity), false);
        onSharedPreferenceChanged(mSharedPreferences,
                mContext.getString(R.string.ccl_key_caption_edge_type), false);
        onSharedPreferenceChanged(mSharedPreferences,
                mContext.getString(R.string.ccl_key_caption_background_color), false);
        onSharedPreferenceChanged(mSharedPreferences,
                mContext.getString(R.string.ccl_key_caption_background_opacity), false);
    }

    private void setCaptionAvailability(boolean status) {
        mCaptionFontScaleListPreference.setEnabled(status);
        mCaptionFontFamilyListPreference.setEnabled(status);
        mCaptionTextColorListPreference.setEnabled(status);
        mCaptionTextOpacityListPreference.setEnabled(status);
        mCaptionEdgeTypeListPreference.setEnabled(status);
        mCaptionBackgroundColorListPreference.setEnabled(status);
        mCaptionBackgroundOpacityListPreference.setEnabled(status);
    }

    private String getCaptionSummaryForList(SharedPreferences sharedPreferences,
            int keyResourceId,
            int defaultResourceId,
            int namesResourceId, int valuesResourceId) {
        Resources resources = mContext.getResources();
        String value = sharedPreferences.getString(resources.getString(keyResourceId),
                resources.getString(defaultResourceId));
        String[] labels = resources.getStringArray(namesResourceId);
        String[] values = resources.getStringArray(valuesResourceId);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return labels[i];
            }
        }
        return "";
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        onSharedPreferenceChanged(sharedPreferences, key, true);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key, boolean broadcast) {
        if (!isInitialized) {
            return;
        }
        if (mContext.getString(R.string.ccl_key_caption_enabled).equals(key)) {
            mCaptionAvailability.setSummary(
                    mCaptionAvailability.isChecked() ? R.string.prefs_caption_enabled
                            : R.string.prefs_caption_disabled
            );
            setCaptionAvailability(mCaptionAvailability.isChecked());
            if (broadcast) {
                try {
                    VideoCastManager.getInstance()
                            .onTextTrackEnabledChanged(mCaptionAvailability.isChecked());
                } catch (CastException e) {

                }
            }
            return;
        }

        if (mContext.getString(R.string.ccl_key_caption_font_scale).equals(key)) {
            mCaptionFontScaleListPreference
                    .setSummary(
                            getCaptionSummaryForList(sharedPreferences,
                                    R.string.ccl_key_caption_font_scale,
                                    R.string.prefs_caption_font_scale_value_default,
                                    R.array.prefs_caption_font_scale_names,
                                    R.array.prefs_caption_font_scale_values)
                    );
        } else if (mContext.getString(R.string.ccl_key_caption_font_family).equals(key)) {
            mCaptionFontFamilyListPreference
                    .setSummary(
                            getCaptionSummaryForList(sharedPreferences,
                                    R.string.ccl_key_caption_font_family,
                                    R.string.prefs_caption_font_family_value_default,
                                    R.array.prefs_caption_font_family_names,
                                    R.array.prefs_caption_font_family_values)
                    );
        } else if (mContext.getString(R.string.ccl_key_caption_text_color).equals(key)) {
            mCaptionTextColorListPreference
                    .setSummary(
                            getCaptionSummaryForList(sharedPreferences,
                                    R.string.ccl_key_caption_text_color,
                                    R.string.prefs_caption_text_color_value_default,
                                    R.array.prefs_caption_color_names,
                                    R.array.prefs_caption_color_values)
                    );
        } else if (mContext.getString(R.string.ccl_key_caption_text_opacity).equals(key)) {
            String opacity = Utils.getStringFromPreference(mContext,
                    mContext.getString(R.string.ccl_key_caption_text_opacity),
                    mContext.getString(R.string.prefs_caption_text_opacity_value_default));
            mCaptionTextOpacityListPreference
                    .setSummary(OPACITY_MAPPING.get(opacity) + "%%");
        } else if (mContext.getString(R.string.ccl_key_caption_edge_type).equals(key)) {
            mCaptionEdgeTypeListPreference
                    .setSummary(
                            getCaptionSummaryForList(sharedPreferences,
                                    R.string.ccl_key_caption_edge_type,
                                    R.string.prefs_caption_edge_type_value_default,
                                    R.array.prefs_caption_edge_type_names,
                                    R.array.prefs_caption_edge_type_values)
                    );
        } else if (mContext.getString(R.string.ccl_key_caption_background_color).equals(key)) {
            mCaptionBackgroundColorListPreference
                    .setSummary(getCaptionSummaryForList(sharedPreferences,
                            R.string.ccl_key_caption_background_color,
                            R.string.prefs_caption_background_color_value_default,
                            R.array.prefs_caption_color_names,
                            R.array.prefs_caption_color_values));
        } else if (mContext.getString(R.string.ccl_key_caption_background_opacity).equals(key)) {
            String opacity = Utils.getStringFromPreference(mContext,
                    mContext.getString(R.string.ccl_key_caption_background_opacity),
                    mContext.getString(R.string.prefs_caption_background_opacity_value_default));
            mCaptionBackgroundOpacityListPreference
                    .setSummary(OPACITY_MAPPING.get(opacity) + "%%");
        }
        if (broadcast) {
            try {
                VideoCastManager.getInstance()
                        .onTextTrackStyleChanged(getTextTrackStyle());
            } catch (CastException e) {
                LOGE(TAG, "Failed to report text track style", e);
            }
        }

    }

    private static int combineColorAndOpacity(String color, String opacity) {
        color = color.replace("#", "");
        return Color.parseColor("#" + opacity + color);
    }
}
