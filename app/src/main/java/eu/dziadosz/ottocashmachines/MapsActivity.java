package eu.dziadosz.ottocashmachines;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.direction;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private JSONArray ottosData;
    private GoogleMap mMap;
    private static Context context;
    Polyline polyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getApplicationContext();
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        FetchDataTask task = new FetchDataTask();
        task.execute("http://dev.dziadosz.eu/android/eu.dziadosz.ottocashmachines/data.json");
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng jyv = new LatLng(62.2333333, 25.7333333);
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(jyv, 12.0f));
    }

    private void getDirections(Marker marker){
        String serverKey = "AIzaSyD2Sk55zsrp8_YcB6OkaFLO4adu6FbX6Nc";
        LatLng origin = new LatLng(62.2333333, 25.7333333);
        GoogleDirection direction = new GoogleDirection();
        direction.withServerKey(serverKey)
                .from(origin)
                .to(marker.getPosition())
                .transportMode(TransportMode.WALKING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction, String rawBody) {
                        // Do something here
                        if (polyline != null) polyline.remove();

                        Route route = direction.getRouteList().get(0);
                        Leg leg = route.getLegList().get(0);
                        ArrayList<LatLng> directionPositionList = leg.getDirectionPoint();
                        PolylineOptions polylineOptions = DirectionConverter.createPolyline(context, directionPositionList, 5, Color.RED);
                        polyline = mMap.addPolyline(polylineOptions);
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        Toast.makeText(getApplicationContext(), "Direction error", Toast.LENGTH_SHORT).show();
                    }
                });


    }

    class FetchDataTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... urls) {
            HttpURLConnection urlConnection = null;
            JSONObject json = null;
            try {
                URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                bufferedReader.close();
                json = new JSONObject(stringBuilder.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
            return json;
        }

        protected void onPostExecute(JSONObject json) {
            try {
                ottosData = json.getJSONArray("Ottos");
                for (int i = 0; i < ottosData.length(); i++) {
                    JSONObject otto = ottosData.getJSONObject(i);
                    mMap.addMarker(new MarkerOptions().position(new LatLng(otto.getDouble("Lat"), otto.getDouble("Lon"))).title(otto.getString("KohteenOsoite")));
                }
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        Toast.makeText(getApplicationContext(), marker.getTitle(), Toast.LENGTH_SHORT).show();
                        getDirections(marker);

                        return true;
                    }
                });
            } catch (JSONException e) {
                Log.e("JSON", "Error getting data.");
            }
        }


    }
}
