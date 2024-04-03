package com.example.engg41x_nav_app;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.engg41x_nav_app.databinding.ActivityMapsBinding;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private Marker userMarker;
    private LatLng destination = new LatLng(0, 0);
    private LatLng userLoc = new LatLng(0, 0);

    // all bluetooth stuff
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final String DEVICE_NAME = "raspberrypi";

    private boolean connectionEstablished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //req perms
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 1);
        }
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyBBoCNH1FTP-sVY1FCvAHyM8uur8-FP5CU");
        }

        System.out.println("TESTTEST");
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            return;
        } else if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            connectToPairedDevice();
        }

        //map setup
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button mapActionButton = findViewById(R.id.button_map_action);
        mapActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start CarActivity
                Intent intent = new Intent(MapsActivity.this, CarActivity.class);
                startActivity(intent);
            }
        });

        //autocomplete setup
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));

        //creating the zone bias
        System.out.println("BEFORE SETTING BOUNDS");
        setUserLoc(new UserLocationCallback() {
            @Override
            public void onLocationSet(LatLng userLocation) {
                System.out.println("SETTING BOUNDS");
                LatLngBounds bounds = new LatLngBounds(
                        new LatLng(userLocation.latitude - 0.1, userLocation.longitude - 0.1),
                        new LatLng(userLocation.latitude + 0.1, userLocation.longitude + 0.1));
                autocompleteFragment.setLocationBias(RectangularBounds.newInstance(bounds));
                System.out.println("BOUNDS SET TO: " + bounds.toString());
            }
        });
        System.out.println("AFTER SETTING BOUNDS");

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onError(@NonNull Status status) {
                System.out.println("ERROR NAVIGATING: " + status.getStatusMessage());
            }

            @Override
            public void onPlaceSelected(@NonNull Place place) {
                destination = place.getLatLng();
            }
        });

        //button setup
        Button getDirectionsButton = findViewById(R.id.btn_get_directions);
        getDirectionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateDirections();
            }
        });
    }

    private void connectToPairedDevice() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //connect to pi
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals(DEVICE_NAME)) {

                    System.out.println("CONNECTION ESTABLISHED!!!!");
                    connectToDevice(device);
                    connectionEstablished = true;
                    break;
                } else {
                    System.out.println("NO CONNECTION");
                }
            }
        }
    }

    private void connectToDevice(BluetoothDevice device) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            System.out.println("CREATING SOCKET");
            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            bluetoothAdapter.cancelDiscovery();
            bluetoothSocket.connect();
            System.out.println("SENDING DATA");
            //sendData("{\"key\": \"value\"}");
        } catch (IOException e) {
            System.out.println("ERROR CONNECTING TO DEVICE:" + e);
        }
    }

    private void sendData(String data) {
        if (bluetoothSocket != null) {
            try {
                OutputStream outputStream = bluetoothSocket.getOutputStream();
                outputStream.write(data.getBytes());
                System.out.println("WROTE DATA: " + data);
            } catch (IOException e) {
                System.out.println("ERROR SENDING DATA TO DEVICE:" + e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                System.out.println("ERROR CLOSING SOCKET:" + e);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //add uofg marker
/*        LatLng sydney = new LatLng(43.53007, -80.22566);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker at UofG"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
    }

    public void setUserLoc(UserLocationCallback callback) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            LatLng userLatLng = new LatLng(latitude, longitude);
                            userLoc = userLatLng;

                            if (userMarker != null){
                                userMarker.remove();
                            }

                            userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title("User Location"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(userLatLng));

                            if (callback != null) {
                                callback.onLocationSet(userLatLng);
                            }
                        }
                    });
        }
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private void drawPolylineOnMap(List<LatLng> list) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(destination).title("Destination"));
        mMap.addMarker(new MarkerOptions().position(userLoc).title("User Location"));
        PolylineOptions options = new PolylineOptions().width(10).color(Color.BLUE).geodesic(true);
        options.addAll(list);
        mMap.addPolyline(options);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : list) {
            builder.include(latLng);
        }
        LatLngBounds bounds = builder.build();

        int padding = 100;
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cu);
    }

    private void fetchDirections(String origin, String destination) {
        System.out.println("HERE IS THE ORIGIN: " + origin);
        System.out.println("HERE IS THE DEST: " + destination);
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... strings) {
                String origin = strings[0];
                String destination = strings[1];
                String response = "";
                try {
                    String urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                            "origin=" + origin + "&destination=" + destination + "&key=AIzaSyBBoCNH1FTP-sVY1FCvAHyM8uur8-FP5CU";
                    URL url = new URL(urlString);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    try {
                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line).append('\n');
                        }
                        response = sb.toString();
                    } finally {
                        urlConnection.disconnect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return response;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                try {
                    // Parse the JSON response
                    final JSONObject json = new JSONObject(result);
                    JSONArray routes = json.getJSONArray("routes");
                    JSONObject route = routes.getJSONObject(0);

                    JSONObject polyline = route.getJSONObject("overview_polyline");
                    String encodedPolyline = polyline.getString("points");
                    List<LatLng> list = decodePoly(encodedPolyline);
                    // Draw the polyline on the map
                    runOnUiThread(() -> drawPolylineOnMap(list));

                    // Process routes
                    JSONArray legs = route.getJSONArray("legs");
                    if (legs.length() > 0) {
                        List<String> directionsList = new ArrayList<>();
                        for (int i = 0; i < legs.length(); i++) {
                            JSONArray steps = legs.getJSONObject(i).getJSONArray("steps");
                            for (int j = 0; j < steps.length(); j++) {
                                JSONObject step = steps.getJSONObject(j);
                                String distance = step.getJSONObject("distance").getString("text");
                                String instruction = step.getString("html_instructions");
                                String maneuver = step.optString("maneuver", "");

                                // Convert maneuver to more user-friendly instruction
                                if (!maneuver.isEmpty() ) {
                                    if (maneuver.contains("left")) {
                                        instruction = "Turn left in " + distance;
                                    } else if (maneuver.contains("right")) {
                                        instruction = "Turn right in " + distance;
                                    } else {
                                        instruction = Html.fromHtml(instruction).toString() + " for " + distance;
                                    }
                                } else {
                                    // Fallback for steps without a maneuver specified
                                    instruction = Html.fromHtml(instruction).toString() + " for " + distance;
                                }
                                directionsList.add(instruction);
                            }
                        }
                        System.out.println("HERE ARE THE DIRECTIONS: " + directionsList.get(1));
                        runOnUiThread(() -> {
                            TextView directionsTextView = findViewById(R.id.directionsTextView); // Assume you have this TextView in your layout
                            directionsTextView.setText(directionsList.get(1));

                            String[] parts = directionsList.get(1).split(" ");
                            String dist = parts[parts.length - 2] + " " + parts[parts.length - 1];
                            System.out.println("HERE WE ARE YAY");
                            System.out.println("parts[1]: " + parts[1]);
                            if (connectionEstablished) {
                                if (parts[1].equalsIgnoreCase("left")) {
                                    System.out.println("{\"dir\":\"left\",\"dist\":\""+dist+"\"}");
                                    sendData("{\"dir\":\"left\",\"dist\":\""+dist+"\"}");
                                } else if (parts[1].equalsIgnoreCase("right")) {
                                    System.out.println("{\"dir\":\"right\",\"dist\":\""+dist+"\"}");
                                    sendData("{\"dir\":\"right\",\"dist\":\""+dist+"\"}");
                                } else {
                                    System.out.println("{\"dir\":\"forward\",\"dist\":\""+dist+"\"}");
                                    sendData("{\"dir\":\"forward\",\"dist\":\""+dist+"\"}");
                                }
                            }
                        });
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }.execute(origin, destination);
    }

    private void updateDirections() {
        System.out.println("UPDATING THE DIRECTIONS AGAIN! NICE!");
        setUserLoc(new UserLocationCallback() {
            @Override
            public void onLocationSet(LatLng userLocation) {
                String originStr = userLocation.latitude+","+userLocation.longitude;
                userLoc = userLocation;
                String destStr = destination.latitude+","+destination.longitude;
                fetchDirections(originStr, destStr);
            }
        });
    }
}