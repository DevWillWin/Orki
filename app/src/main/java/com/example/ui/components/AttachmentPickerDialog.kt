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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AmberPro
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AttachmentPickerDialog(
    currentPlan: String,
    dailyUploadUsage: Int,
    dailyUploadLimit: Int,
    onSelectImage: () -> Unit,
    onSelectPdf: () -> Unit,
    onSelectText: () -> Unit,
    onDismiss: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    val remaining = (dailyUploadLimit - dailyUploadUsage).coerceAtLeast(0)
    val isLimitReached = dailyUploadUsage >= dailyUploadLimit

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Add Attachment",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Images, text files, or PDFs",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Upload Quota Status Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLimitReached) Color(0x22EF4444) else DarkCanvas)
                        .border(
                            1.dp,
                            if (isLimitReached) Color(0x66EF4444) else DarkSurfaceBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$currentPlan Plan Uploads",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLimitReached) Color(0xFFF87171) else TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (currentPlan == "Pro") AmberPro.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = currentPlan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentPlan == "Pro") AmberPro else EmeraldAccent
                                    )
                                }
                            }
                            Text(
                                text = if (isLimitReached) {
                                    "Limit reached ($dailyUploadLimit/$dailyUploadLimit used). Upgrade to get more!"
                                } else {
                                    "$remaining of $dailyUploadLimit uploads remaining today"
                                },
                                fontSize = 10.sp,
                                color = if (isLimitReached) Color(0xFFFCA5A5) else TextSecondary
                            )
                        }

                        if (currentPlan != "Pro") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x3310B981))
                                    .clickable {
                                        onDismiss()
                                        onUpgradeClick()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Upgrade",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                        }
                    }
                }

                // File Type Options
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FileTypeOptionCard(
                        icon = Icons.Default.Image,
                        iconTint = EmeraldAccent,
                        iconBg = Color(0x2210B981),
                        title = "Images & Photos",
                        subtitle = "JPEG, PNG, WebP • Visual recognition & OCR",
                        badge = "Photo Picker",
                        onClick = {
                            onDismiss()
                            onSelectImage()
                        }
                    )

                    FileTypeOptionCard(
                        icon = Icons.Default.PictureAsPdf,
                        iconTint = Color(0xFFF87171),
                        iconBg = Color(0x22EF4444),
                        title = "PDF Document",
                        subtitle = "PDF • Multi-page document reasoning & summary",
                        badge = "Document",
                        onClick = {
                            onDismiss()
                            onSelectPdf()
                        }
                    )

                    FileTypeOptionCard(
                        icon = Icons.Default.Description,
                        iconTint = Color(0xFF38BDF8),
                        iconBg = Color(0x2238BDF8),
                        title = "Text File",
                        subtitle = "TXT, Markdown, JSON, Code • Text extraction",
                        badge = "Text / Code",
                        onClick = {
                            onDismiss()
                            onSelectText()
                        }
                    )
                }

                // Footer helper
                if (currentPlan == "Free") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x1F27272A))
                            .clickable {
                                onDismiss()
                                onUpgradeClick()
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = AmberPro,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Plus has 20 uploads & Pro has 100 uploads",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTypeOptionCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCanvas)
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x1F27272A))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
