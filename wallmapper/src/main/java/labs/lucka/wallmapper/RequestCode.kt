package labs.lucka.wallmapper

enum class RequestCode(val code: Int) {
    RequestPermissionWriteExternalStorage(1001),
    RequestPermissionFineLocation(1002),
    ManageMapStyle(2001),
    SetPreference(2002),
    OpenJsonFile(3001)
}