package org.traccar.client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class HistoryFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.history_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val adapter = SubmissionAdapter()
        recyclerView.adapter = adapter

        val db = DatabaseHelper(requireContext())
        db.selectAllFormSubmissionsAsync(object : DatabaseHelper.DatabaseHandler<List<FormSubmission>> {
            override fun onComplete(success: Boolean, result: List<FormSubmission>?) {
                if (success && result != null) {
                    activity?.runOnUiThread { adapter.submitList(result) }
                }
            }
        })
    }
}

class SubmissionAdapter : ListAdapter<FormSubmission, SubmissionViewHolder>(SubmissionDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubmissionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_submission, parent, false)
        return SubmissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubmissionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class SubmissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(submission: FormSubmission) {
        itemView.findViewById<TextView>(R.id.container_id_text).text = submission.containerId
        itemView.findViewById<TextView>(R.id.comment_text).text = submission.comment
        itemView.findViewById<TextView>(R.id.device_id_text).text = submission.deviceId
        itemView.findViewById<TextView>(R.id.timestamp_text).text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(submission.timestamp))
    }
}

class SubmissionDiffCallback : DiffUtil.ItemCallback<FormSubmission>() {
    override fun areItemsTheSame(oldItem: FormSubmission, newItem: FormSubmission): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: FormSubmission, newItem: FormSubmission): Boolean = oldItem == newItem
}