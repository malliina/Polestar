package com.skogberglabs.polestar.ui

import android.content.Context
import com.skogberglabs.polestar.CarConf
import com.skogberglabs.polestar.JsonConf
import com.skogberglabs.polestar.R

object Previews {
    fun conf(ctx: Context): CarConf =
        ctx.resources.openRawResource(
            R.raw.conf,
        ).bufferedReader().use { JsonConf.decode(it.readText(), CarConf.serializer()) }

    fun lang(ctx: Context) = conf(ctx).languages.first()
}
