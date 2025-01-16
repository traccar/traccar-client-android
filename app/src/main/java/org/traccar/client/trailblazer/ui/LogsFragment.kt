package org.traccar.client.trailblazer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.traccar.client.R
import org.traccar.client.trailblazer.util.Logger

class LogsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_logs, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_logs)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = LogsAdapter(Logger.getLogs())

        return view
    }
}
