package com.example.apptrack.call

data class GroupedCallInfo(
    val phoneNumber: String,
    val contactName: String?,
    val calls: List<CallInfo>,
    val lastCallType: CallType,
    val lastCallTimestamp: Long,
    val totalCallCount: Int,
    val missedCallCount: Int
) {
    val displayName: String
        get() = contactName ?: phoneNumber
    
    val hasMissedCalls: Boolean
        get() = missedCallCount > 0
}

fun groupCallsByContact(calls: List<CallInfo>): List<GroupedCallInfo> {
    val grouped = calls.groupBy { it.phoneNumber }
    
    return grouped.map { (phoneNumber, callList) ->
        val sortedCalls = callList.sortedByDescending { it.timestamp }
        val lastCall = sortedCalls.first()
        val contactName = lastCall.contactName
        
        GroupedCallInfo(
            phoneNumber = phoneNumber,
            contactName = contactName,
            calls = sortedCalls,
            lastCallType = lastCall.callType,
            lastCallTimestamp = lastCall.timestamp,
            totalCallCount = callList.size,
            missedCallCount = callList.count { it.callType == CallType.MISSED }
        )
    }.sortedByDescending { it.lastCallTimestamp }
}
