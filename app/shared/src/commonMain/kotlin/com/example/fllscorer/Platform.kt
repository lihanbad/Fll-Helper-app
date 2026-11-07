package com.example.fllscorer

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform