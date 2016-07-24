
package org.traccar.client;

import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class ProtocolFormatterTest {

    @Test
    public void testFormatRequest() throws Exception {

        Position position = new Position("123456789012345", new Location("gps"), 0);
        position.setTime(new Date(0));

        String url = ProtocolFormatter.formatRequest("localhost", 5055, false, position);

        assertEquals("http://localhost:5055?id=123456789012345&timestamp=0&lat=0.0&lon=0.0&speed=0.0&bearing=0.0&altitude=0.0&batt=0.0", url);

    }

}
