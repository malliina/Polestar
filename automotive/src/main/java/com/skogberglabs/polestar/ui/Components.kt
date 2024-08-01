package com.skogberglabs.polestar.ui

fun Double.formatted(n: Int): String = String.format("%.${n}f", this)

fun Float.formatted(n: Int): String = String.format("%.${n}f", this)
