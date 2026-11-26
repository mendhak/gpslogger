package com.mendhak.gpslogger.senders.dawarich;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.test.filters.SmallTest;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DawarichBatchLocationTest {

    @Test
    public void verticalAccuracy_NormalDops_Computed() {
        Double result = DawarichBatchLocation.verticalAccuracyFrom(10, "2.0", "3.0");
        assertThat(Math.abs(result - 15.0) < 0.0001, is(true));
    }

    @Test
    public void verticalAccuracy_ZeroHdop_Null() {
        assertThat(DawarichBatchLocation.verticalAccuracyFrom(10, "0", "3.0"), nullValue());
        assertThat(DawarichBatchLocation.verticalAccuracyFrom(10, "0.0", "0.0"), nullValue());
    }

    @Test
    public void verticalAccuracy_MissingOrGarbage_Null() {
        assertThat(DawarichBatchLocation.verticalAccuracyFrom(10, "", "3.0"), nullValue());
        assertThat(DawarichBatchLocation.verticalAccuracyFrom(10, null, "3.0"), nullValue());
        assertThat(DawarichBatchLocation.verticalAccuracyFrom(10, "abc", "3.0"), nullValue());
    }

    @Test
    public void verticalAccuracy_NonFiniteResult_Null() {
        assertThat(DawarichBatchLocation.verticalAccuracyFrom(Double.NaN, "1.0", "1.0"), nullValue());
        assertThat(DawarichBatchLocation.verticalAccuracyFrom(10, "1.0", "Infinity"), nullValue());
    }

    @Test
    public void toJSON_NoVerticalAccuracy_OmitsField() throws Exception {
        DawarichBatchLocation loc = new DawarichBatchLocation.Builder(
                new double[]{19.111, 12.193}, "2026-11-26T10:00:00.000+00:00")
                .withHorizontalAccuracy(8)
                .build();

        JSONObject properties = loc.toJSON().getJSONObject("properties");
        assertThat(properties.has("vertical_accuracy"), is(false));
        assertThat(Math.abs(properties.getDouble("horizontal_accuracy") - 8.0) < 0.0001, is(true));
    }
}
