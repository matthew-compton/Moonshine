package com.ambergleam.moonshine.tests;

import android.view.View;

import com.ambergleam.moonshine.ForecastActivity;
import com.ambergleam.moonshine.R;
import com.ambergleam.moonshine.RobolectricGradleTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.util.ActivityController;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
public class AndroidTest {

    @Test
    public void testMainActivityListViewIsVisible() throws Exception {
        ForecastActivity activity = new ForecastActivity();

        ActivityController.of(activity).attach().create();

        int visibility = activity.findViewById(R.id.listview_forecast).getVisibility();
        assertEquals(visibility, View.VISIBLE);
    }

}