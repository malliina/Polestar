package com.skogberglabs.polestar

import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarLocation
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.PlaceListNavigationTemplate

fun row(build: Row.Builder.() -> Unit): Row =
    Row.Builder().apply { build(this) }.build()
fun itemList(build: ItemList.Builder.() -> Unit): ItemList =
    ItemList.Builder().apply { build(this) }.build()
fun pane(build: Pane.Builder.() -> Unit): Pane =
    Pane.Builder().apply { build(this) }.build()
fun paneTemplate(pane: Pane, build: PaneTemplate.Builder.() -> Unit): PaneTemplate =
    PaneTemplate.Builder(pane).apply { build(this) }.build()
fun navigationTemplate(build: NavigationTemplate.Builder.() -> Unit) =
    NavigationTemplate.Builder().apply { build(this) }.build()
// fun mapTemplate(build: MapTemplate.Builder.() -> Unit) = ???
fun place(loc: CarLocation, build: Place.Builder.() -> Unit = {}): Place =
    Place.Builder(loc).apply { build(this) }.build()
fun placeMarker(build: PlaceMarker.Builder.() -> Unit) = PlaceMarker.Builder().apply(build).build()
fun placeListTemplate(build: PlaceListMapTemplate.Builder.() -> Unit): PlaceListMapTemplate =
    PlaceListMapTemplate.Builder().apply { build(this) }.build()
fun placeListNavigationTemplate(build: PlaceListNavigationTemplate.Builder.() -> Unit) =
    PlaceListNavigationTemplate.Builder().apply { build(this) }.build()
fun action(build: Action.Builder.() -> Unit) =
    Action.Builder().apply { build(this) }.build()
fun messageTemplate(explanation: String, build: MessageTemplate.Builder.() -> Unit): MessageTemplate =
    MessageTemplate.Builder(explanation).apply { build(this) }.build()
fun actionStrip(build: ActionStrip.Builder.() -> Unit) =
    ActionStrip.Builder().apply { build(this) }.build()
