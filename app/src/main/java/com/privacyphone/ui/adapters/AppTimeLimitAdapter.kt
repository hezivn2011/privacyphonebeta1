package com.privacyphone.ui.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.privacyphone.R
import com.privacyphone.data.prefs.AppPreferences
import com.privacyphone.model.AppInfo
import com.privacyphone.util.UsageStatsHelper

class AppTimeLimitAdapter(
    private val prefs: AppPreferences,
    private val onLimitChanged: (AppInfo, Int) -> Unit
) : ListAdapter<AppInfo, AppTimeLimitAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(a: AppInfo, b: AppInfo) = a.packageName == b.packageName
            override fun areContentsTheSame(a: AppInfo, b: AppInfo) = a == b
        }
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.iv_app_icon)
        val name: TextView = itemView.findViewById(R.id.tv_app_name)
        val usedToday: TextView = itemView.findViewById(R.id.tv_used_today)
        val seekBar: SeekBar = itemView.findViewById(R.id.seekbar_time)
        val etTime: EditText = itemView.findViewById(R.id.et_time_input)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_app_time, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = getItem(position)

        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.appName
        holder.usedToday.text = "Hôm nay: ${UsageStatsHelper.formatMinutesShort(app.usedTodayMinutes)}"

        val limit = app.timeLimitMinutes
        holder.seekBar.progress = minOf(limit, 180)

        // Format: show hours if >= 60 min
        holder.etTime.setText(formatLimit(limit))

        // Prevent recursive updates
        var updating = false

        holder.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser && !updating) {
                    updating = true
                    val mins = if (progress == 0) 1 else progress
                    holder.etTime.setText(formatLimit(mins))
                    onLimitChanged(app, mins)
                    updating = false
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        holder.etTime.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (updating) return
                val text = s.toString().replace("h", "").replace("m", "").trim()
                val mins = text.toIntOrNull() ?: return
                if (mins in 1..480) {
                    updating = true
                    holder.seekBar.progress = minOf(mins, 180)
                    onLimitChanged(app, mins)
                    updating = false
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun formatLimit(minutes: Int): String {
        return if (minutes >= 60) "${minutes / 60}h" else "${minutes}m"
    }
}
