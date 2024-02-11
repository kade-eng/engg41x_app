package com.example.engg41x_nav_app;

import com.google.android.gms.maps.model.LatLng;

public interface UserLocationCallback {
    void onLocationSet(LatLng userLocation);
}
