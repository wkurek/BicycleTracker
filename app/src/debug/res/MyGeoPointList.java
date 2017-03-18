import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.LinkedList;

final class MyGeoPointList extends ArrayList<MyGeoPoint>
{
    MyGeoPointList()
    {

    }

    MyGeoPointList(ArrayList<MyGeoPoint> arrayList)
    {
        for(MyGeoPoint point : arrayList)
        {
            add(point);
        }
    }

    LinkedList<LatLng> getLatLngList()
    {
        LinkedList<LatLng> locationLinkedList = new LinkedList<LatLng>();

        for( MyGeoPoint geoPoint : this)
        {
            locationLinkedList.add(geoPoint.getLatLng());
        }

        return locationLinkedList;
    }

    LinkedList<Double> getVelocityList()
    {
        LinkedList<Double> locationLinkedList = new LinkedList<Double>();

        for( MyGeoPoint geoPoint : this)
        {
            locationLinkedList.add(geoPoint.getVelocity());
        }

        return locationLinkedList;
    }

    LinkedList<Double> getAltitudeList()
    {
        LinkedList<Double> locationLinkedList = new LinkedList<Double>();

        for( MyGeoPoint geoPoint : this)
        {
            locationLinkedList.add(geoPoint.getAltitude());
        }

        return locationLinkedList;
    }

}