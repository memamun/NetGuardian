package com.example.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.firewall.FirewallState
import com.example.ui.theme.NetGuardianTheme
import com.example.ui.theme.AppSpacing

@Preview(name = "Compact light", widthDp = 360, showBackground = true)
@Preview(name = "Compact dark", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Large text", widthDp = 360, fontScale = 2f)
@Preview(name = "Tablet", widthDp = 840)
@Composable
private fun SearchEmptyPreview() {
    NetGuardianTheme {
        Surface {
            Column {
                AppTopBar("Connections", FirewallState.STOPPED)
                AppSearchField("", {}, "Search connections",
                    Modifier.fillMaxWidth().padding(AppSpacing.content))
                ScreenState("No connections to show", "Start protection to see connection activity.",
                    modifier = Modifier.fillMaxWidth(), icon = Icons.Default.Search)
            }
        }
    }
}

@Preview(name = "DNS form light", widthDp = 360)
@Preview(name = "DNS form dark", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DnsFormPreview() {
    NetGuardianTheme {
        Surface { DnsAddressField("192.168.1.1", {}, Modifier.padding(AppSpacing.content)) }
    }
}
