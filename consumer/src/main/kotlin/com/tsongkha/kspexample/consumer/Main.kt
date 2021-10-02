package com.tsongkha.kspexample.consumer

import com.tsongkha.kspexample.annotation.IntSummable

fun main() {
    println("hello")
    val foo = Foo(1,2)
//    foo.sumInts
}

data class Hello(
    val int: Int
)

@IntSummable
data class Foo(
    val x: Int,
    val y: Int
)