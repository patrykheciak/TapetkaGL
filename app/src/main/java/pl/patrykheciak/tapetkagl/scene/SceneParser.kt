package model.scene

import pl.patrykheciak.tapetkagl.scene.Vec3d


class SceneParser {
    companion object {
        public fun parse(lines: ArrayList<String>): Scene {

            val n = lines[0].toInt()
            val vertices = Array(n, { _ -> DoubleArray(3) })
            for (i: Int in 1..n) {
                val coords = lines[i].split(' ')
                vertices[i - 1][0] = coords[0].toDouble()
                vertices[i - 1][1] = coords[1].toDouble()
                vertices[i - 1][2] = coords[2].toDouble()
            }

            val m = lines[n + 1].toInt()
            val traingles = Array(m, { _ -> IntArray(3) })
            var licznikTrojkatow = 0
            for (i: Int in 2 + n..2 + n + m - 1) {
                val indices = lines[i].split(' ')
                traingles[licznikTrojkatow][0] = indices[0].toInt()
                traingles[licznikTrojkatow][1] = indices[1].toInt()
                traingles[licznikTrojkatow][2] = indices[2].toInt()
                licznikTrojkatow++
            }


            val parts = ArrayList<Part>()
            val k = lines[n + 2 * m + 2].toInt()
            for (i: Int in 0..k - 1) {
                val surfacePara = lines[n + 2 * m + 2 + 1 + i].split(' ')
                parts.add(Part(
                        ArrayList<Int>(),
                        surfacePara[0].toInt(),
                        surfacePara[1].toInt(),
                        surfacePara[2].toInt(),
                        surfacePara[3].toDouble(),
                        surfacePara[4].toDouble(),
                        surfacePara[5].toDouble()))
            }

            for (i: Int in 2 + n + m..2 + n + 2 * m - 1) {
                val partIndexOfTriangle = lines[i].toInt()
                val traingleInd = i - (2 + n + m)
                parts[partIndexOfTriangle].trinagleIds.add(traingleInd)
            }

            // fixing verticies order in triangle for some parts
            for (i in 0 until parts.size) {
                if (false
//                        || i == 0 // tlo obrazka
                        || i == 1 // czerwo detal
                        || i == 2 // rama obrazka    moze
                        || i == 3 // drewno
                        || i == 4 // ?
//                        || i == 5 // skora miala    pomieszana konwencja. I tak zle i tak niedobrze
                        || i == 6 // OK
//                        || i == 7 // sciany NO
//                        || i == 8  // drobny bialy detal obrazka
                        || i == 10 // zaslony

//                    || i == 11 // podloga
                ) {
                    val part = parts[i]
                    for (trinagleId in part.trinagleIds) {
                        val verticiesIndexes = traingles[trinagleId]
                        val wasAt0 = verticiesIndexes[0]
                        verticiesIndexes[0] = verticiesIndexes[2]
                        verticiesIndexes[2] = wasAt0
                    }
                }
            }


            // light
            val lightLineInd = 3 + n + 2 * m + k
            val lightPara = lines[lightLineInd].split(' ')
            val light = Light(
                    lightPara[0].toDouble(),
                    lightPara[1].toDouble(),
                    lightPara[2].toDouble(),
                    lightPara[3].toInt(),
                    lightPara[4].toInt(),
                    lightPara[5].toInt(),
                    Vertex(lightPara[0].toDouble(),
                            lightPara[1].toDouble(),
                            lightPara[2].toDouble()))


            // camera
            val camLineInd = 4 + n + 2 * m + k
            val camPara = lines[camLineInd].split(' ')
            val camera = Camera(
                    Vertex(
                            camPara[0].toDouble(),
                            camPara[1].toDouble(),
                            camPara[2].toDouble()),
                    Vertex(
                            camPara[3].toDouble(),
                            camPara[4].toDouble(),
                            camPara[5].toDouble()),
                    camPara[6].toInt())


            val normals = Array(n, { _ -> DoubleArray(3) })

            for (vertexId: Int in 0 until n) {
                var zbiorTrojkatowZWierzcholkiem = HashSet<Int>()
                for (trojkatId in 0 until traingles.size) {
                    var hasSameVtx = false;
                    for (ind in traingles[trojkatId]) {
                        if (vertexId == ind) hasSameVtx = true
                    }
                    if (hasSameVtx)
                        zbiorTrojkatowZWierzcholkiem.add(trojkatId)
                }

                val normal = Vec3d()
                for (tId in zbiorTrojkatowZWierzcholkiem) {
                    val v0 = vertices[traingles[tId][0]]
                    val v1 = vertices[traingles[tId][1]]
                    val v2 = vertices[traingles[tId][2]]

                    val norma = Vec3d()
                    norma.cross(
                            Vec3d(
                                    v2[0] - v0[0],
                                    v2[1] - v0[1],
                                    v2[2] - v0[2]),
                            Vec3d(
                                    v1[0] - v0[0],
                                    v1[1] - v0[1],
                                    v1[2] - v0[2])
                    )
                    normal.add(norma)
                }
                normal.normalize()

                normals[vertexId][0] = normal.x
                normals[vertexId][1] = normal.y
                normals[vertexId][2] = normal.z
            }

            return Scene(vertices, normals, traingles, parts, light, camera)
        }
    }
}