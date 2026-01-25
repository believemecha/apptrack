package com.example.apptrack.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.apptrack.call.CallInfo
import com.example.apptrack.call.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactProfileData(
    val contactId: Long,
    val displayName: String,
    val phoneNumbers: List<String>,
    val emails: List<String>,
    val photo: Bitmap? = null,
    val organization: String? = null,
    val jobTitle: String? = null,
    val address: String? = null,
    val notes: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactProfileScreen(
    phoneNumber: String,
    callHistory: List<CallInfo>,
    onBack: () -> Unit,
    onMakeCall: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var contactProfile by remember { mutableStateOf<ContactProfileData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val filteredCalls = remember(phoneNumber, callHistory) {
        callHistory.filter { it.phoneNumber == phoneNumber }
            .sortedByDescending { it.timestamp }
    }
    
    LaunchedEffect(phoneNumber) {
        isLoading = true
        contactProfile = loadContactProfile(context, phoneNumber)
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (contactProfile != null && contactProfile!!.phoneNumbers.isNotEmpty()) {
                        IconButton(onClick = { onMakeCall(contactProfile!!.phoneNumbers.first()) }) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Call",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (contactProfile == null) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
                        text = "Contact not found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Header with photo and name
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Contact Photo
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (contactProfile!!.photo != null) {
                                Image(
                                    bitmap = contactProfile!!.photo!!.asImageBitmap(),
                                    contentDescription = contactProfile!!.displayName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = contactProfile!!.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.displayMedium,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        
                        Text(
                            text = contactProfile!!.displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (contactProfile!!.organization != null || contactProfile!!.jobTitle != null) {
                            Text(
                                text = buildString {
                                    if (contactProfile!!.jobTitle != null) {
                                        append(contactProfile!!.jobTitle)
                                    }
                                    if (contactProfile!!.jobTitle != null && contactProfile!!.organization != null) {
                                        append(" at ")
                                    }
                                    if (contactProfile!!.organization != null) {
                                        append(contactProfile!!.organization)
                                    }
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                
                // Phone Numbers
                if (contactProfile!!.phoneNumbers.isNotEmpty()) {
                    item {
                        SectionHeader("Phone Numbers")
                    }
                    items(contactProfile!!.phoneNumbers) { number ->
                        ContactDetailItem(
                            icon = Icons.Default.Phone,
                            title = number,
                            onClick = { onMakeCall(number) },
                            actionIcon = Icons.Default.Phone
                        )
                    }
                }
                
                // Email Addresses
                if (contactProfile!!.emails.isNotEmpty()) {
                    item {
                        SectionHeader("Email Addresses")
                    }
                    items(contactProfile!!.emails) { email ->
                        ContactDetailItem(
                            icon = Icons.Default.Email,
                            title = email,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$email")
                                }
                                context.startActivity(intent)
                            },
                            actionIcon = Icons.Default.Send
                        )
                    }
                }
                
                // Address
                if (contactProfile!!.address != null) {
                    item {
                        SectionHeader("Address")
                    }
                    item {
                        ContactDetailItem(
                            icon = Icons.Default.LocationOn,
                            title = contactProfile!!.address!!,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("geo:0,0?q=${Uri.encode(contactProfile!!.address)}")
                                }
                                context.startActivity(intent)
                            },
                            actionIcon = Icons.Default.LocationOn
                        )
                    }
                }
                
                // Notes
                if (contactProfile!!.notes != null && contactProfile!!.notes!!.isNotEmpty()) {
                    item {
                        SectionHeader("Notes")
                    }
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = contactProfile!!.notes!!,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Call History
                if (filteredCalls.isNotEmpty()) {
                    item {
                        SectionHeader("Call History (${filteredCalls.size})")
                    }
                    items(filteredCalls.take(10)) { call ->
                        CallHistoryItem(
                            call = call,
                            onCall = { onMakeCall(call.phoneNumber) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun ContactDetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (actionIcon != null) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = "Action",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun CallHistoryItem(
    call: CallInfo,
    onCall: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onCall),
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
            Icon(
                imageVector = when (call.callType) {
                    CallType.INCOMING -> Icons.Default.Phone
                    CallType.OUTGOING -> Icons.Default.Phone
                    CallType.MISSED -> Icons.Default.Close
                    CallType.REJECTED -> Icons.Default.Close
                    CallType.BLOCKED -> Icons.Default.Close
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when (call.callType) {
                    CallType.INCOMING -> MaterialTheme.colorScheme.primary
                    CallType.OUTGOING -> MaterialTheme.colorScheme.tertiary
                    CallType.MISSED -> MaterialTheme.colorScheme.error
                    CallType.REJECTED -> MaterialTheme.colorScheme.error
                    CallType.BLOCKED -> MaterialTheme.colorScheme.error
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = call.callType.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatCallDate(call.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            if (call.duration > 0) {
                Text(
                    text = formatCallDuration(call.duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            IconButton(onClick = onCall) {
                Icon(
                    Icons.Default.Phone,
                    contentDescription = "Call",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

suspend fun loadContactProfile(
    context: Context,
    phoneNumber: String
): ContactProfileData? = withContext(Dispatchers.IO) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return@withContext null
    }
    
    try {
        // Normalize phone number
        val normalizedNumber = phoneNumber.replace(Regex("[^+\\d]"), "")
        
        // Find contact by phone number
        val lookupUri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
            .appendPath(normalizedNumber)
            .build()
        
        val lookupCursor = context.contentResolver.query(
            lookupUri,
            arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY),
            null,
            null,
            null
        )
        
        var contactId: Long? = null
        var lookupKey: String? = null
        
        lookupCursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(ContactsContract.PhoneLookup._ID)
                val lookupKeyIndex = it.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)
                if (idIndex >= 0) contactId = it.getLong(idIndex)
                if (lookupKeyIndex >= 0) lookupKey = it.getString(lookupKeyIndex)
            }
        }
        
        if (contactId == null) {
            // Try with original number
            val originalLookupUri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                .appendPath(phoneNumber)
                .build()
            
            val originalCursor = context.contentResolver.query(
                originalLookupUri,
                arrayOf(ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.LOOKUP_KEY),
                null,
                null,
                null
            )
            
            originalCursor?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    val lookupKeyIndex = it.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)
                    if (idIndex >= 0) contactId = it.getLong(idIndex)
                    if (lookupKeyIndex >= 0) lookupKey = it.getString(lookupKeyIndex)
                }
            }
        }
        
        if (contactId == null) {
            return@withContext null
        }
        
        // Load full contact details
        val contactUri = if (lookupKey != null) {
            ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
        } else {
            Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId.toString())
        }
        
        val contactCursor = context.contentResolver.query(
            contactUri,
            arrayOf(
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_URI
            ),
            null,
            null,
            null
        )
        
        var displayName = phoneNumber
        var photoUri: Uri? = null
        
        contactCursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val photoIndex = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                if (nameIndex >= 0) {
                    val name = it.getString(nameIndex)
                    if (name != null) displayName = name
                }
                if (photoIndex >= 0) {
                    val photo = it.getString(photoIndex)
                    if (photo != null) photoUri = Uri.parse(photo)
                }
            }
        }
        
        // Load photo
        val photo = if (photoUri != null) {
            loadContactPhotoFromUri(context, photoUri!!)
        } else {
            val photoStream = ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver,
                contactUri,
                true
            ) ?: ContactsContract.Contacts.openContactPhotoInputStream(
                context.contentResolver,
                contactUri,
                false
            )
            photoStream?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
        
        // Load phone numbers
        val phoneNumbers = mutableListOf<String>()
        val phoneCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )
        
        phoneCursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                if (numberIndex >= 0) {
                    val number = it.getString(numberIndex)
                    if (number != null && number !in phoneNumbers) {
                        phoneNumbers.add(number)
                    }
                }
            }
        }
        
        // Load emails
        val emails = mutableListOf<String>()
        val emailCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.DATA),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )
        
        emailCursor?.use {
            val emailIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA)
            while (it.moveToNext()) {
                if (emailIndex >= 0) {
                    val email = it.getString(emailIndex)
                    if (email != null && email !in emails) {
                        emails.add(email)
                    }
                }
            }
        }
        
        // Load organization and job title
        var organization: String? = null
        var jobTitle: String? = null
        val orgCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.CommonDataKinds.Organization.TITLE
            ),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(
                contactId.toString(),
                ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
            ),
            null
        )
        
        orgCursor?.use {
            if (it.moveToFirst()) {
                val companyIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY)
                val titleIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE)
                if (companyIndex >= 0) {
                    organization = it.getString(companyIndex)
                }
                if (titleIndex >= 0) {
                    jobTitle = it.getString(titleIndex)
                }
            }
        }
        
        // Load address
        var address: String? = null
        val addressCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.StructuredPostal.STREET,
                ContactsContract.CommonDataKinds.StructuredPostal.CITY,
                ContactsContract.CommonDataKinds.StructuredPostal.REGION,
                ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE,
                ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY
            ),
            "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )
        
        addressCursor?.use {
            if (it.moveToFirst()) {
                val parts = mutableListOf<String>()
                val streetIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET)
                val cityIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY)
                val regionIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION)
                val postcodeIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE)
                val countryIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY)
                
                if (streetIndex >= 0) it.getString(streetIndex)?.let { parts.add(it) }
                if (cityIndex >= 0) it.getString(cityIndex)?.let { parts.add(it) }
                if (regionIndex >= 0) it.getString(regionIndex)?.let { parts.add(it) }
                if (postcodeIndex >= 0) it.getString(postcodeIndex)?.let { parts.add(it) }
                if (countryIndex >= 0) it.getString(countryIndex)?.let { parts.add(it) }
                
                if (parts.isNotEmpty()) {
                    address = parts.joinToString(", ")
                }
            }
        }
        
        // Load notes
        var notes: String? = null
        val notesCursor = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Note.NOTE),
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(
                contactId.toString(),
                ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE
            ),
            null
        )
        
        notesCursor?.use {
            if (it.moveToFirst()) {
                val noteIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)
                if (noteIndex >= 0) {
                    notes = it.getString(noteIndex)
                }
            }
        }
        
        ContactProfileData(
            contactId = contactId,
            displayName = displayName,
            phoneNumbers = phoneNumbers,
            emails = emails,
            photo = photo,
            organization = organization,
            jobTitle = jobTitle,
            address = address,
            notes = notes
        )
    } catch (e: Exception) {
        android.util.Log.e("ContactProfileScreen", "Failed to load contact profile: ${e.message}", e)
        null
    }
}

suspend fun loadContactPhotoFromUri(
    context: Context,
    photoUri: Uri
): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.contentResolver.openInputStream(photoUri)
        inputStream?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (e: Exception) {
        null
    }
}
