package com.example.firewall

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.DrawableRes
import java.util.concurrent.Executor

object TileHelper {

    fun isAddTilePromptSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun requestAddTile(
        context: Context,
        tileClass: Class<*>,
        label: CharSequence,
        @DrawableRes iconResId: Int,
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val statusBarManager = context.getSystemService(StatusBarManager::class.java)
            if (statusBarManager != null) {
                val executor: Executor = context.mainExecutor
                statusBarManager.requestAddTileService(
                    ComponentName(context, tileClass),
                    label,
                    Icon.createWithResource(context, iconResId),
                    executor
                ) { statusCode ->
                    onComplete(statusCode == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED)
                }
                return
            }
        }
        onComplete(false)
    }
}
