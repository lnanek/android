package org.owntracks.android.ui.map;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.maps.model.LatLng;
import com.sueselbeck.heredemo.BasicMapActivity;

import org.owntracks.android.model.FusedContact;

import java.util.List;

/**
 * Created by Lance on 9/11/16.
 */
public class CenterOpener {

    //ArrayList<GeoPoint> track;

    public static void openHereMapsAt(Context context, LatLng target) {
        final Intent intent = new Intent(context, BasicMapActivity.class);
        if ( null != target ) {
            intent.putExtra(BasicMapActivity.LAT_EXTRA, target.latitude);
            intent.putExtra(BasicMapActivity.LON_EXTRA, target.longitude);
        }
        context.startActivity(intent);
    }

    public static LatLng getCenter(List<FusedContact> contacts) {
        if ( null == contacts || contacts.isEmpty() ) {
            return null;
        }
        double longitude = 0;
        double latitude = 0;
        double maxlat = 0, minlat = 0, maxlon = 0, minlon = 0;
        int i = 0;
        for (FusedContact p : contacts) {
            latitude = p.getLatLng().latitude;
            longitude = p.getLatLng().longitude;
            if (i == 0) {
                maxlat = latitude;
                minlat = latitude;
                maxlon = longitude;
                minlon = longitude;
            } else {
                if (maxlat < latitude)
                    maxlat = latitude;
                if (minlat > latitude)
                    minlat = latitude;
                if (maxlon < longitude)
                    maxlon = longitude;
                if (minlon > longitude)
                    minlon = longitude;
            }
            i++;
        }
        latitude = (maxlat + minlat) / 2;
        longitude = (maxlon + minlon) / 2;
        return new LatLng(latitude, longitude);
    }

}
