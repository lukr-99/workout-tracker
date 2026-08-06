package com.lukr99.workout.ui.run

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.run.components.RunMap

/**
 * Live-run screen — **R0 stub**: it renders the dark map and follows your location (blue dot +
 * recenter). No recording yet; the foreground GPS service, growing polyline, metrics, and
 * pause/resume/finish land in R1. Fine location is requested just-in-time with a rationale, and
 * `POST_NOTIFICATIONS` is requested alongside so R1's ongoing run notification is ready.
 *
 * @param units carried through for the R1 metrics (km/mi); unused by the stub beyond plumbing.
 */
@Composable
fun LiveRunScreen(
    units: UnitSystem,
    onClose: () -> Unit,
) {
    val context = LocalContext.current

    fun hasFineLocation(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    var locationGranted by remember { mutableStateOf(hasFineLocation()) }
    var recenterSignal by remember { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        locationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true || hasFineLocation()
    }

    fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        RunMap(
            userLocationEnabled = locationGranted,
            recenterSignal = recenterSignal,
            modifier = Modifier.fillMaxSize(),
        )

        // Top row: close + a small "stub" marker so it's clear recording is not live yet.
        CircleIconButton(Icons.Rounded.Close, "Close", Modifier.align(Alignment.TopStart).padding(12.dp), onClose)
        StubBadge(Modifier.align(Alignment.TopCenter).padding(top = 16.dp))

        if (locationGranted) {
            CircleIconButton(
                Icons.Rounded.MyLocation, "Recenter",
                Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) { recenterSignal++ }
        } else {
            LocationRationale(
                onEnable = ::requestPermissions,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.size(44.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun StubBadge(modifier: Modifier) {
    Text(
        "Map preview · recording arrives in R1",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun LocationRationale(onEnable: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(
                "  Location for your run",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            "Ember uses your location to draw your route on the map and measure distance and pace. " +
                "It only tracks while a run is active — never in the background.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onEnable)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Enable location",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
