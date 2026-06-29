package com.privacyphone.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.privacyphone.R
import com.privacyphone.model.MediaItem
import java.io.File

class SensitiveMediaAdapter : ListAdapter<MediaItem, SensitiveMediaAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(a: MediaItem, b: MediaItem) = a.path == b.path
            override fun areContentsTheSame(a: MediaItem, b: MediaItem) = a == b
        }
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.iv_media)
        val score: TextView = itemView.findViewById(R.id.tv_score)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        Glide.with(holder.image)
            .load(File(item.path))
            .centerCrop()
            .into(holder.image)

        val pct = (item.sensitivityScore * 100).toInt()
        holder.score.text = "${pct}%"
    }
}
