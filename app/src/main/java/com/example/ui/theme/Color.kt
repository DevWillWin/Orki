package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Layered Dark Theme: 3 near-black tonal surfaces with organic green depth
val DarkCanvas = Color(0xFF0D0F0D)          // Canvas / screen background (#0D0F0D)
val DarkSurface = Color(0xFF161A16)         // Cards, drawer, content surfaces (#161A16)
val DarkSurfaceVariant = Color(0xFF1F241F)  // Elevated elements, inputs, capsule (#1F241F)
val DarkSurfaceElevated = Color(0xFF262C26) // Higher elevation dialogs, popups
val DarkElevated = Color(0xFF1F241F)        // Elevated elements alias (#1F241F)
val DarkDeepElevated = Color(0xFF262C26)    // Deep elevated cards
val DarkSurfaceBorder = Color(0xFF232A23)   // Subtle surface separation border
val DarkBorderSubtle = Color(0xFF1B201B)    // Hairline divider

// Multiple shades of green for balanced, non-monotone accents
val GreenBright = Color(0xFF10B981)         // Primary action / bright highlight (#10B981)
val GreenHighlight = Color(0xFF34D399)      // Vibrant glow, active indicators (#34D399)
val GreenPrimaryDark = Color(0xFF059669)    // Containers / pressed state
val GreenMuted = Color(0xFF52796F)          // Secondary elements, desaturated green
val GreenBorder = Color(0xFF243B2E)         // Green-tinted border for cards
val GreenBorderGlow = Color(0x6610B981)     // Subtle green glow for cards / focus
val GreenSurfaceTint = Color(0xFF131D16)    // Tinted dark-green background for suggestion cards
val GreenSurfaceElevated = Color(0xFF19251D)// Elevated suggestion card background
val GreenTextMuted = Color(0xFF86A391)      // Muted desaturated green caption text

// Aliases for backwards compatibility
val EmeraldPrimary = GreenBright
val EmeraldPrimaryDark = GreenPrimaryDark
val EmeraldAccent = GreenHighlight

// Tier accent colors
val AmberPro = Color(0xFFF59E0B)
val AmberProLight = Color(0xFFFDE68A)

// Privacy mode
val PurpleIncognito = Color(0xFF9333EA)
val PurpleIncognitoLight = Color(0xFFD8B4FE)

// Typography colors
val TextPrimary = Color(0xFFF4F6F4)
val TextSecondary = Color(0xFFA1ABA3)
val TextMuted = Color(0xFF6F7972)

val ErrorRed = Color(0xFFEF4444)
val ErrorRedDark = Color(0xFF3B1212)
