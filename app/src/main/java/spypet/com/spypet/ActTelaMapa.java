package spypet.com.spypet;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import modelo.Place;
import modelo.PlacesService;

public class ActTelaMapa extends FragmentActivity implements OnMapReadyCallback,LocationListener,GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLongClickListener,GoogleMap.OnMyLocationButtonClickListener
{

    private GoogleMap gMapa;
    private final String TAG = getClass().getSimpleName();

    private Spinner spLocalType;
    private ArrayAdapter<String> adpLocalType;
    private LocationManager locationManager;
    private boolean isUpdateLocation = false;
    private Location location;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_mapa);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);

        if (gMapa != null) {
            gMapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        spLocalType = (Spinner) findViewById(R.id.spLocalType);

        adpLocalType = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adpLocalType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adpLocalType.addAll(Arrays.asList("Selecione um tipo", "Todos", "Pet Shop", "Clínica Veterinária"));
        spLocalType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (gMapa != null) {
                    String item = (String) parent.getItemAtPosition(position);
                    switch (item) {
                        case "Pet Shop":
                            if (location != null) {
                                gMapa.clear();
                                new GetPlaces(ActTelaMapa.this, "pet_store").execute();
                            }
                            break;
                        case "Clínica Veterinária":
                            if (location != null) {
                                gMapa.clear();
                                new GetPlaces(ActTelaMapa.this, "veterinary_care").execute();
                            }
                            break;
                        case "Todos":
                            if (location != null) {
                                gMapa.clear();
                                new GetPlaces(ActTelaMapa.this, "").execute();
                            }
                            break;
                        case "Selecione um tipo":
                            if (location != null) {
                                gMapa.clear();
                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                        .target(new LatLng(location.getLatitude(), location
                                                .getLongitude())) // Configura o centro do mapa para
                                        // o local atual
                                        .zoom(13) // Configura o zoom
                                        .tilt(20) // Configura o tilt para 30 graus
                                        .build(); // Cria a posição da câmera
                                gMapa.animateCamera(CameraUpdateFactory
                                        .newCameraPosition(cameraPosition));
                            }
                            break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spLocalType.setAdapter(adpLocalType);
        spLocalType.setSelection(0);

        setUpdateLocation();

        //nas API > 22 tem de solicitar a permissão do usuário a cada vez que o software é usado
        if (android.os.Build.VERSION.SDK_INT > 22)
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION},
                    12); //12 é uma constante qualquer

    }

    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        gMapa = googleMap;
        //para adicionar os botões de zoom
        gMapa.getUiSettings().setZoomControlsEnabled(true);
        //antes precisa checar se as permissões foram concedidas no AndroidManifest
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED)
        {
            //botão para centralizar o mapa no local que o usuário se encontra
            googleMap.setMyLocationEnabled(true);
        }

        //adiciona os eventos ao mapa
        gMapa.setOnMapClickListener(this);
        gMapa.setOnMapLongClickListener(this);
        gMapa.setOnMyLocationButtonClickListener(this);
    }

    private void showMsgGPSDesabilitado()
    {
        AlertDialog.Builder msg = new AlertDialog.Builder(this);

        msg.setMessage("Deseja habilitar o serviço de localização?")
                .setCancelable(false)
                .setPositiveButton("Ir para a tela de configuração",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                            }
                        });
        msg.setNegativeButton("Cancelar",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = msg.create();
        alert.show();
    }

    private void setUpdateLocation() {
        //é necessário checar se as permissões foram concedidas ao instalar ou usar o app
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(ActTelaMapa.this);
            //verifica se a localização está habilitada
            if (!ActTelaMapa.this.locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                showMsgGPSDesabilitado();
            }
            else {

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        0, 10, ActTelaMapa.this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        0, 10, ActTelaMapa.this);

            }
            String provider = locationManager.getBestProvider(new Criteria(), false);
            location = locationManager.getLastKnownLocation(provider);
        }
    }


    @Override
    public void onLocationChanged(Location loc) {
        Log.e(TAG, "location update : " + location);
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            locationManager.removeUpdates(ActTelaMapa.this);
            location = loc;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    //chamado quando o usuário habilita a localização
    @Override
    public void onProviderEnabled(String provider) {
        setUpdateLocation();
    }

    //chamado quando o usuário desabilita a localização
    @Override
    public void onProviderDisabled(String provider)
    {
        setUpdateLocation();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if(isUpdateLocation)
        {
            setUpdateLocation();
        }
    }

    @Override
    protected void onStop()
    {
        if ( ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED )
            locationManager.removeUpdates(this); //finalizar a atualização
        super.onStop();
    }

    @Override
    public void onMapClick(LatLng latLng)
    {

    }

    @Override
    public void onMapLongClick(LatLng latLng)
    {

    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    private class GetPlaces extends AsyncTask<Void, Void, ArrayList<Place>> {

        private ProgressDialog dialog;
        private Context context;
        private String places;

        public GetPlaces(Context context, String places) {
            this.context = context;
            this.places = places;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(context);
            dialog.setCancelable(false);
            dialog.setMessage("Loading..");
            dialog.isIndeterminate();
            dialog.show();
        }

        @Override
        protected ArrayList<Place> doInBackground(Void... arg0) {
            PlacesService service = new PlacesService(
                    "AIzaSyDS0bQ8j_cxndeL-07vvd0KvhHWJvkCYss");

            ArrayList<Place> findPlaces = service.findPlaces(location.getLatitude(), // -23.189174
                    location.getLongitude(), places); // -45.787756

            for (int i = 0; i < findPlaces.size(); i++) {

                Place placeDetail = findPlaces.get(i);
                Log.e(TAG, "places : " + placeDetail.getName());
            }
            return findPlaces;
        }

        @Override
        protected void onPostExecute(ArrayList<Place> result) {
            super.onPostExecute(result);
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            for (int i = 0; i < result.size(); i++) {
                gMapa.addMarker(new MarkerOptions()
                        .title(result.get(i).getName())
                        .snippet(result.get(i).getVicinity())
                        .position(
                                new LatLng(result.get(i).getLatitude(), result
                                        .get(i).getLongitude()))
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.drawable.pin))
                        .snippet(result.get(i).getVicinity()));

                gMapa.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

                    @Override
                    public View getInfoWindow(Marker arg0) {
                        return null;
                    }

                    @Override
                    public View getInfoContents(Marker marker) {

                        LinearLayout info = new LinearLayout(context);
                        info.setOrientation(LinearLayout.VERTICAL);

                        TextView title = new TextView(context);
                        title.setTextColor(Color.BLACK);
                        title.setGravity(Gravity.CENTER);
                        title.setTypeface(null, Typeface.BOLD);
                        title.setText(marker.getTitle());

                        TextView snippet = new TextView(context);
                        snippet.setTextColor(Color.GRAY);
                        snippet.setText(marker.getSnippet());

                        info.addView(title);
                        info.addView(snippet);

                        return info;
                    }
                });



            }
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(result.get(0).getLatitude(), result
                            .get(0).getLongitude())) // Sets the center of the map to
                    // Mountain View
                    .zoom(14) // Sets the zoom
                    .tilt(30) // Sets the tilt of the camera to 30 degrees
                    .build(); // Creates a CameraPosition from the builder
            gMapa.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition));
        }

    }
}
