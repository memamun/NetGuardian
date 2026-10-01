package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.R
import com.example.ui.theme.AppSpacing

internal fun isIpv4Address(value: String): Boolean {
    val parts = value.trim().split('.')
    return parts.size == 4 && parts.all { part ->
        part.isNotEmpty() && part.length <= 3 && part.all { it in '0'..'9' } &&
            part.toIntOrNull() in 0..255
    }
}

@Composable
fun DnsAddressField(address: String, onSave: (String) -> Unit, modifier: Modifier = Modifier) {
    var draft by rememberSaveable(address) { mutableStateOf(address) }
    var attempted by rememberSaveable(address) { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val save = {
        attempted = true
        if (isIpv4Address(draft)) {
            onSave(draft.trim())
            focus.clearFocus()
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it; attempted = false },
            label = { Text(stringResource(R.string.dns_address)) },
            supportingText = {
                Text(stringResource(if (attempted && !isIpv4Address(draft))
                    R.string.dns_address_error else R.string.dns_address_hint))
            },
            isError = attempted && !isIpv4Address(draft),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("custom_dns_ip_field")
        )
        FilledTonalButton(onClick = save, enabled = draft.trim() != address,
            modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.save_dns))
        }
    }
}
