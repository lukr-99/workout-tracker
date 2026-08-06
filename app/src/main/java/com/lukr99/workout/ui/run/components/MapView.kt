package com.lukr99.workout.ui.run.components

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lukr99.workout.data.map.MapStyle
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.engine.LocationEngineDefault
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * Provider-agnostic map surface for Run Mode. The **only** place screens touch a map SDK — every run
 * screen (live, detail, planner) renders through here, so the map provider (currently MapLibre) or
 * tile source ([MapStyle]) can be swapped without touching UI code.
 *
 * R0 responsibility: render the dark vector basemap and, once fine-location is granted, show the
 * user's location (blue dot) and **follow** it. Recording the trace polyline arrives in R1.
 *
 * @param userLocationEnabled true once `ACCESS_FINE_LOCATION` is granted — gates the location layer.
 * @param recenterSignal increment to snap the camera back to a location-tracking follow mode.
 */
@Composable
fun RunMap(
    userLocationEnabled: Boolean,
    modifier: Modifier = Modifier,
    styleUrl: String = MapStyle.DARK_VECTOR_STYLE_URL,
    recenterSignal: Int = 0,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Holds the async-created map/style so effects can act on them once ready.
    val holder = remember { MapHolder() }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                holder.map = map
                map.setStyle(Style.Builder().fromUri(styleUrl)) { style ->
                    holder.style = style
                    if (userLocationEnabled) holder.enableLocation(context)
                }
            }
        }
    }

    // Forward the host lifecycle to the MapView (MapLibre requires this for GL + location updates).
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // Turn the location layer on when permission is granted (idempotent; safe before the map loads).
    LaunchedEffect(userLocationEnabled) {
        if (userLocationEnabled) holder.enableLocation(context)
    }

    // Recenter/follow when asked.
    LaunchedEffect(recenterSignal) {
        if (recenterSignal > 0) holder.recenter()
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** Mutable async holder — the map and style arrive after `getMapAsync` / `setStyle` callbacks. */
private class MapHolder {
    var map: MapLibreMap? = null
    var style: Style? = null
    private var locationActive = false

    @SuppressLint("MissingPermission")
    fun enableLocation(context: android.content.Context) {
        val map = map ?: return
        val style = style ?: return
        if (!style.isFullyLoaded) return
        val component = map.locationComponent
        if (!locationActive) {
            component.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style)
                    .locationEngine(LocationEngineDefault.getDefaultLocationEngine(context))
                    .build(),
            )
            locationActive = true
        }
        component.isLocationComponentEnabled = true
        component.cameraMode = CameraMode.TRACKING
        component.renderMode = RenderMode.COMPASS
        component.lastKnownLocation?.let {
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), MapStyle.FOLLOW_ZOOM),
            )
        }
    }

    fun recenter() {
        val component = map?.locationComponent ?: return
        if (!locationActive) return
        component.cameraMode = CameraMode.TRACKING
        component.zoomWhileTracking(MapStyle.FOLLOW_ZOOM)
    }
}
