package com.example.ticketfinderapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TicketMasterService {
    // https://app.ticketmaster.com/discovery/v2/events.json?apikey=Ayl9reEX22B94IUU01mLxM89kAE1H6ia
    // https://app.ticketmaster.com/discovery/v2/events.json?keyword=music&city=hartford&apikey=Ayl9reEX22B94IUU01mLxM89kAE1H6ia

    @GET(".")
    fun getData(@Query("apikey") apiKey: String, @Query("keyword") keyword: String, @Query("city") city: String, @Query("sort") sort: String): Call<TicketMasterData>
}