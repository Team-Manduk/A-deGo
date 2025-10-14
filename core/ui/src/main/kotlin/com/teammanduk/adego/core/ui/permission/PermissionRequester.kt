package com.teammanduk.adego.core.ui.permission

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun PermissionRequester(
    permissionTypes: List<PermissionType>,
    onGranted: () -> Unit = {},
    onDenied: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val permissions = permissionTypes.toPermissionArray()
    val message = permissionTypes.toPermissionMessage()

    val context = LocalContext.current
    val activity = context as? Activity ?: run {
        content()
        return
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAlreadyRequest by rememberSaveable { mutableStateOf(false) }
    var state by remember { mutableStateOf<PermissionState>(PermissionState.Idle) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        isAlreadyRequest = true

        state = when {
            result.values.all { it } -> PermissionState.Granted
            else -> permissions.toPermissionState(context, activity, isAlreadyRequest)
        }
    }

    LaunchedEffect(Unit) {
        val current = permissions.toPermissionState(context, activity, isAlreadyRequest)

        if (current is PermissionState.Granted) {
            state = PermissionState.Granted
        } else {
            if (isAlreadyRequest.not()) {
                launcher.launch(permissions)
            } else {
                state = current
            }
        }
    }

    DisposableEffect(lifecycleOwner, isAlreadyRequest) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isAlreadyRequest) {
                state = permissions.toPermissionState(context, activity, requestedOnce = true)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state) {
        when (state) {
            is PermissionState.Granted -> onGranted()
            is PermissionState.DeniedTemporary,
            is PermissionState.DeniedPermanently -> onDenied()
            else -> {}
        }
    }

    when (state) {
        PermissionState.Granted -> {
            content()
        }

        PermissionState.DeniedTemporary -> {
            PermissionRationaleDialog(
                title = message.title,
                message = message.text,
                onDismiss = onDenied,
                onRequestAgain = { launcher.launch(permissions) }
            )
        }

        PermissionState.DeniedPermanently -> {
            GoToSettingsDialog(
                title = message.title,
                message = permissionTypes.firstOrNull()?.settingsText
                    ?: "설정에서 권한을 허용해주세요.",
                onDismiss = onDenied,
                onOpenSettings = {
                    val intent = Intent(
                        ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                    context.startActivity(intent)
                }
            )
        }

        PermissionState.Idle -> {}
    }
}
