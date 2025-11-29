package com.skogberglabs.polestar

import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarLocation
import androidx.car.app.model.GridItem
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Metadata
import androidx.car.app.model.OnClickListener
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.signin.ProviderSignInMethod
import androidx.car.app.model.signin.SignInTemplate
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.navigation.model.NavigationTemplate

fun Coord.carLocation(): CarLocation = CarLocation.create(lat, lng)

fun row(build: Row.Builder.() -> Unit): Row = Row.Builder().apply(build).build()

fun metadata(build: Metadata.Builder.() -> Unit): Metadata = Metadata.Builder().apply(build).build()

fun gridItem(build: GridItem.Builder.() -> Unit): GridItem = GridItem.Builder().apply(build).build()

fun itemList(build: ItemList.Builder.() -> Unit): ItemList = ItemList.Builder().apply(build).build()

fun ItemList.Builder.addRow(build: Row.Builder.() -> Unit): ItemList.Builder = addItem(row(build))

fun pane(build: Pane.Builder.() -> Unit): Pane = Pane.Builder().apply(build).build()

fun Pane.Builder.installRow(title: String) =
    addRow(row { setTitle(title) })

fun Pane.Builder.installAction(title: String, isPrimary: Boolean, listener: OnClickListener) =
    addAction(action {
        setTitle(title)
        setOnClickListener(listener)
        if (isPrimary) {
            setFlags(Action.FLAG_PRIMARY)
        }
    })

fun paneTemplate(
    pane: Pane,
    build: PaneTemplate.Builder.() -> Unit,
): PaneTemplate = PaneTemplate.Builder(pane).apply(build).build()

fun PaneTemplate.Builder.installHeader(build: Header.Builder.() -> Unit) =
    setHeader(header(build))

fun navigationTemplate(build: NavigationTemplate.Builder.() -> Unit) = NavigationTemplate.Builder().apply(build).build()

fun place(
    loc: CarLocation,
    build: Place.Builder.() -> Unit = {},
): Place = Place.Builder(loc).apply(build).build()

fun placeMarker(build: PlaceMarker.Builder.() -> Unit) = PlaceMarker.Builder().apply(build).build()

fun placeListTemplate(build: PlaceListMapTemplate.Builder.() -> Unit): PlaceListMapTemplate =
    PlaceListMapTemplate.Builder().apply(build).build()

fun mapWithContentTemplate(build: MapWithContentTemplate.Builder.() -> Unit): MapWithContentTemplate =
    MapWithContentTemplate.Builder().apply(build).build()

fun action(build: Action.Builder.() -> Unit) = Action.Builder().apply(build).build()

fun titledAction(
    title: String,
    make: Action.Builder.() -> Unit,
) = action {
    setTitle(title)
    make()
}

fun MessageTemplate.Builder.appendAction(
    title: String,
    make: Action.Builder.() -> Unit,
) = addAction(titledAction(title) { make() })

fun MessageTemplate.Builder.installHeader(build: Header.Builder.() -> Unit) =
    setHeader(header(build))

fun messageTemplate(
    message: String,
    build: MessageTemplate.Builder.() -> Unit,
): MessageTemplate = MessageTemplate.Builder(message).apply(build).build()

fun actionStrip(build: ActionStrip.Builder.() -> Unit) = ActionStrip.Builder().apply(build).build()

fun signInTemplate(
    method: ProviderSignInMethod,
    build: SignInTemplate.Builder.() -> Unit,
) = SignInTemplate.Builder(method).apply(build).build()

fun listTemplate(build: ListTemplate.Builder.() -> Unit): ListTemplate = ListTemplate.Builder().apply(build).build()

fun ListTemplate.Builder.installHeader(build: Header.Builder.() -> Unit) =
    setHeader(header(build))

fun header(build: Header.Builder.() -> Unit): Header = Header.Builder().apply(build).build()
