
package org.traccar.client;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.lang.Override;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class RequestManagerTest {

    @Test
    public void testSendRequest() throws Exception {

        RequestManager.sendRequest("http://www.google.com", new RequestManager.RequestHandler() {
            @Override
            public void onSuccess() {
                Assert.assertTrue(true);
            }
            @Override
            public void onFailure() {
                Assert.assertTrue(false);
            }
        });

    }

}
