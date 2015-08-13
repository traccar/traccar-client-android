
package org.traccar.client;

import android.database.sqlite.SQLiteDatabase;
import android.location.Location;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 21)
public class DatabaseHelperTest {

    @Test
    public void test() throws Exception {

        DatabaseHelper databaseHelper = new DatabaseHelper(RuntimeEnvironment.application);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        Position position = new Position("123456789012345", new Location("gps"), 0);
        position.setTime(new Date(0));

        assertNull(databaseHelper.selectPosition());

        databaseHelper.insertPosition(position);

        position = databaseHelper.selectPosition();

        assertNotNull(position);

        databaseHelper.deletePosition(position.getId());

        assertNull(databaseHelper.selectPosition());

    }

}
