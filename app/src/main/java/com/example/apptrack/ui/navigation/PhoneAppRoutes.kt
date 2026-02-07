package com.example.apptrack.ui.navigation

/**
 * Routes for the Phone app (Google Phone style).
 * Tabs: Recents, Contacts, Search.
 * Overlay: DialPad, CallDetails, ContactDetails.
 */
object PhoneAppRoutes {
    const val Recents = "recents"
    const val Contacts = "contacts"
    const val Search = "search"
    const val DialPad = "dial_pad"
    const val CallDetails = "call_details/{phoneNumber}"
    const val ContactDetails = "contact_details/{phoneNumber}"

    fun callDetails(phoneNumber: String): String = "call_details/$phoneNumber"
    fun contactDetails(phoneNumber: String): String = "contact_details/$phoneNumber"
}
