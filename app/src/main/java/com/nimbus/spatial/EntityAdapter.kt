package com.nimbus.spatial

import com.nimbus.spatial.EntityItem
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class EntityAdapter(private val items: MutableList<EntityItem>) :
    RecyclerView.Adapter<EntityAdapter.EntityViewHolder>() {

    inner class EntityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val entityName: TextView = view.findViewById(R.id.tv_entity_name)
        val lockIcon: ImageView = view.findViewById(R.id.iv_lock_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntityViewHolder {
        Log.d("EntityAdapter", "Creating view holder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_entity, parent, false)
        return EntityViewHolder(view)
    }

    override fun onBindViewHolder(holder: EntityViewHolder, position: Int) {
        val item = items[position]
        Log.d("EntityAdapter", "Binding item at position $position: ${item.fileName}, isAccessible: ${item.isAccessible}")
        holder.entityName.text = item.fileName

        // Show lock icon if inaccessible
        if (!item.isAccessible) {
            holder.itemView.alpha = 0.5f
            holder.lockIcon.visibility = View.VISIBLE
        } else {
            holder.itemView.alpha = 1.0f
            holder.lockIcon.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            if (item.isAccessible) {
                val context = holder.itemView.context
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(Uri.parse(item.localUri), item.mimeType)
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("EntityAdapter", "Failed to open file: ${e.message}", e)
                    Toast.makeText(context, "No app to open this file type", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(holder.itemView.context, "You're outside the geofence", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int {
        Log.d("EntityAdapter", "Item count: ${items.size}")
        return items.size
    }
}