package com.skogberglabs.polestar

import android.content.Context

object Previews {
    fun conf(ctx: Context): CarConf =
        ctx.resources.openRawResource(R.raw.conf).bufferedReader().use { Adapters.carConf.fromJson(it.readText()) }!!
    fun lang(ctx: Context) = conf(ctx).languages.first()
}
