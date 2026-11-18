package com.example.oopproject.frontEnd

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.oopproject.databinding.FragmentCampusMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class CampusMapFragment : Fragment() {

    companion object {
        private const val ARG_TARGET_BUILDING = "target_building"

        fun newInstance(targetBuilding: String = ""): CampusMapFragment {
            val fragment = CampusMapFragment()
            val args = Bundle()
            args.putString(ARG_TARGET_BUILDING, targetBuilding)
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: FragmentCampusMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequest: LocationRequest? = null

    private var lastUserLat: Double = 0.0
    private var lastUserLng: Double = 0.0
    private var isMapLoaded = false
    private var targetBuilding: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            startLocationUpdates()
        } else {
            updateGpsStatus("GPS Permission Denied", "Enable location permission to track position.", "#B91C1C")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCampusMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetBuilding = arguments?.getString(ARG_TARGET_BUILDING) ?: ""
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupWebView()

        binding.fabMyLocation.setOnClickListener {
            if (lastUserLat != 0.0 && lastUserLng != 0.0) {
                binding.webViewMap.evaluateJavascript("map.setView([$lastUserLat, $lastUserLng], 19);", null)
            } else {
                checkLocationPermissionAndStart()
            }
        }

        binding.btnClearRoute.setOnClickListener {
            binding.cardRouteBanner.visibility = View.GONE
            binding.webViewMap.evaluateJavascript("clearRoute();", null)
        }

        checkLocationPermissionAndStart()
    }

    private fun setupWebView() {
        val webView = binding.webViewMap
        val webSettings: WebSettings = webView.settings
        @SuppressLint("SetJavaScriptEnabled")
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapLoaded = true
                loadKmlIntoMap()
                if (lastUserLat != 0.0 && lastUserLng != 0.0) {
                    updateMapUserLocation(lastUserLat, lastUserLng)
                }
            }
        }

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Campus Map</title>
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <style>
                    html, body, #map {
                        width: 100%;
                        height: 100%;
                        margin: 0;
                        padding: 0;
                        background: #F5F3FA;
                    }
                    .leaflet-popup-content-wrapper {
                        border-radius: 12px;
                        padding: 4px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                    }
                    .leaflet-popup-content {
                        font-family: sans-serif;
                        font-size: 14px;
                        color: #111827;
                        line-height: 1.4;
                    }
                    .map-popup h3 {
                        margin: 0 0 6px 0;
                        color: #312E81;
                        font-size: 16px;
                        font-weight: bold;
                    }
                    .map-popup p {
                        margin: 0;
                        color: #4B5563;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <script>
                    const campusBounds = L.latLngBounds(
                        [14.0645, 120.6245],
                        [14.0688, 120.6285]
                    );

                    const map = L.map('map', {
                        maxBounds: campusBounds,
                        maxBoundsViscosity: 1.0,
                        minZoom: 17,
                        maxZoom: 22
                    }).setView([14.06679, 120.62613], 18);

                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                        maxZoom: 22,
                        attribution: '&copy; OpenStreetMap contributors'
                    }).addTo(map);

                    let userMarker = null;
                    let userAccuracyCircle = null;
                    let activeRoutePolyline = null;
                    const buildingMarkers = [];

                    function loadKmlData(kmlString) {
                        try {
                            const parser = new DOMParser();
                            const xmlDoc = parser.parseFromString(kmlString, "text/xml");
                            const placemarks = xmlDoc.getElementsByTagName("Placemark");

                            for (let i = 0; i < placemarks.length; i++) {
                                const pm = placemarks[i];
                                const visEl = pm.getElementsByTagName("visibility")[0];
                                if (visEl && visEl.textContent.trim() === "0") {
                                    continue;
                                }

                                const nameEl = pm.getElementsByTagName("name")[0];
                                const name = nameEl ? nameEl.textContent.trim() : "Campus Location";

                                // 1. Points (Buildings)
                                const pointEl = pm.getElementsByTagName("Point")[0];
                                if (pointEl) {
                                    const coordEl = pointEl.getElementsByTagName("coordinates")[0];
                                    if (coordEl) {
                                        const coords = coordEl.textContent.trim().split(',');
                                        if (coords.length >= 2) {
                                            const lng = parseFloat(coords[0]);
                                            const lat = parseFloat(coords[1]);

                                            const buildingTextIcon = L.divIcon({
                                                className: 'building-label',
                                                html: '<div style="background-color: #312E81; color: #FFFFFF; font-family: sans-serif; font-size: 9.5px; font-weight: bold; padding: 2px 5px; border-radius: 4px; border: 1px solid #FFFFFF; box-shadow: 0 1px 4px rgba(0,0,0,0.3); white-space: nowrap; display: inline-block; transform: translate(-50%, -50%);">' + name + '</div>',
                                                iconSize: null,
                                                iconAnchor: [0, 0]
                                            });
                                            const marker = L.marker([lat, lng], { icon: buildingTextIcon }).addTo(map);
                                            buildingMarkers.push({ name: name, latLng: [lat, lng], marker: marker });
                                        }
                                    }
                                }

                                // 2. Polygons (Areas / Campus bounds)
                                const polygonEl = pm.getElementsByTagName("Polygon")[0];
                                if (polygonEl) {
                                    const outerEl = polygonEl.getElementsByTagName("outerBoundaryIs")[0];
                                    if (outerEl) {
                                        const coordEl = outerEl.getElementsByTagName("coordinates")[0];
                                        if (coordEl) {
                                            const rawCoords = coordEl.textContent.trim().split(/\s+/);
                                            const latLngs = [];
                                            for (let c of rawCoords) {
                                                const parts = c.split(',');
                                                if (parts.length >= 2) {
                                                    const lng = parseFloat(parts[0]);
                                                    const lat = parseFloat(parts[1]);
                                                    latLngs.push([lat, lng]);
                                                }
                                            }
                                            if (latLngs.length > 0) {
                                                const isCampusArea = name.toLowerCase().includes("campus area");
                                                const polyOptions = isCampusArea ? {
                                                    color: '#4F46E5',
                                                    weight: 2,
                                                    fillColor: '#6366F1',
                                                    fillOpacity: 0.15
                                                } : {
                                                    color: '#312E81',
                                                    weight: 2.5,
                                                    fillColor: '#818CF8',
                                                    fillOpacity: 0.35
                                                };
                                                const polygon = L.polygon(latLngs, polyOptions).addTo(map);
                                                if (!isCampusArea) {
                                                    polygon.bindPopup('<div class="map-popup"><h3>📍 ' + name + '</h3></div>');
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. LineStrings (Pathways)
                                const lineStringEl = pm.getElementsByTagName("LineString")[0];
                                if (lineStringEl) {
                                    const coordEl = lineStringEl.getElementsByTagName("coordinates")[0];
                                    if (coordEl) {
                                        const rawCoords = coordEl.textContent.trim().split(/\s+/);
                                        const latLngs = [];
                                        for (let c of rawCoords) {
                                            const parts = c.split(',');
                                            if (parts.length >= 2) {
                                                const lng = parseFloat(parts[0]);
                                                const lat = parseFloat(parts[1]);
                                                latLngs.push([lat, lng]);
                                            }
                                        }
                                        if (latLngs.length > 0) {
                                            const polyline = L.polyline(latLngs, {
                                                color: '#10B981',
                                                weight: 4,
                                                opacity: 0.85
                                            }).addTo(map);
                                            polyline.bindPopup('<div class="map-popup"><h3>🚶 Path: ' + name + '</h3></div>');
                                        }
                                    }
                                }
                            }
                        } catch (e) {
                            console.error("Error parsing KML: ", e);
                        }
                    }

                    const buildingPathways = {
                        "heb": [
                            [14.0676983, 120.6275044],
                            [14.0676689, 120.6275040],
                            [14.0676790, 120.6272302],
                            [14.0676024, 120.6272095],
                            [14.0675513, 120.6271159],
                            [14.0675770, 120.6270092],
                            [14.0676084, 120.6269929],
                            [14.0676217, 120.6269261],
                            [14.0672786, 120.6268539],
                            [14.0672862, 120.6268078],
                            [14.0673227, 120.6267778]
                        ],
                        "cteb": [
                            [14.0676983, 120.6275044],
                            [14.0676689, 120.6275040],
                            [14.0676790, 120.6272302],
                            [14.0676024, 120.6272095],
                            [14.0675513, 120.6271159],
                            [14.0675770, 120.6270092],
                            [14.0676084, 120.6269929],
                            [14.0676217, 120.6269261],
                            [14.0668322, 120.6267601],
                            [14.0668634, 120.6265769],
                            [14.0668771, 120.6263868],
                            [14.0668760, 120.6262777],
                            [14.0668224, 120.6262786],
                            [14.0665895, 120.6263647]
                        ],
                        "taal": [
                            [14.0676983, 120.6275044],
                            [14.0676689, 120.6275040],
                            [14.0676790, 120.6272302],
                            [14.0676024, 120.6272095],
                            [14.0675513, 120.6271159],
                            [14.0675770, 120.6270092],
                            [14.0676084, 120.6269929],
                            [14.0676217, 120.6269261],
                            [14.0668322, 120.6267601],
                            [14.0668634, 120.6265769],
                            [14.0668771, 120.6263868],
                            [14.0668682, 120.6257677],
                            [14.0674974, 120.6257075],
                            [14.0674232, 120.6257123],
                            [14.0674219, 120.6256604],
                            [14.0673323, 120.6256278]
                        ],
                        "cas": [
                            [14.0676983, 120.6275044],
                            [14.0676689, 120.6275040],
                            [14.0676790, 120.6272302],
                            [14.0676024, 120.6272095],
                            [14.0675513, 120.6271159],
                            [14.0675770, 120.6270092],
                            [14.0676084, 120.6269929],
                            [14.0676217, 120.6269261],
                            [14.0668322, 120.6267601],
                            [14.0668634, 120.6265769],
                            [14.0668771, 120.6263868],
                            [14.0668712, 120.6259707],
                            [14.0664964, 120.6259733],
                            [14.0661662, 120.6259628],
                            [14.0661653, 120.6261302],
                            [14.0660605, 120.6261273],
                            [14.0660227, 120.6261462]
                        ],
                        "lemery": [
                            [14.0676983, 120.6275044],
                            [14.0676689, 120.6275040],
                            [14.0676790, 120.6272302],
                            [14.0676024, 120.6272095],
                            [14.0675513, 120.6271159],
                            [14.0675770, 120.6270092],
                            [14.0676084, 120.6269929],
                            [14.0676217, 120.6269261],
                            [14.0668322, 120.6267601],
                            [14.0668634, 120.6265769],
                            [14.0668771, 120.6263868],
                            [14.0668712, 120.6259707],
                            [14.0664964, 120.6259733],
                            [14.0661662, 120.6259628],
                            [14.0660282, 120.6259611],
                            [14.0658861, 120.6259605],
                            [14.0658819, 120.6259896],
                            [14.0658592, 120.6261515]
                        ]
                    };

                    const buildingPathways2ndEntrance = {
                        "taal": [
                            [14.0677514, 120.6259536],
                            [14.0677776, 120.6259555],
                            [14.0677666, 120.6261068],
                            [14.0677368, 120.6261044],
                            [14.0674991, 120.6260995],
                            [14.0674974, 120.6257075],
                            [14.0674232, 120.6257123],
                            [14.0674219, 120.6256604],
                            [14.0673323, 120.6256278]
                        ],
                        "cas": [
                            [14.0677514, 120.6259536],
                            [14.0677776, 120.6259555],
                            [14.0677666, 120.6261068],
                            [14.0677368, 120.6261044],
                            [14.0674991, 120.6260995],
                            [14.0668712, 120.6259707],
                            [14.0664964, 120.6259733],
                            [14.0661662, 120.6259628],
                            [14.0661653, 120.6261302],
                            [14.0660605, 120.6261273],
                            [14.0660227, 120.6261462]
                        ],
                        "lemery": [
                            [14.0677514, 120.6259536],
                            [14.0677776, 120.6259555],
                            [14.0677666, 120.6261068],
                            [14.0677368, 120.6261044],
                            [14.0674991, 120.6260995],
                            [14.0668712, 120.6259707],
                            [14.0664964, 120.6259733],
                            [14.0661662, 120.6259628],
                            [14.0660282, 120.6259611],
                            [14.0658861, 120.6259605],
                            [14.0658819, 120.6259896],
                            [14.0658592, 120.6261515]
                        ],
                        "cteb": [
                            [14.0677514, 120.6259536],
                            [14.0677776, 120.6259555],
                            [14.0677666, 120.6261068],
                            [14.0677368, 120.6261044],
                            [14.0674991, 120.6260995],
                            [14.0668712, 120.6259707],
                            [14.0668760, 120.6262777],
                            [14.0668224, 120.6262786],
                            [14.0665895, 120.6263647]
                        ],
                        "heb": [
                            [14.0677514, 120.6259536],
                            [14.0677776, 120.6259555],
                            [14.0677666, 120.6261068],
                            [14.0677368, 120.6261044],
                            [14.0674991, 120.6260995],
                            [14.0674974, 120.6257075],
                            [14.0668682, 120.6257677],
                            [14.0668771, 120.6263868],
                            [14.0668634, 120.6265769],
                            [14.0668322, 120.6267601],
                            [14.0676217, 120.6269261],
                            [14.0672786, 120.6268539],
                            [14.0672862, 120.6268078],
                            [14.0673227, 120.6267778]
                        ]
                    };

                    function getDistanceInMeters(lat1, lon1, lat2, lon2) {
                        const R = 6371000;
                        const dLat = (lat2 - lat1) * Math.PI / 180;
                        const dLon = (lon2 - lon1) * Math.PI / 180;
                        const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                                  Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                                  Math.sin(dLon/2) * Math.sin(dLon/2);
                        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
                        return R * c;
                    }

                    function focusBuilding(targetName) {
                        if (!targetName) return;
                        const cleanTarget = targetName.toLowerCase().replace("building", "").trim();

                        const mainEntrancePos = [14.0676983, 120.6275044];
                        const secondEntrancePos = [14.0677514, 120.6259536];
                        let useSecondEntrance = false;

                        if (userMarker) {
                            const uLat = userMarker.getLatLng().lat;
                            const uLng = userMarker.getLatLng().lng;
                            const dMain = getDistanceInMeters(uLat, uLng, mainEntrancePos[0], mainEntrancePos[1]);
                            const dSecond = getDistanceInMeters(uLat, uLng, secondEntrancePos[0], secondEntrancePos[1]);
                            useSecondEntrance = dSecond < dMain;
                        }

                        const activeMap = useSecondEntrance ? buildingPathways2ndEntrance : buildingPathways;
                        let routeCoords = null;

                        for (let key in activeMap) {
                            if (cleanTarget.includes(key) || key.includes(cleanTarget)) {
                                routeCoords = Array.from(activeMap[key]);
                                break;
                            }
                        }

                        for (let i = 0; i < buildingMarkers.length; i++) {
                            const item = buildingMarkers[i];
                            if (item.name.toLowerCase().includes(cleanTarget) || cleanTarget.includes(item.name.toLowerCase())) {
                                if (!routeCoords) {
                                    const startLatLng = userMarker ? [userMarker.getLatLng().lat, userMarker.getLatLng().lng] : [14.0676983, 120.6275044];
                                    routeCoords = [startLatLng, item.latLng];
                                } else if (userMarker) {
                                    routeCoords.unshift([userMarker.getLatLng().lat, userMarker.getLatLng().lng]);
                                }

                                if (activeRoutePolyline) {
                                    map.removeLayer(activeRoutePolyline);
                                }
                                activeRoutePolyline = L.polyline(routeCoords, {
                                    color: '#2563EB',
                                    weight: 6,
                                    opacity: 0.95,
                                    dashArray: '8, 8'
                                }).addTo(map);
                                map.fitBounds(activeRoutePolyline.getBounds(), { padding: [50, 50] });
                                break;
                            }
                        }
                    }

                    function clearRoute() {
                        if (activeRoutePolyline) {
                            map.removeLayer(activeRoutePolyline);
                            activeRoutePolyline = null;
                        }
                        map.setView([14.06679, 120.62613], 18);
                    }

                    function updateUserLocation(lat, lng) {
                        const latLng = [lat, lng];
                        if (!userMarker) {
                            const userIcon = L.divIcon({
                                className: 'user-gps-dot',
                                html: '<div style="background-color: #3B82F6; width: 16px; height: 16px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 8px rgba(0,0,0,0.4);"></div>',
                                iconSize: [16, 16],
                                iconAnchor: [8, 8]
                            });
                            userMarker = L.marker(latLng, { icon: userIcon }).addTo(map);
                            userMarker.bindPopup("<b>📍 You are here</b>");
                        } else {
                            userMarker.setLatLng(latLng);
                        }

                        if (!userAccuracyCircle) {
                            userAccuracyCircle = L.circle(latLng, {
                                radius: 10,
                                color: '#3B82F6',
                                fillColor: '#3B82F6',
                                fillOpacity: 0.15,
                                weight: 1
                            }).addTo(map);
                        } else {
                            userAccuracyCircle.setLatLng(latLng);
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL("https://localhost", htmlContent, "text/html", "UTF-8", null)
    }

    private fun loadKmlIntoMap() {
        try {
            val inputStream = requireContext().assets.open("UniFind.kml")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val kmlString = reader.use { it.readText() }
            val escapedKml = JSONObject.quote(kmlString)
            binding.webViewMap.evaluateJavascript("loadKmlData($escapedKml);", null)

            if (targetBuilding.isNotEmpty()) {
                val escapedTarget = JSONObject.quote(targetBuilding)
                binding.webViewMap.evaluateJavascript("focusBuilding($escapedTarget);", null)
                binding.cardRouteBanner.visibility = View.VISIBLE
                binding.tvRouteTitle.text = "🚶 Route to $targetBuilding"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkLocationPermissionAndStart() {
        val fineLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val fineLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation != PackageManager.PERMISSION_GRANTED && coarseLocation != PackageManager.PERMISSION_GRANTED) {
            updateGpsStatus("GPS Permission Required", "Please grant location permission.", "#B91C1C")
            return
        }

        try {
            updateGpsStatus("Acquiring GPS Location...", "Connecting to satellites...", "#D97706")

            locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
                .setMinUpdateIntervalMillis(2000)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location = locationResult.lastLocation ?: return
                    handleNewLocation(location)
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest!!,
                locationCallback,
                Looper.getMainLooper()
            )

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    handleNewLocation(location)
                }
            }
        } catch (e: SecurityException) {
            updateGpsStatus("GPS Error", "Security Exception: ${e.message}", "#B91C1C")
        } catch (e: Exception) {
            updateGpsStatus("GPS Error", "Error: ${e.message}", "#B91C1C")
        }
    }

    private fun handleNewLocation(location: Location) {
        lastUserLat = location.latitude
        lastUserLng = location.longitude

        if (isMapLoaded) {
            updateMapUserLocation(lastUserLat, lastUserLng)
        }

        val campusCenterLat = 14.06679
        val campusCenterLng = 120.62613

        val results = FloatArray(1)
        Location.distanceBetween(lastUserLat, lastUserLng, campusCenterLat, campusCenterLng, results)
        val distanceMeters = results[0]

        val maxAllowedMeters = 190f

        if (distanceMeters <= maxAllowedMeters) {
            updateGpsStatus(
                "🟢 Within Campus Parameter",
                "Distance to campus center: ${distanceMeters.toInt()}m (Inside/Within 10m)",
                "#15803D"
            )
        } else {
            val extraMeters = (distanceMeters - maxAllowedMeters).toInt()
            updateGpsStatus(
                "⚠️ Outside Campus Parameter!",
                "You are ~$extraMeters meters outside the 10m campus limit!",
                "#B91C1C"
            )
        }
    }

    private fun updateMapUserLocation(lat: Double, lng: Double) {
        val script = "updateUserLocation($lat, $lng);"
        binding.webViewMap.evaluateJavascript(script, null)
    }

    private fun updateGpsStatus(title: String, subtitle: String, colorHex: String) {
        if (_binding == null) return
        binding.tvGpsStatusTitle.text = title
        binding.tvGpsStatusSubtitle.text = subtitle
        try {
            binding.ivGpsIndicator.setColorFilter(Color.parseColor(colorHex))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        _binding = null
    }
}
