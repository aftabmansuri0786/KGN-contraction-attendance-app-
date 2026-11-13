package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.ConstructionOrange
import java.io.File

@Composable
fun WorkerAvatar(
    workerName: String,
    localPhotoPath: String = "",
    photoUrl: String = "",
    isActive: Boolean = true,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val localFile = remember(localPhotoPath) {
        localPhotoPath.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() }
    }
    val imageModel: Any? = localFile ?: photoUrl.takeIf { it.isNotBlank() }
    var loadFailed by remember(imageModel) { mutableStateOf(false) }

    val bgColor = if (isActive) ConstructionOrange.copy(alpha = 0.16f) else Color.LightGray.copy(alpha = 0.5f)
    val fgColor = if (isActive) ConstructionOrange else Color.DarkGray
    val borderColor = if (imageModel != null && !loadFailed) ConstructionOrange.copy(alpha = 0.45f) else fgColor.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (imageModel != null && !loadFailed) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageModel)
                    .crossfade(true)
                    .build(),
                contentDescription = "Photo of $workerName",
                contentScale = ContentScale.Crop,
                onError = { loadFailed = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val initial = workerName.trim().take(1).uppercase()
            if (initial.isNotBlank()) {
                Text(
                    text = initial,
                    color = fgColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.42f).sp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default Worker Avatar",
                    tint = fgColor,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}
