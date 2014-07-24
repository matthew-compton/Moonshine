package com.ambergleam.moonshine;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

import com.ambergleam.moonshine.model.WeatherContract.LocationEntry;
import com.ambergleam.moonshine.model.WeatherContract.WeatherEntry;

public class TestProvider extends AndroidTestCase {
    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    // The target api annotation is needed for the call to keySet -- we wouldn't want
    // to use this in our app, but in a test it's fine to assume a higher target.
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void addAllContentValues(ContentValues destination, ContentValues source) {
        for (String key : source.keySet()) {
            destination.put(key, source.getAsString(key));
        }
    }

    // Since we want each test to start with a clean slate, run deleteAllRecords
    // in setUp (called by the test runner before each test).
    public void setUp() {
        deleteAllRecords();
    }

    public void testInsertReadDb() {
        ContentValues locationValues = TestDatabase.createNorthPoleLocationValues();
        Uri locationInsertUri = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI, locationValues);

        long locationRowId = ContentUris.parseId(locationInsertUri);
        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New row id: " + locationRowId);

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,  // Table to Query
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null // sort order
        );
        TestDatabase.validateCursor(cursor, locationValues);

        // Now see if we can successfully query if we include the row id
        cursor = mContext.getContentResolver().query(
                LocationEntry.buildLocationUri(locationRowId),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestDatabase.validateCursor(cursor, locationValues);

        // Fantastic.  Now that we have a location, add some weather!
        ContentValues weatherValues = TestDatabase.createWeatherValues(locationRowId);
        Uri weatherInsertUri = mContext.getContentResolver().insert(WeatherEntry.CONTENT_URI, weatherValues);
        assertTrue(weatherInsertUri != null);

        // A cursor is your primary interface to the query results.
        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );
        TestDatabase.validateCursor(weatherCursor, weatherValues);


        // Add the location values in with the weather data so that we can make
        // sure that the join worked and we actually get all the values back
        addAllContentValues(weatherValues, locationValues);

        // Get the joined Weather and Location data
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocation(TestDatabase.TEST_LOCATION),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestDatabase.validateCursor(weatherCursor, weatherValues);

        // Get the joined Weather and Location data with a start date
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocationWithStartDate(
                        TestDatabase.TEST_LOCATION, TestDatabase.TEST_DATE),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestDatabase.validateCursor(weatherCursor, weatherValues);

        // Get the joined Weather data for a specific date
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocationWithDate(TestDatabase.TEST_LOCATION, TestDatabase.TEST_DATE),
                null,
                null,
                null,
                null
        );
        TestDatabase.validateCursor(weatherCursor, weatherValues);
    }

    public void testGetType() {
        // content://com.example.android.sunshine.app/weather/
        String type = mContext.getContentResolver().getType(WeatherEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testLocation = "94074";
        // content://com.example.android.sunshine.app/weather/94074
        type = mContext.getContentResolver().getType(WeatherEntry.buildWeatherLocation(testLocation));
        // vnd.android.cursor.dir/com.example.android.sunshine.app/weather
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testDate = "20140612";
        // content://com.example.android.sunshine.app/weather/94074/20140612
        type = mContext.getContentResolver().getType(WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));
        // vnd.android.cursor.item/com.example.android.sunshine.app/weather
        assertEquals(WeatherEntry.CONTENT_ITEM_TYPE, type);

        // content://com.example.android.sunshine.app/location/
        type = mContext.getContentResolver().getType(LocationEntry.CONTENT_URI);
        // vnd.android.cursor.dir/com.example.android.sunshine.app/location
        assertEquals(LocationEntry.CONTENT_TYPE, type);

        // content://com.example.android.sunshine.app/location/1
        type = mContext.getContentResolver().getType(LocationEntry.buildLocationUri(1L));
        // vnd.android.cursor.item/com.example.android.sunshine.app/location
        assertEquals(LocationEntry.CONTENT_ITEM_TYPE, type);
    }

    // brings our database to an empty state
    public void deleteAllRecords() {
        mContext.getContentResolver().delete(
                WeatherEntry.CONTENT_URI,
                null,
                null
        );
        mContext.getContentResolver().delete(
                LocationEntry.CONTENT_URI,
                null,
                null
        );

        Cursor cursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    public void testUpdateLocation() {
        // Create a new map of values, where column names are the keys
        ContentValues values = TestDatabase.createNorthPoleLocationValues();

        Uri locationUri = mContext.getContentResolver().
                insert(LocationEntry.CONTENT_URI, values);
        long locationRowId = ContentUris.parseId(locationUri);

        // Verify we got a row back.
        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New row id: " + locationRowId);

        ContentValues updatedValues = new ContentValues(values);
        updatedValues.put(LocationEntry._ID, locationRowId);
        updatedValues.put(LocationEntry.COLUMN_CITY_NAME, "Santa's Village");

        int count = mContext.getContentResolver().update(
                LocationEntry.CONTENT_URI, updatedValues, LocationEntry._ID + "= ?",
                new String[]{Long.toString(locationRowId)});

        assertEquals(count, 1);

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.buildLocationUri(locationRowId),
                null,
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null // sort order
        );

        TestDatabase.validateCursor(cursor, updatedValues);
    }

    // Make sure we can still delete after adding/updating stuff
    public void testDeleteRecordsAtEnd() {
        deleteAllRecords();
    }

    static final String KALAMAZOO_LOCATION_SETTING = "kalamazoo";
    static final String KALAMAZOO_WEATHER_START_DATE = "20140625";

    long locationRowId;

    static ContentValues createKalamazooWeatherValues(long locationRowId) {
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationRowId);
        weatherValues.put(WeatherEntry.COLUMN_DATETEXT, KALAMAZOO_WEATHER_START_DATE);
        weatherValues.put(WeatherEntry.COLUMN_DEGREES, 1.2);
        weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, 1.5);
        weatherValues.put(WeatherEntry.COLUMN_PRESSURE, 1.1);
        weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, 85);
        weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, 35);
        weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, "Cats and Dogs");
        weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, 3.4);
        weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, 42);

        return weatherValues;
    }

    static ContentValues createKalamazooLocationValues() {
        // Create a new map of values, where column names are the keys
        ContentValues testValues = new ContentValues();
        testValues.put(LocationEntry.COLUMN_LOCATION_SETTING, KALAMAZOO_LOCATION_SETTING);
        testValues.put(LocationEntry.COLUMN_CITY_NAME, "Kalamazoo");
        testValues.put(LocationEntry.COLUMN_COORD_LAT, 42.2917);
        testValues.put(LocationEntry.COLUMN_COORD_LONG, -85.5872);

        return testValues;
    }


    // Inserts both the location and weather data for the Kalamazoo data set.
    public void insertKalamazooData() {
        ContentValues kalamazooLocationValues = createKalamazooLocationValues();
        Uri locationInsertUri = mContext.getContentResolver()
                .insert(LocationEntry.CONTENT_URI, kalamazooLocationValues);
        assertTrue(locationInsertUri != null);

        locationRowId = ContentUris.parseId(locationInsertUri);

        ContentValues kalamazooWeatherValues = createKalamazooWeatherValues(locationRowId);
        Uri weatherInsertUri = mContext.getContentResolver()
                .insert(WeatherEntry.CONTENT_URI, kalamazooWeatherValues);
        assertTrue(weatherInsertUri != null);
    }

    public void testUpdateAndReadWeather() {
        insertKalamazooData();
        String newDescription = "Cats and Frogs (don't warn the tadpoles!)";

        // Make an update to one value.
        ContentValues kalamazooUpdate = new ContentValues();
        kalamazooUpdate.put(WeatherEntry.COLUMN_SHORT_DESC, newDescription);

        mContext.getContentResolver().update(
                WeatherEntry.CONTENT_URI, kalamazooUpdate, null, null);

        // A cursor is your primary interface to the query results.
        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Make the same update to the full ContentValues for comparison.
        ContentValues kalamazooAltered = createKalamazooWeatherValues(locationRowId);
        kalamazooAltered.put(WeatherEntry.COLUMN_SHORT_DESC, newDescription);

        TestDatabase.validateCursor(weatherCursor, kalamazooAltered);
    }

    public void testRemoveHumidityAndReadWeather() {
        insertKalamazooData();

        mContext.getContentResolver().delete(WeatherEntry.CONTENT_URI,
                WeatherEntry.COLUMN_HUMIDITY + " = " + locationRowId, null);

        // A cursor is your primary interface to the query results.
        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        // Make the same update to the full ContentValues for comparison.
        ContentValues kalamazooAltered = createKalamazooWeatherValues(locationRowId);
        kalamazooAltered.remove(WeatherEntry.COLUMN_HUMIDITY);

        TestDatabase.validateCursor(weatherCursor, kalamazooAltered);
    }

}