package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import android.content.Intent
import android.widget.Toast
import com.example.data.network.ImageGenerationService
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ChatMessageEntity
import com.example.ui.components.bounceClick
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GreenBorder
import com.example.ui.theme.GreenBorderGlow
import com.example.ui.theme.GreenBright
import com.example.ui.theme.GreenHighlight
import com.example.ui.theme.GreenMuted
import com.example.ui.theme.GreenSurfaceElevated
import com.example.ui.theme.GreenSurfaceTint
import com.example.ui.theme.GreenTextMuted
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.formatFileSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    onPlayTts: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    if (isUser) {
        // User message: Sleek right-aligned rounded pill with layered neutral dark surface (Requirement 1, 3, 4)
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                // If attachment is present, render attachment header
                if (message.attachmentName != null) {
                    AttachmentBubbleCard(
                        name = message.attachmentName,
                        type = message.attachmentType,
                        size = message.attachmentSize,
                        uri = message.attachmentUri
                    )
                    if (message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    } else {
        // Assistant message: Full-width flowing ChatGPT/Claude style layout
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Header: Avatar + Model Name + Monogram
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkSurfaceBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Orki AI",
                        tint = TextPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Orki AI",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkBorderSubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Assistant",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // If assistant generated an image, display the artwork card
            if (message.attachmentUri != null && (message.attachmentType == "image" || message.attachmentType == "generated_image")) {
                GeneratedImageAssistantCard(
                    imageUri = message.attachmentUri,
                    prompt = message.text
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // If assistant generated a video, display the video card
            if (message.attachmentUri != null && message.attachmentType == "generated_video") {
                GeneratedVideoAssistantCard(
                    videoUri = message.attachmentUri,
                    thumbnailUrl = message.attachmentName,
                    prompt = message.text
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Message text body formatted with clean typography
            val formattedText = formatMarkdownText(message.text)
            Text(
                text = formattedText,
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
            )

            // Bottom Action Bar: Listen & Copy buttons with spring bounce feedback
            if (message.text.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // Listen Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                            .bounceClick {
                                onPlayTts(message.ttsText ?: message.text)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Listen",
                            tint = TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Listen",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Copy Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                            .bounceClick {
                                clipboardManager.setText(AnnotatedString(message.text))
                                isCopied = true
                                scope.launch {
                                    delay(2000)
                                    isCopied = false
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy text",
                            tint = if (isCopied) GreenHighlight else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCopied) "Copied" else "Copy",
                            color = if (isCopied) GreenHighlight else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// Markdown parser for bold and code spans
fun formatMarkdownText(raw: String) = buildAnnotatedString {
    var i = 0
    val len = raw.length
    while (i < len) {
        when {
            // Bold **text**
            raw.startsWith("**", i) -> {
                val end = raw.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                        append(raw.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append(raw[i])
                    i++
                }
            }
            // Inline code `code`
            raw.startsWith("`", i) && !raw.startsWith("```", i) -> {
                val end = raw.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0x33000000),
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    ) {
                        append(raw.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(raw[i])
                    i++
                }
            }
            else -> {
                append(raw[i])
                i++
            }
        }
    }
}

@Composable
private fun AttachmentBubbleCard(
    name: String,
    type: String?,
    size: Long?,
    uri: String?
) {
    val formattedSize = if (size != null && size > 0) formatFileSize(size) else null

    when (type) {
        "image" -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
            ) {
                if (uri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(DarkCanvas),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (formattedSize != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedSize,
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
        "pdf" -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1715))
                    .border(1.dp, Color(0x55EF4444), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x33EF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF",
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (formattedSize != null) "PDF Document • $formattedSize" else "PDF Document",
                        fontSize = 11.sp,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }
        }
        else -> {
            // Text file
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0x5538BDF8), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x3338BDF8)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = "Text file",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (formattedSize != null) "Text File • $formattedSize" else "Text Document",
                        fontSize = 11.sp,
                        color = Color(0xFFBAE6FD)
                    )
                }
            }
        }
    }
}

@Composable
fun GeneratedImageAssistantCard(
    imageUri: String,
    prompt: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imageService = remember { ImageGenerationService(context) }
    var showFullPreview by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCanvas)
            .border(1.dp, GreenBorder, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clickable { showFullPreview = true }
        ) {
            AsyncImage(
                model = imageUri,
                contentDescription = prompt,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )

            // Zoom hint badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0x99000000))
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInFull,
                    contentDescription = "Expand image",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Bottom action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = GreenHighlight,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Generated with Orki AI Worker",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Save to Gallery
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(8.dp))
                        .clickable {
                            scope.launch {
                                val file = File(imageUri)
                                if (file.exists()) {
                                    val res = imageService.saveImageToGallery(file, prompt)
                                    res.onSuccess {
                                        isSaved = true
                                        Toast.makeText(context, "Saved to Pictures / Orki AI!", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        Toast.makeText(context, "Could not save: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Image file not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.Check else Icons.Default.Download,
                        contentDescription = "Save image",
                        tint = if (isSaved) GreenHighlight else TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSaved) "Saved" else "Save",
                        color = if (isSaved) GreenHighlight else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Share
                IconButton(
                    onClick = {
                        val file = File(imageUri)
                        if (file.exists()) {
                            scope.launch {
                                val uriRes = imageService.saveImageToGallery(file, prompt)
                                uriRes.onSuccess { uri ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, "Generated by Orki AI: $prompt")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }

    if (showFullPreview) {
        Dialog(onDismissRequest = { showFullPreview = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCanvas)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
                    .padding(8.dp)
            ) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = prompt,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                )
                IconButton(
                    onClick = { showFullPreview = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close preview",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
