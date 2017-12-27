/*
 * Copyright (C) 2016 The CyanogenMod Project
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

package org.lineageos.weather.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import lineageos.providers.WeatherContract;
import lineageos.providers.WeatherContract.WeatherColumns;
import lineageos.weather.WeatherInfo;
import lineageos.weather.WeatherInfo.DayForecast;

import java.util.ArrayList;
import java.util.List;

import static lineageos.providers.WeatherContract.WeatherColumns.CURRENT_CITY;
import static lineageos.providers.WeatherContract.WeatherColumns.CURRENT_CONDITION;
import static lineageos.providers.WeatherContract.WeatherColumns.CURRENT_CONDITION_CODE;
import static lineageos.providers.WeatherContract.WeatherColumns.CURRENT_HUMIDITY;
import static lineageos.providers.WeatherContract.WeatherColumns.CURRENT_TEMPERATURE;
import static lineageos.providers.WeatherContract.WeatherColumns.CURRENT_TEMPERATURE_UNIT;
import static lineageos.providers.WeatherContract.WeatherColumns.CURRENT_TIMESTAMP;
import static lineageos.providers.WeatherContract.WeatherColumns.CURRENT_WIND_DIRECTION;
import static lineageos.providers.WeatherContract.WeatherColumns.CURRENT_WIND_SPEED;
import static lineageos.providers.WeatherContract.WeatherColumns.CURRENT_WIND_SPEED_UNIT;
import static lineageos.providers.WeatherContract.WeatherColumns.FORECAST_CONDITION;
import static lineageos.providers.WeatherContract.WeatherColumns.FORECAST_CONDITION_CODE;
import static lineageos.providers.WeatherContract.WeatherColumns.FORECAST_HIGH;
import static lineageos.providers.WeatherContract.WeatherColumns.FORECAST_LOW;
import static lineageos.providers.WeatherContract.WeatherColumns.TODAYS_HIGH_TEMPERATURE;
import static lineageos.providers.WeatherContract.WeatherColumns.TODAYS_LOW_TEMPERATURE;

public class WeatherContentProvider extends ContentProvider {

    private static final int URI_TYPE_CURRENT_AND_FORECAST = 1;
    private static final int URI_TYPE_CURRENT = 2;
    private static final int URI_TYPE_FORECAST = 3;

    private static final String[] PROJECTION_CURRENT = new String[] {
            CURRENT_CITY,
            CURRENT_CONDITION,
            CURRENT_CONDITION_CODE,
            CURRENT_HUMIDITY,
            CURRENT_WIND_DIRECTION,
            CURRENT_WIND_SPEED,
            CURRENT_WIND_SPEED_UNIT,
            CURRENT_TEMPERATURE,
            CURRENT_TEMPERATURE_UNIT,
            TODAYS_HIGH_TEMPERATURE,
            TODAYS_LOW_TEMPERATURE,
            CURRENT_TIMESTAMP
    };

    private static final String[] PROJECTION_FORECAST = new String[] {
            FORECAST_LOW,
            FORECAST_HIGH,
            FORECAST_CONDITION_CODE
    };

    private static final String[] PROJECTION_CURRENT_AND_FORECAST = new String[] {
            CURRENT_CITY,
            CURRENT_CONDITION,
            CURRENT_CONDITION_CODE,
            CURRENT_HUMIDITY,
            CURRENT_WIND_DIRECTION,
            CURRENT_WIND_SPEED,
            CURRENT_WIND_SPEED_UNIT,
            CURRENT_TEMPERATURE,
            CURRENT_TEMPERATURE_UNIT,
            TODAYS_HIGH_TEMPERATURE,
            TODAYS_LOW_TEMPERATURE,
            CURRENT_TIMESTAMP,
            FORECAST_LOW,
            FORECAST_HIGH,
            FORECAST_CONDITION,
            FORECAST_CONDITION_CODE
    };

    private static final UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(WeatherContract.AUTHORITY, "weather/current_and_forecast",
                URI_TYPE_CURRENT_AND_FORECAST);
        sUriMatcher.addURI(WeatherContract.AUTHORITY, "weather/current", URI_TYPE_CURRENT);
        sUriMatcher.addURI(WeatherContract.AUTHORITY, "weather/forecast", URI_TYPE_FORECAST);
    }

    private static WeatherInfo mCachedWeatherInfo;

    @Override
    public boolean onCreate() {
        //TODO: Trigger an update?
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {

        final int projectionType = sUriMatcher.match(uri);
        final MatrixCursor result = new MatrixCursor(resolveProjection(projection, projectionType));

        if (mCachedWeatherInfo != null) {

            if (projectionType!=URI_TYPE_FORECAST) {
                // current
                result.newRow()
                        .add(CURRENT_CITY, mCachedWeatherInfo.getCity())
                        .add(CURRENT_CONDITION, mapWeatherCodeToString(mCachedWeatherInfo
                                .getConditionCode()))
                        .add(CURRENT_CONDITION_CODE, mCachedWeatherInfo.getConditionCode())
                        .add(CURRENT_HUMIDITY, mCachedWeatherInfo.getHumidity())
                        .add(CURRENT_WIND_DIRECTION, mCachedWeatherInfo.getWindDirection())
                        .add(CURRENT_WIND_SPEED, mCachedWeatherInfo.getWindSpeed())
                        .add(CURRENT_WIND_SPEED_UNIT, mCachedWeatherInfo.getWindSpeedUnit())
                        .add(CURRENT_TEMPERATURE, mCachedWeatherInfo.getTemperature())
                        .add(CURRENT_TEMPERATURE_UNIT, mCachedWeatherInfo.getTemperatureUnit())
                        .add(TODAYS_HIGH_TEMPERATURE, mCachedWeatherInfo.getTodaysHigh())
                        .add(TODAYS_LOW_TEMPERATURE, mCachedWeatherInfo.getTodaysLow())
                        .add(CURRENT_TIMESTAMP, mCachedWeatherInfo.getTimestamp());
            }

            if (projectionType != URI_TYPE_CURRENT) {
                // forecast
                for (DayForecast day : mCachedWeatherInfo.getForecasts()) {
                    result.newRow()
                            .add(FORECAST_LOW, day.getLow())
                            .add(FORECAST_HIGH, day.getHigh())
                            .add(FORECAST_CONDITION, mapWeatherCodeToString(day.getConditionCode()))
                            .add(FORECAST_CONDITION_CODE, day.getConditionCode());
                }
            }
            return result;
        } else {
            return null;
        }
    }

    private String[] resolveProjection(String[] projection, int uriType) {
        if (projection != null)
            return projection;
        switch (uriType) {
            default:
            case URI_TYPE_CURRENT_AND_FORECAST:
                return PROJECTION_CURRENT_AND_FORECAST;

            case URI_TYPE_CURRENT:
                return PROJECTION_CURRENT;

            case URI_TYPE_FORECAST:
                return PROJECTION_FORECAST;
        }
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int bulkInsert (Uri uri, ContentValues[] contentValues) {
        int match = sUriMatcher.match(uri);
        if (match == URI_TYPE_CURRENT_AND_FORECAST) {
            int contentValuesCount = contentValues.length;
            synchronized (WeatherContentProvider.this) {
                List<DayForecast> dayForecasts = new ArrayList<>(contentValuesCount - 1);

                for (int indx = 1; indx < contentValuesCount; indx++) {
                    DayForecast.Builder forecastBuilder
                            = new DayForecast.Builder(contentValues[indx]
                                .getAsInteger(FORECAST_CONDITION_CODE));

                    double low = contentValues[indx].getAsDouble(FORECAST_LOW);
                    if (!Double.isNaN(low)) forecastBuilder.setLow(low);

                    double high = contentValues[indx].getAsDouble(FORECAST_HIGH);
                    if (!Double.isNaN(high)) forecastBuilder.setHigh(high);

                    dayForecasts.add(forecastBuilder.build());
                }

                //First row is ALWAYS current weather

                WeatherInfo.Builder builder = new WeatherInfo.Builder(
                        contentValues[0].getAsString(CURRENT_CITY),
                        contentValues[0].getAsDouble(CURRENT_TEMPERATURE),
                        contentValues[0].getAsInteger(CURRENT_TEMPERATURE_UNIT))
                        .setTimestamp(contentValues[0].getAsLong(CURRENT_TIMESTAMP))
                        .setWeatherCondition(contentValues[0].getAsInteger(CURRENT_CONDITION_CODE))
                        .setForecast(dayForecasts);

                double humidity = contentValues[0].getAsDouble(CURRENT_HUMIDITY);
                if (!Double.isNaN(humidity)) builder.setHumidity(humidity);

                double high = contentValues[0].getAsDouble(TODAYS_HIGH_TEMPERATURE);
                if (!Double.isNaN(high)) builder.setTodaysHigh(high);

                double low = contentValues[0].getAsDouble(TODAYS_LOW_TEMPERATURE);
                if (!Double.isNaN(low)) builder.setTodaysLow(low);

                double windSpeed = contentValues[0].getAsDouble(CURRENT_WIND_SPEED);
                double windDirection = contentValues[0].getAsDouble(CURRENT_WIND_DIRECTION);
                int windSpeedUnit = contentValues[0].getAsInteger(CURRENT_WIND_SPEED_UNIT);

                if (!Double.isNaN(windSpeed) && !Double.isNaN(windDirection)) {
                    builder.setWind(windSpeed, windDirection, windSpeedUnit);
                }

                mCachedWeatherInfo = builder.build();
            }
            getContext().getContentResolver().notifyChange(
                    WeatherColumns.CURRENT_AND_FORECAST_WEATHER_URI, null);
            getContext().getContentResolver().notifyChange(
                    WeatherColumns.CURRENT_WEATHER_URI, null);
            getContext().getContentResolver().notifyChange(
                    WeatherColumns.FORECAST_WEATHER_URI, null);
            return contentValuesCount;
        } else {
            throw new IllegalArgumentException("Invalid URI: " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private String mapWeatherCodeToString(int conditionCode) {
        final Resources res = getContext().getResources();
        final int resId = res.getIdentifier("weather_" + conditionCode,
                "string", getContext().getPackageName());
        if (resId != 0) {
            return res.getString(resId);
        }
        return "";
    }
}
