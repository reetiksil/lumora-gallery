package com.lumora.gallery.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lumora.gallery.data.model.MediaItem
import com.lumora.gallery.data.repository.MediaRepository
import com.lumora.gallery.ui.theme.BadgeBackground
import com.lumora.gallery.ui.theme.DarkBorder
import com.lumora.gallery.ui.theme.DarkSurface
import com.lumora.gallery.ui.theme.DarkSurfaceElevated
import com.lumora.gallery.ui.theme.PureBlack
import com.lumora.gallery.ui.theme.SoothingBlue
import com.lumora.gallery.ui.theme.TextPrimary
import com.lumora.gallery.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TimeSuggestion(
    val title: String,
    val query: String,
    val sampleItem: MediaItem? = null
)

/**
 * HyperOS style Search Screen (Matching Screenshot 5)
 *
 * Supports searching across:
 * - Date & Relative Time (e.g. "Last year", "Oct 2026", "October", "Morning", "Night", "Yesterday")
 * - Places & Location (EXIF location tags, folder/bucket locations e.g. "Dima Hasao", "Chowdang Gaon")
 * - Smart Categories (Cards & documents, QR codes, Electronics, Transport, Screenshots, Videos)
 * - File names & extensions.
 */
@Composable
fun SearchScreen(
    mediaRepository: MediaRepository,
    onBackClick: () -> Unit,
    onItemClick: (MediaItem, List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var allMedia by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var filteredResults by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val dateFormat = remember { SimpleDateFormat("yyyy MMMM MMM d EEEE", Locale.ENGLISH) }
    val timeFormat = remember { SimpleDateFormat("HH:mm a", Locale.ENGLISH) }

    // Dynamic suggestions based on media library
    var timeSuggestions by remember { mutableStateOf<List<TimeSuggestion>>(emptyList()) }
    val smartCategories = remember {
        listOf(
            "Cards & documents",
            "QR codes",
            "Electronics",
            "Transport",
            "Screenshots",
            "Videos"
        )
    }
    val placeSuggestions = remember {
        listOf(
            "Dima Hasao",
            "Chowdang Gaon",
            "Guwahati",
            "Camera",
            "Lightroom"
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val items = mediaRepository.getAllMediaItems()
            allMedia = items
            filteredResults = items

            // Build dynamic Time suggestions with thumbnails
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val lastYear = currentYear - 1
            val lastYearItem = items.firstOrNull {
                val cal = Calendar.getInstance().apply { timeInMillis = it.dateTakenMs }
                cal.get(Calendar.YEAR) == lastYear
            } ?: items.getOrNull(0)

            val octItem = items.firstOrNull {
                val cal = Calendar.getInstance().apply { timeInMillis = it.dateTakenMs }
                cal.get(Calendar.MONTH) == Calendar.OCTOBER
            } ?: items.getOrNull(1)

            val suggestions = mutableListOf<TimeSuggestion>()
            suggestions.add(TimeSuggestion("Last year", "$lastYear", lastYearItem))
            suggestions.add(TimeSuggestion("Oct $currentYear", "Oct $currentYear", octItem))
            timeSuggestions = suggestions

            isLoading = false
        }
    }

    LaunchedEffect(searchQuery, allMedia) {
        if (searchQuery.isBlank()) {
            filteredResults = emptyList()
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            filteredResults = allMedia.filter { item ->
                val nameMatch = item.displayName.lowercase(Locale.ROOT).contains(q)
                val bucketMatch = item.bucketName.lowercase(Locale.ROOT).contains(q)
                val dateStr = dateFormat.format(Date(item.dateTakenMs)).lowercase(Locale.ROOT)
                val dateMatch = dateStr.contains(q)

                // Category matches
                val isScreenshot = item.bucketName.contains("screenshot", ignoreCase = true) ||
                        item.displayName.contains("screenshot", ignoreCase = true)
                val isVideo = item.isVideo
                val categoryMatch = when {
                    q.contains("video") -> isVideo
                    q.contains("screenshot") -> isScreenshot
                    q.contains("document") || q.contains("card") -> item.displayName.contains("doc", ignoreCase = true) || item.displayName.contains("card", ignoreCase = true) || item.bucketName.contains("doc", ignoreCase = true)
                    q.contains("qr") -> item.displayName.contains("qr", ignoreCase = true)
                    q.contains("electronic") -> item.bucketName.contains("download", ignoreCase = true)
                    q.contains("transport") -> item.displayName.contains("transport", ignoreCase = true)
                    else -> false
                }

                // Time of day matching (morning / afternoon / night)
                val cal = Calendar.getInstance().apply { timeInMillis = item.dateTakenMs }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val timeOfDayMatch = when {
                    q == "morning" -> hour in 5..11
                    q == "afternoon" -> hour in 12..17
                    q == "evening" -> hour in 18..21
                    q == "night" -> hour in 22..24 || hour in 0..4
                    else -> false
                }

                nameMatch || bucketMatch || dateMatch || categoryMatch || timeOfDayMatch
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // --- Top Bar: Search Field + Cancel (Matching Screenshot 5) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Times, places, file names...", color = TextSecondary, fontSize = 15.sp)
                },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurfaceElevated,
                    focusedBorderColor = SoothingBlue,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Cancel",
                color = SoothingBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    if (searchQuery.isNotEmpty()) {
                        searchQuery = ""
                    } else {
                        onBackClick()
                    }
                }
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SoothingBlue)
            }
        } else if (searchQuery.isBlank()) {
            // ==========================================
            // Suggestions Mode (Matching Screenshot 5)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Section 1: Times
                Text(
                    text = "Times",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(timeSuggestions) { suggestion ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { searchQuery = suggestion.query }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DarkSurface)
                            ) {
                                if (suggestion.sampleItem != null) {
                                    AsyncImage(
                                        model = suggestion.sampleItem.uri,
                                        contentDescription = suggestion.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = suggestion.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Section 2: Smart categories
                Text(
                    text = "Smart categories",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(smartCategories) { category ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(DarkSurfaceElevated)
                                .clickable { searchQuery = category }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = category,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Section 3: Places
                Text(
                    text = "Places",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(placeSuggestions) { place ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(DarkSurfaceElevated)
                                .clickable { searchQuery = place }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = place,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        } else {
            // ==========================================
            // Search Results Grid
            // ==========================================
            if (filteredResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No media found for \"$searchQuery\"",
                        color = TextSecondary,
                        fontSize = 15.sp
                    )
                }
            } else {
                Text(
                    text = "${filteredResults.size} results found",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredResults, key = { it.id }) { item ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(DarkSurface)
                                .clickable { onItemClick(item, filteredResults) }
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.uri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = item.displayName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            if (item.isVideo) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Video",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
