package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class ChatFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ChatAdapter
    private var queryId: String = ""
    private var adminId: String = ""
    private var studentId: String = ""

    companion object {
        fun newInstance(queryId: String, adminId: String, studentId: String) =
            ChatFragment().apply {
                arguments = Bundle().apply {
                    putString("queryId", queryId)
                    putString("adminId", adminId)
                    putString("studentId", studentId)
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        // Get arguments
        queryId = arguments?.getString("queryId") ?: ""
        adminId = arguments?.getString("adminId") ?: ""
        studentId = arguments?.getString("studentId") ?: ""

        // Initialize views
        recyclerView = view.findViewById(R.id.messagesRecyclerView)
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)

        setupRecyclerView()
        setupSendButton()
        loadMessages()
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = this@ChatFragment.adapter
        }
    }

    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }
    }

    private fun sendMessage(messageText: String) {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        val isAdmin = sharedPref?.getBoolean("isAdmin", false) ?: false

        val senderId = if (isAdmin) adminId else studentId
        val receiverId = if (isAdmin) studentId else adminId
        val senderName = if (isAdmin) {
            "${sharedPref?.getString("adminFirstName", "")} ${sharedPref?.getString("adminLastName", "")}"
        } else {
            sharedPref?.getString("studentName", "")
        }

        val message = ChatMessage(
            messageId = UUID.randomUUID().toString(),
            queryId = queryId,
            senderId = senderId,
            senderType = if (isAdmin) "admin" else "student",
            senderName = senderName ?: "",
            receiverId = receiverId,
            receiverName = "", // This will be set by the receiver when they read it
            message = messageText
        )

        db.collection("Chats")
            .document(message.messageId)
            .set(message)
            .addOnSuccessListener {
                messageInput.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMessages() {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        val isAdmin = sharedPref?.getBoolean("isAdmin", false) ?: false
        val currentUserId = if (isAdmin) adminId else studentId

        db.collection("Chats")
            .whereEqualTo("queryId", queryId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    val message = change.document.toObject(ChatMessage::class.java)
                    if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED &&
                        message.senderId != currentUserId &&
                        message.timestamp > System.currentTimeMillis() - 1000) {

                    }
                }

                val messages = snapshot?.documents?.mapNotNull {
                    it.toObject(ChatMessage::class.java)
                } ?: listOf()

                adapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    recyclerView.scrollToPosition(messages.size - 1)
                }
            }
    }

    inner class ChatAdapter : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {
        private var messages: List<ChatMessage> = listOf()

        fun submitList(newMessages: List<ChatMessage>) {
            messages = newMessages
            notifyDataSetChanged()
        }

        inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val messageText: TextView = view.findViewById(R.id.messageText)
            val messageContainer: LinearLayout = view.findViewById(R.id.messageContainer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message, parent, false)
            return MessageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
            val message = messages[position]
            val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
            val isAdmin = sharedPref?.getBoolean("isAdmin", false) ?: false
            val currentUserId = if (isAdmin) adminId else studentId

            holder.messageText.text = message.message

            // Align messages based on sender
            val params = holder.messageContainer.layoutParams as LinearLayout.LayoutParams
            if (message.senderId == currentUserId) {
                params.gravity = android.view.Gravity.END
                holder.messageContainer.setBackgroundResource(R.drawable.message_bubble_sent)
            } else {
                params.gravity = android.view.Gravity.START
                holder.messageContainer.setBackgroundResource(R.drawable.message_bubble_received)
            }
            holder.messageContainer.layoutParams = params
        }

        override fun getItemCount() = messages.size
    }


}