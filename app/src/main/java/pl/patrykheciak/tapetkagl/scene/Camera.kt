package model.scene

data class Camera(var v0: Vertex,
                  var vc: Vertex,
                  var a: Int) {
    fun deepCopy():  Camera {
        return Camera(Vertex(v0.x, v0.y, v0.z), vc.copy(), a)
    }
}