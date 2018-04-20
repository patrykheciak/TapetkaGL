package model.scene

data class Part(var trinagleIds: ArrayList<Int>,
                val R: Int,
                val GG: Int,
                val B: Int,
                val kd: Double,
                val ks: Double,
                val g: Double)