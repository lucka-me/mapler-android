package labs.lucka.wallmapper

data class MapStyleIndex(
    var name: String,
    var author: String,
    var path: String,
    var type: StyleType = StyleType.ONLINE
) {

    enum class StyleType {
        ONLINE, LOCAL, CUSTOMIZED, MAPBOX, LUCKA
    }

}