package com.example.data

import com.example.database.AppRuleEntity

data class AppItem(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val isSystemApp: Boolean,
    val hasInternetPermission: Boolean,
    val versionName: String,
    val rule: AppRuleEntity
)
