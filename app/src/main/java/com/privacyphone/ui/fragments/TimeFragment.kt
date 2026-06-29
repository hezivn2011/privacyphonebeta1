package com.privacyphone.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.privacyphone.R
import com.privacyphone.data.prefs.AppPreferences
import com.privacyphone.model.AppInfo
import com.privacyphone.ui.adapters.AppTimeLimitAdapter
import com.privacyphone.util.GeminiHelper
import com.privacyphone.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimeFragment : Fragment() {

    private lateinit var prefs: AppPreferences
    private lateinit var adapter: AppTimeLimitAdapter
    private var allApps = listOf<AppInfo>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_time, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = AppPreferences(requireContext())

        setupRecyclerView(view)
        loadApps(view)
        setupSearch(view)
        loadAiTip(view)
    }

    private fun setupRecyclerView(view: View) {
        adapter = AppTimeLimitAdapter(prefs) { app, minutes ->
            prefs.setAppTimeLimit(app.packageName, minutes)
        }
        view.findViewById<RecyclerView>(R.id.rv_apps).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TimeFragment.adapter
        }
    }

    private fun loadApps(view: View) {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                UsageStatsHelper.getInstalledApps(requireContext()).map { app ->
                    app.copy(timeLimitMinutes = prefs.getAppTimeLimit(app.packageName))
                }
            }
            allApps = apps
            adapter.submitList(apps)
        }
    }

    private fun setupSearch(view: View) {
        view.findViewById<TextInputEditText>(R.id.et_search_app).addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val query = s.toString().lowercase().trim()
                    val filtered = if (query.isEmpty()) allApps
                    else allApps.filter { it.appName.lowercase().contains(query) }
                    adapter.submitList(filtered)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
        )
    }

    private fun loadAiTip(view: View) {
        lifecycleScope.launch {
            val tip = withContext(Dispatchers.IO) {
                GeminiHelper.evaluateUsage(
                    UsageStatsHelper.getTodayUsageMinutes(requireContext()),
                    listOf(), 0
                )
            }
            view.findViewById<TextView>(R.id.tv_ai_tip).text = tip
        }
    }
}
