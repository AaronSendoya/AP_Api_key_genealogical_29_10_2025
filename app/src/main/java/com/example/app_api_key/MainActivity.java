package com.example.app_api_key;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, FormFragment.OnPlaceAddedListener {

    private static final String KEY_FAMILY_PLACES = "key_family_places";
    private static final int REQ_LOCATION = 101;
    private static final String PREFS = "genealogico_prefs";
    private static final String KEY_PLACES = "family_places_json";

    private GoogleMap myMap;
    private final ArrayList<FamilyPlace> familyPlaces = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.form_container, new FormFragment())
                    .commitNow();
        } else {
            ArrayList<FamilyPlace> restored = savedInstanceState.getParcelableArrayList(KEY_FAMILY_PLACES);
            if (restored != null) {
                familyPlaces.clear();
                familyPlaces.addAll(restored);
            }
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.map_container, mapFragment)
                    .commitNow();
        }
        // Restore persisted places before the map renders
        restorePlaces();
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        // Mantener lógica existente del marcador de demo si no hay lugares aún
        LatLng santaCruz = new LatLng(-17.78629, -63.18117);
        MarkerOptions demoOptions = new MarkerOptions().position(santaCruz).title("Santa Cruz puej perrito");
        demoOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        myMap.addMarker(demoOptions);

        if (familyPlaces.isEmpty()) {
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(santaCruz, 10f));
        }

        renderAllMarkers(); // Re-dibujar lugares guardados tras rotación/inicio
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_FAMILY_PLACES, familyPlaces);
    }

    private void renderAllMarkers() {
        if (myMap == null) return;
        myMap.clear();

        for (FamilyPlace place : familyPlaces) {
            float hue = (place.colorName == null || "Automático".equalsIgnoreCase(place.colorName))
                    ? getHueForRelation(place.relation)
                    : getHueForColorName(place.colorName);
            MarkerOptions options = new MarkerOptions()
                    .position(place.toLatLng())
                    .title(place.name)
                    .snippet(place.relation + " — " + place.address)
                    .icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(hue));
            myMap.addMarker(options);
        }

        if (!familyPlaces.isEmpty()) {
            FamilyPlace first = familyPlaces.get(0);
            myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first.toLatLng(), 12f));
        }
    }

    @Override
    public void onClearAllRequested() {
        if (familyPlaces.isEmpty()) {
            Toast.makeText(this, "No hay datos que borrar", Toast.LENGTH_SHORT).show();
            return;
        }
        familyPlaces.clear();
        if (myMap != null) {
            myMap.clear();
        }
        Toast.makeText(this, "Se eliminaron todas las direcciones", Toast.LENGTH_SHORT).show();
        persistPlaces();
    }

    @Override
    public void onRemoveLastRequested() {
        if (familyPlaces.isEmpty()) {
            Toast.makeText(this, "No hay direcciones para borrar", Toast.LENGTH_SHORT).show();
            return;
        }
        familyPlaces.remove(familyPlaces.size() - 1);
        if (myMap != null) {
            myMap.clear();
            renderAllMarkers();
        }
        Toast.makeText(this, "Se eliminó la última dirección", Toast.LENGTH_SHORT).show();
        persistPlaces();
    }

    private float getHueForRelation(String relation) {
        String r = relation == null ? "" : relation.trim().toLowerCase();
        if (r.contains("padre")) return BitmapDescriptorFactory.HUE_BLUE;
        if (r.contains("madre")) return BitmapDescriptorFactory.HUE_ROSE;
        if (r.contains("abuelo")) return BitmapDescriptorFactory.HUE_GREEN;
        if (r.contains("abuela")) return BitmapDescriptorFactory.HUE_ORANGE;
        if (r.contains("tío")) return BitmapDescriptorFactory.HUE_AZURE;
        if (r.contains("tía")) return BitmapDescriptorFactory.HUE_VIOLET;
        if (r.contains("primo")) return BitmapDescriptorFactory.HUE_CYAN;
        if (r.contains("prima")) return BitmapDescriptorFactory.HUE_MAGENTA;
        return BitmapDescriptorFactory.HUE_RED;
    }

    @Override
    public void onPlaceAdded(FamilyPlace place) {
        familyPlaces.add(place);
        if (myMap == null) return;

        float hue;
        if (place.colorName == null || "Automático".equalsIgnoreCase(place.colorName)) {
            hue = getHueForRelation(place.relation);
        } else {
            hue = getHueForColorName(place.colorName);
        }

        MarkerOptions options = new MarkerOptions()
                .position(place.toLatLng())
                .title(place.name)
                .snippet(place.relation + " — " + place.address)
                .icon(BitmapDescriptorFactory.defaultMarker(hue));
        myMap.addMarker(options);
        myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.toLatLng(), 12f));
        persistPlaces();
    }

    @Override
    protected void onPause() {
        super.onPause();
        persistPlaces();
    }

    private void persistPlaces() {
        try {
            JSONArray arr = new JSONArray();
            for (FamilyPlace p : familyPlaces) {
                try { arr.put(p.toJson()); } catch (Exception ignored) {}
            }
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            sp.edit().putString(KEY_PLACES, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void restorePlaces() {
        try {
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            String json = sp.getString(KEY_PLACES, "[]");
            JSONArray arr = new JSONArray(json);
            familyPlaces.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                try { familyPlaces.add(FamilyPlace.fromJson(o)); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    private float getHueForColorName(String name) {
        if (name == null) return BitmapDescriptorFactory.HUE_RED;
        String n = name.trim().toLowerCase();
        switch (n) {
            case "rojo": return BitmapDescriptorFactory.HUE_RED;
            case "azul": return BitmapDescriptorFactory.HUE_BLUE;
            case "verde": return BitmapDescriptorFactory.HUE_GREEN;
            case "naranja": return BitmapDescriptorFactory.HUE_ORANGE;
            case "violeta": return BitmapDescriptorFactory.HUE_VIOLET;
            case "magenta": return BitmapDescriptorFactory.HUE_MAGENTA;
            case "cian": return BitmapDescriptorFactory.HUE_CYAN;
            case "amarillo": return BitmapDescriptorFactory.HUE_YELLOW;
            case "rosa": return BitmapDescriptorFactory.HUE_ROSE;
            case "azul claro": return BitmapDescriptorFactory.HUE_AZURE;
            default: return BitmapDescriptorFactory.HUE_RED;
        }
    }

    @Override
    public void onCenterOnMyLocationRequested() {
        centerOnMyLocation();
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQ_LOCATION);
    }

    private void centerOnMyLocation() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }
        if (myMap != null) {
            try { myMap.setMyLocationEnabled(true); } catch (SecurityException ignored) {}
            LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location loc = null;
            if (lm != null) {
                try { if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) loc = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ? lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) : null; } catch (SecurityException ignored) {}
                if (loc == null) {
                    try { if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); } catch (SecurityException ignored) {}
                }
            }
            if (loc != null) {
                LatLng me = new LatLng(loc.getLatitude(), loc.getLongitude());
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 15f));
            } else {
                Toast.makeText(this, "Ubicación no disponible", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                centerOnMyLocation();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}