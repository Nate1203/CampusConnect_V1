package za.co.varstycollege.st1009749.campusconnect__v1

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class ChatListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ChatListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        recyclerView = view.findViewById(R.id.chatListRecyclerView)
        emptyStateText = view.findViewById(R.id.emptyStateText)

        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        loadChats()
    }

    private fun setupRecyclerView() {
        adapter = ChatListAdapter { thread ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ChatFragment.newInstance(
                    queryId = thread.queryId,
                    adminId = thread.adminId,
                    studentId = thread.studentId
                ))
                .addToBackStack(null)
                .commit()
        }
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }


    private fun loadChats() {
        val sharedPref = activity?.getSharedPreferences("CampusConnectPrefs", AppCompatActivity.MODE_PRIVATE)
        val isAdmin = sharedPref?.getBoolean("isAdmin", false) ?: false
        val userId = if (isAdmin) {
            sharedPref?.getString("adminEmail", "")
        } else {
            sharedPref?.getString("studentId", "")
        }

        Log.d("ChatList", "Loading chats for user: $userId, isAdmin: $isAdmin")

        if (userId != null) {
            db.collection("Chats")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("ChatList", "Error loading chats: ${e.message}")
                        return@addSnapshotListener
                    }

                    snapshot?.documentChanges?.forEach { change ->
                        val message = change.document.toObject(ChatMessage::class.java)
                        // Show notification for new messages meant for current user
                        if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED &&
                            message.receiverId == userId &&
                            message.timestamp > System.currentTimeMillis() - 1000) {

                        }
                    }

                    val messages = snapshot?.documents ?: listOf()
                    Log.d("ChatList", "Retrieved ${messages.size} messages")

                    val threadMap = mutableMapOf<String, ThreadInfo>()

                    messages.forEach { doc ->
                        val message = doc.toObject(ChatMessage::class.java) ?: return@forEach

                        if (message.senderId == userId || message.receiverId == userId) {
                            val threadKey = message.queryId

                            if (!threadMap.containsKey(threadKey) ||
                                (threadMap[threadKey]?.lastMessageTimestamp ?: 0) < message.timestamp) {

                                threadMap[threadKey] = ThreadInfo(
                                    queryId = message.queryId,
                                    lastMessage = message.message,
                                    lastMessageTimestamp = message.timestamp,
                                    otherParticipantName = if (message.senderId == userId)
                                        message.receiverName
                                    else
                                        message.senderName,
                                    adminId = if (isAdmin) userId else message.receiverId,
                                    studentId = if (isAdmin) message.senderId else userId
                                )
                            }
                        }
                    }

                    val threads = threadMap.values.toList()
                        .sortedByDescending { it.lastMessageTimestamp }

                    Log.d("ChatList", "Processed ${threads.size} threads")

                    if (threads.isEmpty()) {
                        emptyStateText.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyStateText.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter.submitList(threads)
                    }
                }
        } else {
            Log.e("ChatList", "User ID is null")
            Toast.makeText(context, "Error: User ID not found", Toast.LENGTH_SHORT).show()
        }
    }


    private data class ThreadInfo(
        val queryId: String,
        val lastMessage: String,
        val lastMessageTimestamp: Long,
        val otherParticipantName: String,
        val adminId: String = "",
        val studentId: String = ""
    )

    private inner class ChatListAdapter(
        private val onThreadClick: (ThreadInfo) -> Unit
    ) : RecyclerView.Adapter<ChatListAdapter.ThreadViewHolder>() {

        private var threads: List<ThreadInfo> = listOf()

        inner class ThreadViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val participantName: TextView = view.findViewById(R.id.participantName)
            val lastMessage: TextView = view.findViewById(R.id.lastMessage)
            val timestamp: TextView = view.findViewById(R.id.timestampText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_thread, parent, false)
            return ThreadViewHolder(view)
        }

        override fun onBindViewHolder(holder: ThreadViewHolder, position: Int) {
            val thread = threads[position]
            holder.participantName.text = thread.otherParticipantName
            holder.lastMessage.text = thread.lastMessage

            val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            holder.timestamp.text = dateFormat.format(Date(thread.lastMessageTimestamp))

            holder.itemView.setOnClickListener {
                onThreadClick(thread)
            }
        }

        override fun getItemCount() = threads.size

        fun submitList(newThreads: List<ThreadInfo>) {
            threads = newThreads
            notifyDataSetChanged()
        }
    }


}