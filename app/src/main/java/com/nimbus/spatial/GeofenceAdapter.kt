package com.nimbus.spatial

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat

class GeofenceAdapter(
    private val geofenceList: MutableList<GeofenceArea>,
    private val onDelete: (GeofenceArea) -> Unit,
    private val onClick: (GeofenceArea) -> Unit
) : RecyclerView.Adapter<GeofenceAdapter.GeofenceViewHolder>() {

    inner class GeofenceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tv_area_name)
        val delete: ImageView = view.findViewById(R.id.iv_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GeofenceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_geofence_area, parent, false)
        return GeofenceViewHolder(view)
    }

    override fun onBindViewHolder(holder: GeofenceViewHolder, position: Int) {
        val item = geofenceList[position]

        holder.name.text = item.name

        // Click opens details
        holder.itemView.setOnClickListener {
            onClick(item)
        }

        // Delete button
        holder.delete.setOnClickListener {
            onDelete(item)
        }
    }

    override fun getItemCount(): Int = geofenceList.size


}
