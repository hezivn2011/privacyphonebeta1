package com.privacyphone.ui.fragments

import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.privacyphone.R
import com.privacyphone.data.prefs.AppPreferences
import com.privacyphone.ui.views.UsageBarChartView
import com.privacyphone.util.GeminiHelper
import com.privacyphone.util.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var prefs: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = AppPreferences(requireContext())

        updateDeviceName(view)
        loadUsageStats(view)
        setupAiEvaluate(view)

        view.findViewById<Button>(R.id.btn_refresh_ai).setOnClickListener {
            loadAiEvaluate(view, forceRefresh = true)
        }
    }

    private fun updateDeviceName(view: View) {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        view.findViewById<TextView>(R.id.tv_device_name).text = deviceName
    }

    private fun loadUsageStats(view: View) {
        lifecycleScope.launch {
            val totalMinutes = withContext(Dispatchers.IO) {
                UsageStatsHelper.getTodayUsageMinutes(requireContext())
            }
            val weeklyData = withContext(Dispatchers.IO) {
                UsageStatsHelper.getWeeklyUsage(requireContext())
            }

            // Update UI
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            view.findViewById<TextView>(R.id.tv_total_usage).text =
                if (hours > 0) "${hours}h${mins}'" else "${mins}'"

            view.findViewById<TextView>(R.id.tv_sensitive_count).text =
                prefs.sensitiveCount.toString()

            // Update chart
            val chart = view.findViewById<UsageBarChartView>(R.id.chart_usage)
            chart.setData(weeklyData)
        }
    }

    private fun setupAiEvaluate(view: View) {
        // Load cached first
        val cached = prefs.aiEvaluateCache
        if (cached.isNotEmpty()) {
            view.findViewById<TextView>(R.id.tv_ai_evaluate).text = cached
        }
        loadAiEvaluate(view)
    }

    private fun loadAiEvaluate(view: View, forceRefresh: Boolean = false) {
        val aiText = view.findViewById<TextView>(R.id.tv_ai_evaluate)
        val loading = view.findViewById<ProgressBar>(R.id.ai_loading)

        if (!forceRefresh && prefs.aiEvaluateCache.isNotEmpty()) return

        loading.isVisible = true
        aiText.text = "Đang phân tích..."

        lifecycleScope.launch {
            val totalMinutes = withContext(Dispatchers.IO) {
                UsageStatsHelper.getTodayUsageMinutes(requireContext())
            }
            val apps = withContext(Dispatchers.IO) {
                UsageStatsHelper.getInstalledApps(requireContext())
                    .take(5)
                    .map { Pair(it.appName, it.usedTodayMinutes) }
            }

            val result = GeminiHelper.evaluateUsage(totalMinutes, apps, prefs.sensitiveCount)

            withContext(Dispatchers.Main) {
                loading.isVisible = false
                aiText.text = result
                prefs.aiEvaluateCache = result
            }
        }
    }
}
