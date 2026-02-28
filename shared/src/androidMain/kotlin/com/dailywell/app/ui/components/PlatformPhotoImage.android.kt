package com.dailywell.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
actual fun PlatformPhotoImage(
    model: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale
) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
