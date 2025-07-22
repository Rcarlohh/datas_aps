package com.example.datasimulator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DataAdapter(private var dataList: List<DataItem>) : RecyclerView.Adapter<DataAdapter.DataViewHolder>() {

    class DataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeIcon: ImageView = itemView.findViewById(R.id.typeIcon)
        val typeText: TextView = itemView.findViewById(R.id.typeText)
        val valueText: TextView = itemView.findViewById(R.id.valueText)
        val timestampText: TextView = itemView.findViewById(R.id.timestampText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data, parent, false)
        return DataViewHolder(view)
    }

    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        val dataItem = dataList[position]
        
        holder.typeText.text = dataItem.type
        holder.valueText.text = "Valor: ${dataItem.getFormattedValue()}"
        holder.timestampText.text = dataItem.getFormattedTimestamp()
        
        // Asignar icono y color según el tipo de dato
        when (dataItem.type.lowercase()) {
            "temperatura" -> {
                holder.typeIcon.setImageResource(android.R.drawable.ic_menu_compass)
                holder.typeIcon.setColorFilter(holder.itemView.context.getColor(R.color.icon_red))
            }
            "humedad" -> {
                holder.typeIcon.setImageResource(android.R.drawable.ic_menu_help)
                holder.typeIcon.setColorFilter(holder.itemView.context.getColor(R.color.icon_blue))
            }
            "presión" -> {
                holder.typeIcon.setImageResource(android.R.drawable.ic_menu_zoom)
                holder.typeIcon.setColorFilter(holder.itemView.context.getColor(R.color.icon_green))
            }
            "velocidad" -> {
                holder.typeIcon.setImageResource(android.R.drawable.ic_menu_directions)
                holder.typeIcon.setColorFilter(holder.itemView.context.getColor(R.color.icon_orange))
            }
            else -> {
                holder.typeIcon.setImageResource(android.R.drawable.ic_menu_info_details)
                holder.typeIcon.setColorFilter(holder.itemView.context.getColor(R.color.icon_purple))
            }
        }
    }

    override fun getItemCount(): Int = dataList.size

    fun updateData(newDataList: List<DataItem>) {
        dataList = newDataList
        notifyDataSetChanged()
    }
} 