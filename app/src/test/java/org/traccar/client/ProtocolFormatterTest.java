
package org.traccar.client;

import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class ProtocolFormatterTest {

    @Test
    public void testFormatRequest() throws Exception {

        Position position = new Position("123456789012345", new Location("gps"), 0);
        position.setTime(new Date(0));

        String url = ProtocolFormatter.formatRequest("http://localhost:5055", position);
        assertEquals("http://localhost:5055?id=123456789012345&timestamp=0&lat=0.0&lon=0.0&speed=0.0&bearing=0.0&altitude=0.0&accuracy=0.0&batt=0.0", url);
    }

    @Test
    public void testFormatPathPortRequest() throws Exception {

        Position position = new Position("123456789012345", new Location("gps"), 0);
        position.setTime(new Date(0));

        String url = ProtocolFormatter.formatRequest("http://localhost:8888/path", position);
        assertEquals("http://localhost:8888/path?id=123456789012345&timestamp=0&lat=0.0&lon=0.0&speed=0.0&bearing=0.0&altitude=0.0&accuracy=0.0&batt=0.0", url);
    }

    @Test
    public void testFormatAlarmRequest() throws Exception {

        Position position = new Position("123456789012345", new Location("gps"), 0);
        position.setTime(new Date(0));

        String url = ProtocolFormatter.formatRequest("http://localhost:5055/path", position, "alert message");
        assertEquals("http://localhost:5055/path?id=123456789012345&timestamp=0&lat=0.0&lon=0.0&speed=0.0&bearing=0.0&altitude=0.0&accuracy=0.0&batt=0.0&alarm=alert%20message", url);
    }
}
