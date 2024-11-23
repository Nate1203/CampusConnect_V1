package za.co.varstycollege.st1009749.campusconnect__v1

data class ChatMessage(
    val messageId: String = "",
    val queryId: String = "",
    val senderId: String = "",  // studentId for students, adminEmail for admins
    val senderType: String = "", // "student" or "admin"
    val senderName: String = "",
    val receiverId: String = "", // studentId for students, adminEmail for admins
    val receiverName: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)