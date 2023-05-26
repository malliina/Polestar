package com.skogberglabs.polestar.ui

import android.content.Context
import com.skogberglabs.polestar.Adapters
import com.skogberglabs.polestar.CarConf
import com.skogberglabs.polestar.R

object Previews {
    fun conf(ctx: Context): CarConf =
        ctx.resources.openRawResource(R.raw.conf).bufferedReader().use { Adapters.carConf.fromJson(it.readText()) }!!
    fun lang(ctx: Context) = conf(ctx).languages.first()
}
