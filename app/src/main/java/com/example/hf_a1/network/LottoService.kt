package com.example.hf_a1.network

import retrofit2.http.GET
import retrofit2.http.Query

interface LottoService {
    @GET("common.do?method=getLottoNumber")
    suspend fun getWinningNumbers(
        @Query("gameNo") gameNo: String = "6556",  // 로또6/45
        @Query("drwNo") drwNo: String
    ): LottoResponse
} 