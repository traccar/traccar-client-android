/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.client;

import java.util.Formatter;
import java.util.Locale;

import android.location.Location;

/**
 * Protocol formatting
 */
public class Protocol {

    /**
     * Format device id message
     */
    public static String createId(String id) {
        StringBuilder s = new StringBuilder("$PGID,");
        Formatter f = new Formatter(s, Locale.ENGLISH);

        s.append(id);

        byte checksum = 0;
        for (byte b : s.substring(1).getBytes()) {
            checksum ^= b;
        }
        f.format("*%02x\r\n", (int) checksum);
        f.close();

        return s.toString();
    }

    /**
     * Format location message
     */
    public static String createNmea(Location l) {
        StringBuilder s = new StringBuilder("$GPRMC,");
        Formatter f = new Formatter(s, Locale.ENGLISH); // Should use DecimalFormat instead

        f.format("%1$tH%1$tM%1$tS.%1$tL,A,", l.getTime());

        double lat = l.getLatitude();
        double lon = l.getLongitude();
        f.format("%02d%07.4f,%c,", (int) Math.abs(lat), Math.abs(lat) % 1 * 60, lat < 0 ? 'S' : 'N' );
        f.format("%03d%07.4f,%c,", (int) Math.abs(lon), Math.abs(lon) % 1 * 60, lon < 0 ? 'W' : 'E' );

        f.format("%.2f,%.2f,", l.getSpeed(), l.getBearing());
        f.format("%1$td%1$tm%1$ty,,", l.getTime());

        byte checksum = 0;
        for (byte b : s.substring(1).getBytes()) {
            checksum ^= b;
        }
        f.format("*%02x\r\n", (int) checksum);
        f.close();

        return s.toString();
    }
}

