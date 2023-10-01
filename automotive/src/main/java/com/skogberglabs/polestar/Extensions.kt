package com.skogberglabs.polestar

import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarLocation
import androidx.car.app.model.GridItem
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.TabTemplate
import androidx.car.app.model.signin.ProviderSignInMethod
import androidx.car.app.model.signin.SignInTemplate
import androidx.car.app.navigation.model.MapTemplate
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.PlaceListNavigationTemplate

fun row(build: Row.Builder.() -> Unit): Row =
    Row.Builder().apply(build).build()
fun gridItem(build: GridItem.Builder.() -> Unit): GridItem =
    GridItem.Builder().apply(build).build()
fun itemList(build: ItemList.Builder.() -> Unit): ItemList =
    ItemList.Builder().apply(build).build()
fun ItemList.Builder.addRow(build: Row.Builder.() -> Unit): ItemList.Builder =
    addItem(row(build))
fun pane(build: Pane.Builder.() -> Unit): Pane =
    Pane.Builder().apply(build).build()
fun paneTemplate(pane: Pane, build: PaneTemplate.Builder.() -> Unit): PaneTemplate =
    PaneTemplate.Builder(pane).apply(build).build()
fun navigationTemplate(build: NavigationTemplate.Builder.() -> Unit) =
    NavigationTemplate.Builder().apply(build).build()
fun mapTemplate(build: MapTemplate.Builder.() -> Unit) =
    MapTemplate.Builder().apply(build).build()
fun place(loc: CarLocation, build: Place.Builder.() -> Unit = {}): Place =
    Place.Builder(loc).apply(build).build()
fun placeMarker(build: PlaceMarker.Builder.() -> Unit) = PlaceMarker.Builder().apply(build).build()
fun placeListTemplate(build: PlaceListMapTemplate.Builder.() -> Unit): PlaceListMapTemplate =
    PlaceListMapTemplate.Builder().apply(build).build()
fun placeListNavigationTemplate(build: PlaceListNavigationTemplate.Builder.() -> Unit) =
    PlaceListNavigationTemplate.Builder().apply(build).build()
fun action(build: Action.Builder.() -> Unit) =
    Action.Builder().apply(build).build()
fun messageTemplate(explanation: String, build: MessageTemplate.Builder.() -> Unit): MessageTemplate =
    MessageTemplate.Builder(explanation).apply(build).build()
fun actionStrip(build: ActionStrip.Builder.() -> Unit) =
    ActionStrip.Builder().apply(build).build()
fun signInTemplate(method: ProviderSignInMethod, build: SignInTemplate.Builder.() -> Unit) =
    SignInTemplate.Builder(method).apply(build).build()
fun listTemplate(build: ListTemplate.Builder.() -> Unit): ListTemplate =
    ListTemplate.Builder().apply(build).build()