package com.example.ticketfinderapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface TicketMasterService {
    @GET("events")
    fun getData(@Query("apikey") apiKey: String, @Query("keyword") keyword: String, @Query("city") city: String, @Query("sort") sort: String): Call<TicketMasterData>

    @GET("events")
    fun getData(@Query("apikey") apiKey: String, @Query("keyword") keyword: String, @Query("city") city: String, @Query("sort") sort: String, @Query("venueId") venueId: String): Call<TicketMasterData>

    @GET("events")
    fun getFavorites(@Query("apikey") apiKey: String, @Query("sort") sort: String, @Query("id") favoriteEvents: ArrayList<String>): Call<TicketMasterData>

}