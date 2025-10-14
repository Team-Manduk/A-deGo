package com.teammanduk.adego.core.ui.permission

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

sealed interface PermissionState {
    data object Idle : PermissionState
    data object Granted : PermissionState
    data object DeniedTemporary : PermissionState
    data object DeniedPermanently : PermissionState
}

fun Array<String>.toPermissionState(
    context: Context,
    activity: Activity,
    requestedOnce: Boolean
): PermissionState {
    if (isAllGranted(context)) return PermissionState.Granted

    val anyRationale = any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }

    return when {
        !anyRationale && requestedOnce -> PermissionState.DeniedPermanently
        else -> PermissionState.DeniedTemporary
    }
}

private fun Array<String>.isAllGranted(context: Context): Boolean {
    return this.all { p ->
        ActivityCompat.checkSelfPermission(
            context,
            p
        ) == PackageManager.PERMISSION_GRANTED
    }
}
