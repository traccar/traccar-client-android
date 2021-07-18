package org.traccar.client

import android.os.Build
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.traccar.client.RequestManager.sendRequest

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class RequestManagerTest {

    @Ignore("Not a real unit test")
    @Test
    fun testSendRequest() {
        Assert.assertTrue(sendRequest("http://www.google.com"))
    }

}
