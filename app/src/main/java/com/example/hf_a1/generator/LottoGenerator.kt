package com.example.hf_a1.generator

class LottoGenerator {
    fun generateMultipleSets(count: Int): List<List<Int>> {
        return List(count) { generateSingleSet() }
    }

    private fun generateSingleSet(): List<Int> {
        return (1..45).shuffled().take(6).sorted()
    }
} 