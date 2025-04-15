package com.example.hf_a1.model

data class LottoNumber(
    val numbers: List<Int>,
    val timestamp: Long = System.currentTimeMillis(),
    var matchCount: Int = 0,  // 맞은 개수
    var hasBonusMatch: Boolean = false  // 보너스 번호 일치 여부
) 