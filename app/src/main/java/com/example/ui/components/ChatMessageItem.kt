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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
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
        // User message: Sleek right-aligned rounded pill with signature emerald tint (like ChatGPT/Claude)
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
                    .background(Color(0xFF064E3B))
                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
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
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0C241B))
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Orki AI",
                        tint = EmeraldAccent,
                        modifier = Modifier.size(14.dp)
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
                        .background(EmeraldPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Assistant",
                        color = EmeraldAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Message text body formatted with clean typography
            val formattedText = formatMarkdownText(message.text)
            Text(
                text = formattedText,
                color = Color(0xFFF3F4F6),
                fontSize = 15.sp,
                lineHeight = 24.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
            )

            // Bottom Action Bar: Listen & Copy buttons (ChatGPT/Claude style)
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
                            .background(Color(0x2210B981))
                            .clickable {
                                onPlayTts(message.ttsText ?: message.text)
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Listen",
                            tint = EmeraldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Listen",
                            color = EmeraldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Copy Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                clipboardManager.setText(AnnotatedString(message.text))
                                isCopied = true
                                scope.launch {
                                    delay(2000)
                                    isCopied = false
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy text",
                            tint = if (isCopied) EmeraldAccent else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isCopied) "Copied" else "Copy",
                            color = if (isCopied) EmeraldAccent else TextSecondary,
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
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = EmeraldAccent)) {
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
                            color = EmeraldAccent,
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
                    .background(Color(0xFF042F2E))
                    .border(1.dp, Color(0x3310B981), RoundedCornerShape(12.dp))
            ) {
                if (uri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .background(Color(0xFF021E1D)),
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
                        tint = EmeraldAccent,
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
                            color = Color(0xFFA7F3D0)
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
