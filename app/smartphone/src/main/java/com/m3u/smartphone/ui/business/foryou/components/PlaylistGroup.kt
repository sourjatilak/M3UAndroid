package com.m3u.smartphone.ui.business.foryou.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m3u.core.foundation.components.AbsoluteSmoothCornerShape
import com.m3u.core.foundation.components.CircularProgressIndicator
import com.m3u.data.database.model.DataSource
import com.m3u.data.database.model.Playlist
import com.m3u.data.database.model.epgUrlsOrXtreamXmlUrl
import com.m3u.data.database.model.refreshable
import com.m3u.data.database.model.type
import com.m3u.data.parser.xtream.XtreamInput
import com.m3u.i18n.R.string
import com.m3u.smartphone.ui.material.components.Badge
import com.m3u.smartphone.ui.material.components.FontFamilies
import com.m3u.smartphone.ui.material.model.LocalSpacing

/**
 * A home-screen subscription wrapper. Xtream subscriptions are represented by up
 * to three DB rows (Live / VOD / Series) sharing the same server credentials;
 * they collapse into a single [PlaylistGroup] so the user sees one card per
 * server with sub-chips for each sub-playlist. Non-Xtream subscriptions always
 * produce a single-entry group.
 */
internal data class PlaylistGroup(
    /** Stable identity used as the lazy-grid item key. */
    val key: String,
    /** Title shown as the card header — taken from the first entry, stripped of
     *  the Xtream type suffix if present so "Big Buck Bunny live" becomes
     *  "Big Buck Bunny". May be empty for non-refreshable imported playlists;
     *  [PlaylistGroupCard] substitutes a localized fallback in that case. */
    val title: String,
    /** True if any entry in the group is refreshable (source M3U, real URL).
     *  Non-refreshable groups are locally-imported playlists and get an
     *  "imported" title fallback when [title] is empty. */
    val refreshable: Boolean,
    val entries: List<Entry>,
) {
    /** The [Playlist] to target when the user taps the card-level gear icon.
     *  For Xtream we prefer Live (richest per-playlist settings — EPG,
     *  user-agent); otherwise we fall through to the first available entry. */
    val configurationTarget: Playlist
        get() = entries.firstOrNull {
            it.playlist.type == DataSource.Xtream.TYPE_LIVE
        }?.playlist
            ?: entries.firstOrNull {
                it.playlist.type == DataSource.Xtream.TYPE_VOD
            }?.playlist
            ?: entries.first().playlist

    internal data class Entry(
        val playlist: Playlist,
        val count: Int,
        val subscribingOrRefreshing: Boolean,
        /** Short label for the sub-chip. "LIVE" / "VOD" / "SERIES" for Xtream,
         *  "M3U" for M3U etc. Kept as literal protocol tokens (not translated). */
        val label: String,
    )
}

/**
 * Groups DB playlist rows into home-screen cards. Xtream rows sharing the same
 * `(basicUrl, username, password)` collapse into one group; other rows each
 * become a single-entry group. Input iteration order is preserved for each
 * group's first-seen position in the output list.
 */
internal fun groupPlaylists(
    playlists: Map<Playlist, Int>,
    subscribingPlaylistUrls: Set<String>,
    refreshingEpgUrls: Set<String>,
    refreshingEpgUrlsOfPlaylist: (Playlist) -> Boolean,
): List<PlaylistGroup> {
    if (playlists.isEmpty()) return emptyList()

    // Preserve insertion order (Map<Playlist, Int> is typically a LinkedHashMap
    // produced from Iterable<PlaylistWithCount>.toMap() in the repo). We track
    // groups in a LinkedHashMap keyed by server identity / playlist URL.
    val grouped = LinkedHashMap<String, MutableList<PlaylistGroup.Entry>>()
    val groupTitles = HashMap<String, String>()

    for ((playlist, count) in playlists) {
        // Skip entries hidden by visibility toggles
        if (playlist.source == DataSource.Xtream) {
            val type = playlist.type
            if (type == DataSource.Xtream.TYPE_LIVE && !playlist.showLive) continue
            if (type == DataSource.Xtream.TYPE_VOD && !playlist.showVod) continue
            if (type == DataSource.Xtream.TYPE_SERIES && !playlist.showSeries) continue
        }
        val entry = PlaylistGroup.Entry(
            playlist = playlist,
            count = count,
            subscribingOrRefreshing = playlist.url in subscribingPlaylistUrls ||
                    refreshingEpgUrlsOfPlaylist(playlist),
            label = entryLabelOf(playlist),
        )
        val key = groupKeyOf(playlist)
        val title = groupTitleOf(playlist)
        grouped.getOrPut(key) { mutableListOf() }.add(entry)
        // First writer wins — all sub-playlists in an Xtream group share the
        // same server title anyway.
        groupTitles.putIfAbsent(key, title)
    }

    return grouped.map { (key, entries) ->
        PlaylistGroup(
            key = key,
            title = groupTitles.getValue(key),
            refreshable = entries.any { it.playlist.refreshable },
            entries = sortXtreamEntries(entries),
        )
    }
}

