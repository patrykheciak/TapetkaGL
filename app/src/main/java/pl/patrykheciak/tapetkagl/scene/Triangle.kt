package model.scene

data class Triangle(val vertices : MutableList<Vertex>){

    fun deepCopy() : Triangle {
        var list = ArrayList<Vertex>()
        for (v in vertices)
            list.add(v.copy())
        return Triangle(list.toMutableList())
    }
}