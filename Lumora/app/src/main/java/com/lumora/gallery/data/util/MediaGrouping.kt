package com.lumora.gallery.data.util

import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.data.model.SortOrder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pure functions for media filtering, sorting, date grouping, and formatting.
 * Decoupled from Android Context for fast, reliable unit testing.
 */
object MediaGrouping {

    /**
     * Filters a list of media items by allowed bucket IDs.
     * If allowedBucketIds is empty, returns an empty list (strict filtering).
     */
    fun filterByBuckets(items: List<MediaItem>, allowedBucketIds: Set<String>): List<MediaItem> {
        if (allowedBucketIds.isEmpty()) return emptyList()
        return items.filter { allowedBucketIds.contains(it.bucketId) }
    }

    /**
     * Sorts media items according to the requested SortOrder.
     */
    fun sortMedia(items: List<MediaItem>, sortOrder: SortOrder): List<MediaItem> {
        return when (sortOrder) {
            SortOrder.DATE_TAKEN_DESC -> items.sortedByDescending { it.dateTakenMs }
            SortOrder.DATE_TAKEN_ASC -> items.sortedBy { it.dateTakenMs }
            SortOrder.DATE_MODIFIED_DESC -> items.sortedByDescending { it.dateModifiedSec }
            SortOrder.DISPLAY_NAME_ASC -> items.sortedBy { it.displayName.lowercase(Locale.ROOT) }
        }
    }

    /**
     * Groups media items by date headers:
     * - "Today" for current day
     * - "Yesterday" for previous day
     * - "d MMMM" (e.g. "1 Nov") for items in the current year
     * - "d MMMM yyyy" (e.g. "1 Nov 2024") for older years
     */
    fun groupMediaByDate(
        items: List<MediaItem>,
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): Map<String, List<MediaItem>> {
        val nowCal = Calendar.getInstance().apply { timeInMillis = referenceTimeMillis }
        val currentYear = nowCal.get(Calendar.YEAR)
        val todayDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        val itemCal = Calendar.getInstance()
        val dayMonthFormat = SimpleDateFormat("d MMMM", Locale.ENGLISH)
        val fullDateFormat = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)

        val grouped = linkedMapOf<String, MutableList<MediaItem>>()

        for (item in items) {
            itemCal.timeInMillis = item.dateTakenMs
            val itemYear = itemCal.get(Calendar.YEAR)
            val itemDayOfYear = itemCal.get(Calendar.DAY_OF_YEAR)

            val header = when {
                itemYear == currentYear && itemDayOfYear == todayDayOfYear -> "Today"
                itemYear == currentYear && itemDayOfYear == todayDayOfYear - 1 -> "Yesterday"
                itemYear == currentYear -> dayMonthFormat.format(Date(item.dateTakenMs))
                else -> fullDateFormat.format(Date(item.dateTakenMs))
            }

            grouped.getOrPut(header) { mutableListOf() }.add(item)
        }
        return grouped
    }

    /**
     * Formats raw bytes into a human-readable storage string (e.g., "14.2 MB").
     */
    fun formatStorageSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0

        return when {
            bytes >= gb -> String.format(Locale.US, "%.1f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
            else -> "$bytes B"
        }
    }
}
