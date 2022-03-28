package com.gugact.dummyobjectgenerator

data class Dummy(val name: String, val dummyMap: Map<String, List<SubDummy>>)

data class SubDummy(val name: String, val age: Int)