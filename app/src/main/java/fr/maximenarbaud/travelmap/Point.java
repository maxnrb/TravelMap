package fr.maximenarbaud.travelmap;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class Point extends AppCompatActivity {
    private final Map<String, Marker> markerMap;

    private boolean selected;

    private final int markerResDrawable;
    private final int menuItemId;
    private final int iconResDrawable;

    private final String poiType;
    private String city;
    private Integer counter;

    public Point(boolean selected, int markerResDrawable, int menuItemId, int iconResDrawable, String poiType) {
        this.selected = selected;
        this.markerResDrawable = markerResDrawable;
        this.menuItemId = menuItemId;
        this.iconResDrawable = iconResDrawable;
        this.poiType = poiType;

        this.markerMap = new HashMap<>();

        counter = 0;
        city = null;
    }

    public int getMenuItemId() { return menuItemId; }
    public int getIconResDrawable() { return iconResDrawable; }
    public int getMarkerResDrawable() { return markerResDrawable; }

    public String getPoiType() { return poiType; }
    public Integer getCounter() { return counter; }
    public boolean isSelected() { return selected; }
    public String getCity() { return city; }

    public void setCity(String city) { this.city = city; }

    public void setSelected(boolean selected) {
        this.selected = selected;

        for (Marker marker : markerMap.values()) {
            marker.setVisible(selected);
        }
    }

    public void incrementCounter() {
        counter++;
    }

    public void initCounter() {
        counter = 0;
    }

    public void addMarker(String markerId, Marker marker) {
        markerMap.put(markerId, marker);
    }

    public void removeMarker(String id) {
        Marker marker = markerMap.get(id);

        if (marker != null) {
            marker.remove();
        }

        markerMap.remove(id);
    }

    public void modifyMarker(QueryDocumentSnapshot document) {
        Double latitude = document.getDouble("latitude");
        Double longitude = document.getDouble("longitude");
        String name = (String)document.getData().get("name");

        Marker marker = markerMap.get(document.getId());

        if (marker != null && latitude != null && longitude != null) {
            marker.setPosition(new LatLng(latitude, longitude));
            marker.setTitle(name);
        }
    }


    public String getMarkerId(Marker marker) {

        for (Map.Entry<String, Marker> entry : markerMap.entrySet()) {
            if (entry.getValue().equals(marker)) {
                return entry.getKey();
            }
        }

        return null;
    }
}
