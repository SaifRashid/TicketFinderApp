package com.example.ticketfinderapp

data class TicketMasterData(
    val _embedded: Embedded
)

data class Embedded(
    val events: List<Event>
)

data class Event(
    val name: String,
    val url: String,
    val images: List<Images>,
    val dates: Dates,
    val priceRanges: List<PriceRange>,
    val _embedded: Embedded2
)

data class Images(
    val url: String,
    val width: Int,
    val height: Int
)

data class Dates(
    val start: Start
)

data class Start(
    val localDate: String,
    val localTime: String
)

data class PriceRange(
    val min: Double,
    val max: Double
)

data class Embedded2(
    val venues: List<Venue>
)

data class Venue(
    val name: String,
    val city: City,
    val state: State,
    val country: Country,
    val address: Address,
    val id: String
)

data class City(
    val name: String
)

data class State(
    val stateCode: String
)

data class Country(
    val countryCode: String
)

data class Address(
    val line1: String
)