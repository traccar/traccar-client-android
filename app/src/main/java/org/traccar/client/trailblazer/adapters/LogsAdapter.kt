package org.traccar.client.trailblazer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.traccar.client.R
import org.traccar.client.trailblazer.util.Logger

class LogsAdapter(private val logs: List<Logger.LogEntry>) :
    RecyclerView.Adapter<LogsAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.log_title)
        val description: TextView = view.findViewById(R.id.log_description)
        val timestamp: TextView = view.findViewById(R.id.log_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        holder.title.text = log.title
        holder.description.text = log.description
        holder.timestamp.text = log.timestamp
    }

    override fun getItemCount(): Int = logs.size
}
