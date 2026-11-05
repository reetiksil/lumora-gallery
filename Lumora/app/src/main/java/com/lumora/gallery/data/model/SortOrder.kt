package com.lumora.gallery.data.model

enum class SortOrder(val displayName: String) {
    DATE_TAKEN_DESC("Date taken (newest first)"),
    DATE_TAKEN_ASC("Date taken (oldest first)"),
    DATE_MODIFIED_DESC("Date modified (newest first)"),
    DISPLAY_NAME_ASC("Name (A to Z)")
}

enum class ThemeMode(val displayName: String) {
    AMOLED_DARK("AMOLED Dark (Pure black)"),
    LIGHT("Light"),
    SYSTEM("Follow system")
}
