package com.example.apptrack.ui.search

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apptrack.call.CallInfo
import com.example.apptrack.call.GroupedCallInfo
import com.example.apptrack.call.groupCallsByContact
import com.example.apptrack.ui.screens.ContactInfo
import com.example.apptrack.ui.screens.formatCallDate
import com.example.apptrack.ui.screens.loadContactPhotoForNumber
import com.example.apptrack.ui.screens.loadContacts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private val recentSearches = mutableListOf<String>()
private val _searchQueryFlow = MutableStateFlow("")
val searchQueryFlow: StateFlow<String> = _searchQueryFlow.asStateFlow()

fun updateSearchQuery(query: String) {
    _searchQueryFlow.update { query }
}

fun addToRecentSearches(query: String) {
    if (query.isNotBlank()) {
        recentSearches.remove(query)
        recentSearches.add(0, query)
        if (recentSearches.size > 10) recentSearches.removeAt(recentSearches.lastIndex)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    callHistory: List<CallInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBack: () -> Unit = {},
    onContactClick: (String) -> Unit,
    onCallDetailsClick: (String) -> Unit,
    onMakeCall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    val groupedCalls = remember(callHistory) { groupCallsByContact(callHistory) }

    LaunchedEffect(searchQuery) {
        contacts = loadContacts(context, searchQuery)
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredCalls = remember(groupedCalls, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else groupedCalls.filter {
            it.phoneNumber.contains(searchQuery) ||
                (it.contactName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }
    val recentList = remember { recentSearches.distinct().take(5) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Search contacts") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { onSearchQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        addToRecentSearches(searchQuery)
                        onSearchQueryChange("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            placeholder = { Text("Search contacts") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        when {
            searchQuery.isBlank() && recentList.isNotEmpty() -> {
                Text(
                    "Recent searches",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(recentList) { query ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSearchQueryChange(query) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.List,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    query,
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
            searchQuery.isBlank() -> {
                // Suggested section (like reference)
                Text(
                    "Suggested",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(groupedCalls.take(10), key = { it.phoneNumber }) { grouped ->
                        SearchCallRow(
                            grouped = grouped,
                            context = context,
                            onClick = { onCallDetailsClick(grouped.phoneNumber) },
                            onCall = { onMakeCall(grouped.phoneNumber) }
                        )
                    }
                }
            }
            filteredContacts.isEmpty() && filteredCalls.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No results",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (filteredContacts.isNotEmpty()) {
                        item(key = "header_contacts") {
                            Text(
                                "Contacts",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(filteredContacts.take(20), key = { "${it.id}_${it.phoneNumber}" }) { contact ->
                            SearchContactRow(
                                contact = contact,
                                onClick = { onContactClick(contact.phoneNumber) },
                                onCall = { onMakeCall(contact.phoneNumber) }
                            )
                        }
                    }
                    if (filteredCalls.isNotEmpty()) {
                        item(key = "header_calls") {
                            Text(
                                "Call history",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(filteredCalls.take(20), key = { it.phoneNumber }) { grouped ->
                            SearchCallRow(
                                grouped = grouped,
                                context = context,
                                onClick = { onCallDetailsClick(grouped.phoneNumber) },
                                onCall = { onMakeCall(grouped.phoneNumber) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchContactRow(
    contact: ContactInfo,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (contact.photo != null) {
                    androidx.compose.foundation.Image(
                        bitmap = contact.photo.asImageBitmap(),
                        contentDescription = contact.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        contact.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(contact.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onCall, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SearchCallRow(
    grouped: GroupedCallInfo,
    context: android.content.Context,
    onClick: () -> Unit,
    onCall: () -> Unit
) {
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(grouped.phoneNumber) {
        photo = loadContactPhotoForNumber(context, grouped.phoneNumber)
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (photo != null) {
                    androidx.compose.foundation.Image(
                        bitmap = photo!!.asImageBitmap(),
                        contentDescription = grouped.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        grouped.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(grouped.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Mobile ${grouped.phoneNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onCall, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
