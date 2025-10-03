package com.teammanduk.adego.core.designsystem.ui.theme

import androidx.compose.ui.graphics.Color

// Main colors
val Main100 = Color(0xFFB5EBD7)
val Main500 = Color(0xFF3ECF8E)
val Main900 = Color(0xFF1F7A50)

val OnMain100 = Color(0xFF2D6A4F)
val OnMain500 = Color(0xFFFFFFFF)
val OnMain900 = Color(0xFFFFFFFF)

// Highlight colors
val Highlight100 = Color(0xFFB3D9FF)
val Highlight500 = Color(0xFF4A9FFF)
val Highlight900 = Color(0xFF1E5A8E)

val OnHighlight100 = Color(0xFF1E4D7B)
val OnHighlight500 = Color(0xFFFFFFFF)
val OnHighlight900 = Color(0xFFFFFFFF)

// Error colors
val Error100 = Color(0xFFFFB4B4)
val Error500 = Color(0xFFFF6B6B)
val Error900 = Color(0xFF8B0000)

val OnError100 = Color(0xFF8B0000)
val OnError500 = Color(0xFFFFFFFF)
val OnError900 = Color(0xFFFFFFFF)

// Background & Surface
val Background = Color(0xFFE8F5F0)
val OnBackground = Color(0xFF000000)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF000000)

// Line colors
val Line100 = Color(0xFFE8E8E8)
val Line500 = Color(0xFFB0B0B0)
val Line900 = Color(0xFF707070)

interface AdegoColor {
    val main100: Color
    val main500: Color
    val main900: Color
    val onMain100: Color
    val onMain500: Color
    val onMain900: Color

    val highlight100: Color
    val highlight500: Color
    val highlight900: Color
    val onHighlight100: Color
    val onHighlight500: Color
    val onHighlight900: Color

    val error100: Color
    val error500: Color
    val error900: Color
    val onError100: Color
    val onError500: Color
    val onError900: Color

    val background: Color
    val onBackground: Color
    val surface: Color
    val onSurface: Color

    val line100: Color
    val line500: Color
    val line900: Color
}

internal object AdegoLightColor : AdegoColor {
    override val main100 = Main100
    override val main500 = Main500
    override val main900 = Main900
    override val onMain100 = OnMain100
    override val onMain500 = OnMain500
    override val onMain900 = OnMain900

    override val highlight100 = Highlight100
    override val highlight500 = Highlight500
    override val highlight900 = Highlight900
    override val onHighlight100 = OnHighlight100
    override val onHighlight500 = OnHighlight500
    override val onHighlight900 = OnHighlight900

    override val error100 = Error100
    override val error500 = Error500
    override val error900 = Error900
    override val onError100 = OnError100
    override val onError500 = OnError500
    override val onError900 = OnError900

    override val background = Background
    override val onBackground = OnBackground
    override val surface = Surface
    override val onSurface = OnSurface

    override val line100 = Line100
    override val line500 = Line500
    override val line900 = Line900
}