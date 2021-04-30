
package org.traccar.client;

import android.location.Location;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Date;

import static org.junit.Assert.assertEquals;

@Config(sdk = Build.VERSION_CODES.P)
@RunWith(RobolectricTestRunner.class)
public class ProtocolFormatterTest {

    @Test
    public void testFormatRequestGps() throws Exception {

        Position position = new Position("123456789012345", new Location("gps"), 0);
        position.setTime(new Date(0));
        position.setSpeed(10);
        position.setCourse(45);
        position.setAltitude(100);
        position.setAccuracy(2);

        String url = ProtocolFormatter.formatRequest("http://localhost:5055", position);
        assertEquals("http://localhost:5055?id=123456789012345&timestamp=0&lat=0.0&lon=0.0&batt=0.0&speed=10.0&bearing=45.0&altitude=100.0&accuracy=2.0", url);
    }

    @Test
    public void testFormatPathPortRequestGps() throws Exception {

        Position position = new Position("123456789012345", new Location("gps"), 0);
        position.setTime(new Date(0));
        position.setSpeed(10);
        position.setCourse(45);
        position.setAltitude(100);
        position.setAccuracy(2);

        String url = ProtocolFormatter.formatRequest("http://localhost:8888/path", position);
        assertEquals("http://localhost:8888/path?id=123456789012345&timestamp=0&lat=0.0&lon=0.0&batt=0.0&speed=10.0&bearing=45.0&altitude=100.0&accuracy=2.0", url);
    }

    @Test
    public void testFormatAlarmRequestGps() throws Exception {

        Position position = new Position("123456789012345", new Location("gps"), 0);
        position.setTime(new Date(0));
        position.setSpeed(10);
        position.setCourse(45);
        position.setAltitude(100);
        position.setAccuracy(2);

        String url = ProtocolFormatter.formatRequest("http://localhost:5055/path", position, "alert message");
        assertEquals("http://localhost:5055/path?id=123456789012345&timestamp=0&lat=0.0&lon=0.0&batt=0.0&speed=10.0&bearing=45.0&altitude=100.0&accuracy=2.0&alarm=alert%20message", url);
    }

    @Test
    public void testFormatRequestNetwork() throws Exception {

        Position position = new Position("123456789012345", new Location("network"), 0);
        position.setTime(new Date(0));
        position.setAccuracy(2);

        String url = ProtocolFormatter.formatRequest("http://localhost:5055", position);
        assertEquals("http://localhost:5055?id=123456789012345&timestamp=0&lat=0.0&lon=0.0&batt=0.0&accuracy=2.0", url);
    }

    @Test
    public void testFormatPathPortRequestNetwork() throws Exception {

        Position position = new Position("123456789012345", new Location("network"), 0);
        position.setTime(new Date(0));
        position.setAccuracy(2);

        String url = ProtocolFormatter.formatRequest("http://localhost:8888/path", position);
        assertEquals("http://localhost:8888/path?id=123456789012345&timestamp=0&lat=0.0&lon=0.0&batt=0.0&accuracy=2.0", url);
    }

    @Test
    public void testFormatAlarmRequestNetwork() throws Exception {

        Position position = new Position("123456789012345", new Location("network"), 0);
        position.setTime(new Date(0));
        position.setAccuracy(2);

        String url = ProtocolFormatter.formatRequest("http://localhost:5055/path", position, "alert message");
        assertEquals("http://localhost:5055/path?id=123456789012345&timestamp=0&lat=0.0&lon=0.0&batt=0.0&accuracy=2.0&alarm=alert%20message", url);
    }
}
