package com.skogberglabs.polestar

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.assistant.appactions.testing.aatl.AppActionsTestManager
import com.google.assistant.appactions.testing.aatl.fulfillment.AppActionsFulfillmentIntentResult
import com.google.assistant.appactions.testing.aatl.fulfillment.FulfillmentType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppActionIntentTest {
    private lateinit var aatl: AppActionsTestManager

    private val appId = "com.skogberglabs.polestar"

    @Before
    fun init() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val lookupPackages = listOf(appId)
        aatl = AppActionsTestManager(appContext, lookupPackages)
    }

    @Test
    fun handleAppActionIntent() {
        val intentParams =
            mapOf(
                "parkingFacility.name" to "street",
                "parkingFacility.geo.latitude" to "60",
                "parkingFacility.geo.longitude" to "24",
            )
        val intentName = "actions.intent.GET_PARKING_FACILITY"
        val result = aatl.fulfill(intentName, intentParams)
        assertEquals(FulfillmentType.INTENT, result.fulfillmentType)
        val intentResult = result as AppActionsFulfillmentIntentResult
        val intent = intentResult.intent
        assertEquals("https", intent.scheme)
        assertEquals("https://www.car-map.com?name=street&latitude=60&longitude=24", intent.dataString)
    }
}
