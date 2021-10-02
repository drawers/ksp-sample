package com.tsongkha.kspexample.consumer

import com.tsongkha.kspexample.annotation.IntSummable

fun main() {
    println(Foo(1,2).sumInts())
}

@IntSummable
data class Foo(
    val x: Int,
    val y: Int
)