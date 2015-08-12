
package org.traccar.client;

import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class ProtocolTest {

    @Test
    public void testFormatRequest() throws Exception {

        Location location = new Location("gps");
        location.setTime(0);

        String url = Protocol.formatRequest("localhost", 5055, "123456789012345", location, 0);

        assertEquals("http://localhost:5055?id=123456789012345&timestamp=0&lat=0.0&lon=0.0&speed=0.0&bearing=0.0&altitude=0.0&batt=0.0", url);

    }

}