/**
 * Overload that derives the refreshing-set check from a plain refreshingEpgUrls
 * collection — matches how PlaylistGallery already tests per-playlist refresh
 * state (`playlist.epgUrlsOrXtreamXmlUrl().any { it in refreshingEpgUrls }`).
 */
internal fun groupPlaylists(
    playlists: Map<Playlist, Int>,
    subscribingPlaylistUrls: List<String>,
    refreshingEpgUrls: List<String>,
): List<PlaylistGroup> {
    val subscribingSet = subscribingPlaylistUrls.toHashSet()
    val refreshingSet = refreshingEpgUrls.toHashSet()
    return groupPlaylists(
        playlists = playlists,
        subscribingPlaylistUrls = subscribingSet,
        refreshingEpgUrls = refreshingSet,
        refreshingEpgUrlsOfPlaylist = { playlist ->
            val epgCandidates = runCatching { playlist.epgUrlsOrXtreamXmlUrl() }
                .getOrDefault(emptyList())
            epgCandidates.any { it in refreshingSet }
        },
    )
}

private fun groupKeyOf(playlist: Playlist): String = when (playlist.source) {
    DataSource.Xtream -> {
        val input = XtreamInput.decodeFromPlaylistUrlOrNull(playlist.url)
        if (input != null) {
            "xtream:${input.basicUrl}|${input.username}|${input.password}"
        } else {
            // Corrupted Xtream URL — fall back to the URL itself so the row
            // still renders (as its own group) rather than being dropped.
            "xtream-raw:${playlist.url}"
        }
    }
    else -> "url:${playlist.url}"
}

private fun groupTitleOf(playlist: Playlist): String {
    // Prefer user-set display title over the internal subscription title.
    playlist.displayTitle?.let { if (it.isNotBlank()) return it }
    val raw = playlist.title.trim()
    if (raw.isEmpty()) return raw
    // Strip a trailing " live" / " vod" / " series" (case-insensitive) that the
    // subscription worker tacks on per Xtream sub-playlist so the wrapper header
    // reads as the server name, not the sub-playlist name.
    val suffixes = arrayOf(" live", " vod", " series")
    val lower = raw.lowercase()
    for (suffix in suffixes) {
        if (lower.endsWith(suffix)) return raw.dropLast(suffix.length).trim()
    }
    return raw
}

private fun entryLabelOf(playlist: Playlist): String = when (playlist.source) {
    DataSource.Xtream -> playlist.type?.uppercase().orEmpty().ifEmpty { "XTREAM" }
    DataSource.M3U -> "M3U"
    DataSource.EPG -> "EPG"
    else -> playlist.source.toString().uppercase()
}

private val xtreamOrder = listOf(
    DataSource.Xtream.TYPE_LIVE,
    DataSource.Xtream.TYPE_VOD,
    DataSource.Xtream.TYPE_SERIES,
)

/** Force Live → VOD → Series display order so the chips line up consistently
 *  regardless of DB insertion order. Non-Xtream groups pass through unchanged. */
private fun sortXtreamEntries(
    entries: List<PlaylistGroup.Entry>,
): List<PlaylistGroup.Entry> {
    if (entries.size <= 1) return entries
    val anyXtream = entries.any { it.playlist.source == DataSource.Xtream }
    if (!anyXtream) return entries
    return entries.sortedBy { entry ->
        val t = entry.playlist.type
        val idx = xtreamOrder.indexOf(t)
        if (idx == -1) Int.MAX_VALUE else idx
    }
}

@Composable
internal fun PlaylistGroupCard(
    group: PlaylistGroup,
    onEntryClick: (Playlist) -> Unit,
    onConfigure: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val displayTitle = group.title.ifEmpty {
        if (!group.refreshable) stringResource(string.feat_foryou_imported_playlist_title)
        else ""
    }
    OutlinedCard(
        shape = AbsoluteSmoothCornerShape(spacing.medium, 65),
        colors = CardDefaults.cardColors(Color.Transparent),
        modifier = modifier.semantics(mergeDescendants = true) { },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = spacing.medium,
                    end = spacing.extraSmall,
                    top = spacing.small,
                    bottom = spacing.medium,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { onConfigure(group.configurationTarget) },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "configure $displayTitle",
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = spacing.small),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.small),
                ) {
                    group.entries.forEach { entry ->
                        SubPlaylistChip(
                            entry = entry,
                            onClick = { onEntryClick(entry.playlist) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tap → open the sub-playlist. The card-level gear icon handles configuration
 * and per-server unsubscribe, so chips no longer need a long-press affordance.
 */
@Composable
private fun SubPlaylistChip(
    entry: PlaylistGroup.Entry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Surface(
        shape = AbsoluteSmoothCornerShape(spacing.small, 65),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.semantics(mergeDescendants = true) { },
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(
                    horizontal = spacing.small,
                    vertical = spacing.extraSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontFamily = FontFamilies.LexendExa,
                fontSize = 11.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Badge {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall),
                ) {
                    if (entry.subscribingOrRefreshing) {
                        CircularProgressIndicator(
                            color = LocalContentColor.current,
                            size = 8.dp,
                        )
                    }
                    Text(
                        text = entry.count.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        fontFamily = FontFamilies.LexendExa,
                    )
                }
            }
        }
    }
}
