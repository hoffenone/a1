package com.example.hf_a1.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface LottoService {
    @GET("/common.do?method=getLottoNumber")
    fun getWinningNumbers(@Query("drwNo") round: Int): Call<LottoResponse>
}

data class LottoResponse(
    val drwNo: Int,          // 회차
    val drwNoDate: String,   // 추첨일
    val drwtNo1: Int,        // 당첨번호 1
    val drwtNo2: Int,        // 당첨번호 2
    val drwtNo3: Int,        // 당첨번호 3
    val drwtNo4: Int,        // 당첨번호 4
    val drwtNo5: Int,        // 당첨번호 5
    val drwtNo6: Int,        // 당첨번호 6
    val bnusNo: Int,         // 보너스 번호
    val firstWinamnt: Long   // 1등 당첨금
) 