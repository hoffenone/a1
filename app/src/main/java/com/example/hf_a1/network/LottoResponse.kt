package com.example.hf_a1.network

data class LottoResponse(
    val drwNo: Int,           // 회차
    val drwNoDate: String,    // 추첨일
    val drwtNo1: Int,         // 당첨번호 1
    val drwtNo2: Int,         // 당첨번호 2
    val drwtNo3: Int,         // 당첨번호 3
    val drwtNo4: Int,         // 당첨번호 4
    val drwtNo5: Int,         // 당첨번호 5
    val drwtNo6: Int,         // 당첨번호 6
    val bnusNo: Int,          // 보너스 번호
    val firstWinamnt: Long,   // 1등 당첨금
    val firstPrzwnerCo: Int,  // 1등 당첨자 수
    val returnValue: String   // 응답 상태
) 