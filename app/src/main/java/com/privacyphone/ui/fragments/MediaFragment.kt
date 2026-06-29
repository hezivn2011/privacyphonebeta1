package com.privacyphone.ui.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial
import com.privacyphone.R
import com.privacyphone.data.prefs.AppPreferences
import com.privacyphone.model.MediaItem
import com.privacyphone.model.MediaType
import com.privacyphone.ui.adapters.SensitiveMediaAdapter
import com.privacyphone.util.GeminiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Base64

class MediaFragment : Fragment() {

    private lateinit var prefs: AppPreferences
    private lateinit var adapter: SensitiveMediaAdapter
    private val sensitiveItems = mutableListOf<MediaItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_media, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = AppPreferences(requireContext())

        setupRecyclerView(view)
        updateStats(view)

        view.findViewById<SwitchMaterial>(R.id.switch_scan).apply {
            isChecked = prefs.scanEnabled
            setOnCheckedChangeListener { _, checked ->
                prefs.scanEnabled = checked
            }
        }

        view.findViewById<Button>(R.id.btn_scan_now).setOnClickListener {
            checkPermissionsAndScan(view)
        }
    }

    private fun setupRecyclerView(view: View) {
        adapter = SensitiveMediaAdapter()
        view.findViewById<RecyclerView>(R.id.rv_sensitive_media).apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = this@MediaFragment.adapter
        }
    }

    private fun updateStats(view: View) {
        view.findViewById<TextView>(R.id.tv_sensitive_count).text = prefs.sensitiveCount.toString()
        view.findViewById<TextView>(R.id.tv_blocked_today).text = prefs.blockedToday.toString()
    }

    private fun checkPermissionsAndScan(view: View) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            startScan(view)
        } else {
            requestPermissions(arrayOf(permission), 100)
        }
    }

    private fun startScan(view: View) {
        val progressBar = view.findViewById<LinearProgressIndicator>(R.id.scan_progress)
        val aiText = view.findViewById<TextView>(R.id.tv_ai_media)
        val scanBtn = view.findViewById<Button>(R.id.btn_scan_now)

        progressBar.isVisible = true
        progressBar.isIndeterminate = true
        scanBtn.isEnabled = false
        aiText.text = "Đang quét hình ảnh và video..."
        sensitiveItems.clear()

        lifecycleScope.launch {
            val images = withContext(Dispatchers.IO) { getRecentImages() }
            val totalCount = minOf(images.size, 20) // limit to 20 for demo
            var scanned = 0
            var found = 0

            progressBar.isIndeterminate = false
            progressBar.max = totalCount

            for (imagePath in images.take(totalCount)) {
                val result = withContext(Dispatchers.IO) {
                    try {
                        val bmp = BitmapFactory.decodeFile(imagePath) ?: return@withContext null
                        val scaled = Bitmap.createScaledBitmap(bmp, 512, 512, true)
                        val baos = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                        val base64 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Base64.getEncoder().encodeToString(baos.toByteArray())
                        } else {
                            android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT)
                        }
                        GeminiHelper.analyzeMediaSafety(base64)
                    } catch (e: Exception) {
                        null
                    }
                }

                scanned++
                progressBar.progress = scanned

                if (result?.first == true) {
                    found++
                    sensitiveItems.add(MediaItem(imagePath, MediaType.IMAGE, result.second, "Nhạy cảm"))
                    withContext(Dispatchers.Main) {
                        adapter.submitList(sensitiveItems.toList())
                        view.findViewById<TextView>(R.id.tv_sensitive_count).text = found.toString()
                    }
                }
            }

            // Done
            prefs.sensitiveCount = found
            progressBar.isVisible = false
            scanBtn.isEnabled = true
            aiText.text = if (found == 0)
                "✅ Không phát hiện nội dung nhạy cảm trong $scanned ảnh đã quét."
            else
                "⚠️ Phát hiện $found nội dung nhạy cảm trong $scanned ảnh. Hãy xem xét xoá chúng."
        }
    }

    private fun getRecentImages(): List<String> {
        val paths = mutableListOf<String>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_ADDED)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        requireContext().contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (cursor.moveToNext() && paths.size < 50) {
                paths.add(cursor.getString(dataCol))
            }
        }
        return paths
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 100 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            view?.let { startScan(it) }
        }
    }
}
