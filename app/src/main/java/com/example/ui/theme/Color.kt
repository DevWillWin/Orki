package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Layered Dark Neutral Theme: 4 tonal surfaces with real depth (Requirement 4)
val DarkCanvas = Color(0xFF0F1012)          // Layer 1: App screen canvas background (#0F1012)
val DarkSurface = Color(0xFF16181B)         // Layer 2: Modal, drawer, surface dialogs (#16181B)
val DarkSurfaceVariant = Color(0xFF1E2126)  // Layer 3: Default cards, containers, composer (#1E2126)
val DarkSurfaceElevated = Color(0xFF262A30) // Layer 4: Inputs, nested pills, elevated bars (#262A30)
val DarkElevated = Color(0xFF262A30)        // Elevated elements alias (#262A30)
val DarkDeepElevated = Color(0xFF2E333C)    // Deep elevated cards
val DarkSurfaceBorder = Color(0xFF2C313A)   // Neutral card & container border (#2C313A)
val DarkBorderSubtle = Color(0xFF21252C)    // Hairline divider

// Green Accent: Restrained for primary CTAs and active/selected states only (Requirement 1 & 3)
val GreenBright = Color(0xFF10B981)         // Primary action / CTA fill (#10B981)
val GreenHighlight = Color(0xFF34D399)      // Active state indicator, selected radio (#34D399)
val GreenPrimaryDark = Color(0xFF059669)    // Pressed CTA state
val GreenMuted = Color(0xFF6B7280)          // Neutral muted gray for secondary icons/indicators
val GreenBorder = Color(0xFF2C313A)         // Neutral border alias to eliminate green border overuse
val GreenBorderGlow = Color(0x6610B981)     // Green outline reserved strictly for actively selected items
val GreenSurfaceTint = Color(0xFF14241B)    // Subtle tint for actively selected items ONLY
val GreenSurfaceElevated = Color(0xFF1B2F23)// Elevated tint for actively selected items ONLY
val GreenTextMuted = Color(0xFF9CA3AF)      // Neutral muted gray for subtitles/captions (Requirement 2)

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

// Clean Neutral Typography colors (Requirement 2)
val TextPrimary = Color(0xFFF3F4F6)         // Crisp primary white (#F3F4F6)
val TextSecondary = Color(0xFF9CA3AF)       // Muted gray for secondary descriptions/badges (#9CA3AF)
val TextMuted = Color(0xFF6B7280)           // Muted gray for captions & hints (#6B7280)

val ErrorRed = Color(0xFFEF4444)
val ErrorRedDark = Color(0xFF3B1212)
