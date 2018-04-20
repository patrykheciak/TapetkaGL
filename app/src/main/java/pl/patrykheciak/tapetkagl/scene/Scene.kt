package model.scene

data class Scene(var vtxs: Array<DoubleArray>,
                 var normals: Array<DoubleArray>,
                 var trngs: Array<IntArray>,
                 val parts: List<Part>,
                 var light: Light,
                 var camera: Camera) {
}
