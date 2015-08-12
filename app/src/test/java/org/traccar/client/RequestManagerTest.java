
package org.traccar.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class RequestManagerTest {

    @Test
    public void testSendRequest() throws Exception {

        assertTrue(RequestManager.sendRequest("http://www.google.com"));

    }

}
