package com.example.apptrack.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactInfo(
    val id: Long,
    val name: String,
    val phoneNumber: String,
    val photo: Bitmap? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsSearchScreen(
    onBack: () -> Unit,
    onContactSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Load contacts when screen opens
    LaunchedEffect(Unit) {
        isLoading = true
        contacts = loadContacts(context, "")
        isLoading = false
    }
    
    // Filter contacts based on search query
    LaunchedEffect(searchQuery) {
        isLoading = true
        contacts = loadContacts(context, searchQuery)
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Contacts List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) "No contacts found" else "No results",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(contacts) { contact ->
                        ContactItem(
                            contact = contact,
                            onClick = { onContactSelected(contact.phoneNumber) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        placeholder = { Text("Search contacts...") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun ContactItem(
    contact: ContactInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact Photo
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (contact.photo != null) {
                    Image(
                        bitmap = contact.photo.asImageBitmap(),
                        contentDescription = contact.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Contact Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                Icons.Default.Phone,
                contentDescription = "Call",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

suspend fun loadContacts(context: android.content.Context, query: String): List<ContactInfo> {
    return withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext emptyList()
        }
        
        val contacts = mutableListOf<ContactInfo>()
        
        try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            
            val selection = if (query.isNotEmpty()) {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            } else {
                null
            }
            
            val selectionArgs = if (query.isNotEmpty()) {
                arrayOf("%$query%", "%$query%")
            } else {
                null
            }
            
            val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                
                val seenContacts = mutableSetOf<String>()
                
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex) ?: "Unknown"
                    val number = cursor.getString(numberIndex) ?: ""
                    
                    // Skip duplicates
                    val key = "$id-$number"
                    if (seenContacts.contains(key)) continue
                    seenContacts.add(key)
                    
                    // Load photo
                    val photo = loadContactPhoto(context, id)
                    
                    contacts.add(
                        ContactInfo(
                            id = id,
                            name = name,
                            phoneNumber = number,
                            photo = photo
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsSearch", "Failed to load contacts: ${e.message}", e)
        }
        
        contacts
    }
}

suspend fun loadContactPhoto(context: android.content.Context, contactId: Long): Bitmap? {
    return withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CONTACTS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }
        
        try {
            // First, get the lookup key for the contact
            var lookupKey: String? = null
            val contactUri = android.net.Uri.withAppendedPath(
                ContactsContract.Contacts.CONTENT_URI,
                contactId.toString()
            )
            
            val cursor = context.contentResolver.query(
                contactUri,
                arrayOf(ContactsContract.Contacts.LOOKUP_KEY),
                null,
                null,
                null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val lookupKeyIndex = it.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
                    if (lookupKeyIndex >= 0) {
                        lookupKey = it.getString(lookupKeyIndex)
                    }
                }
            }
            
            // Build photo URI with lookup key if available
            val photoUri = if (lookupKey != null) {
                ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
            } else {
                contactUri
            }
            
            // Try high-res first, then thumbnail
            val photoStream = ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver,
                photoUri,
                true // prefer high-res
            ) ?: ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver,
                photoUri,
                false // fallback to thumbnail
            )
            
            photoStream?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsSearchScreen", "Failed to load contact photo for ID $contactId: ${e.message}", e)
            null
        }
    }
}
