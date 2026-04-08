package com.fam4k007.videoplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.Anime4KManager

class Anime4KModeAdapter(
    private val modes: List<Anime4KManager.Mode>,
    private var selectedMode: Anime4KManager.Mode,
    private val onModeSelected: (Anime4KManager.Mode) -> Unit
) : RecyclerView.Adapter<Anime4KModeAdapter.ModeViewHolder>() {

    inner class ModeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardAnime4KMode)
        val tvModeIcon: TextView = itemView.findViewById(R.id.tvModeIcon)
        val tvModeName: TextView = itemView.findViewById(R.id.tvModeName)
        val tvModeDescription: TextView = itemView.findViewById(R.id.tvModeDescription)
        val ivModeSelected: ImageView = itemView.findViewById(R.id.ivModeSelected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anime4k_mode, parent, false)
        return ModeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ModeViewHolder, position: Int) {
        val mode = modes[position]
        val isSelected = mode == selectedMode
        val context = holder.itemView.context
        
        // 使用真实的模式描述
        val (icon, name, description) = when (mode) {
            Anime4KManager.Mode.OFF -> ModeInfo(context.getString(R.string.anime4k_mode_off_name), context.getString(R.string.anime4k_mode_off_desc), context.getString(R.string.anime4k_off))
            Anime4KManager.Mode.A -> ModeInfo(context.getString(R.string.anime4k_mode_a_name), context.getString(R.string.anime4k_mode_a_desc_short), context.getString(R.string.anime4k_mode_a_hint))
            Anime4KManager.Mode.B -> ModeInfo(context.getString(R.string.anime4k_mode_b_name), context.getString(R.string.anime4k_mode_b_desc_short), context.getString(R.string.anime4k_mode_b_hint))
            Anime4KManager.Mode.C -> ModeInfo(context.getString(R.string.anime4k_mode_c_name), context.getString(R.string.anime4k_mode_c_desc_short), context.getString(R.string.anime4k_mode_c_hint))
            Anime4KManager.Mode.A_PLUS -> ModeInfo(context.getString(R.string.anime4k_mode_a_plus_name), context.getString(R.string.anime4k_mode_a_plus_desc), context.getString(R.string.anime4k_mode_a_plus_hint))
            Anime4KManager.Mode.B_PLUS -> ModeInfo(context.getString(R.string.anime4k_mode_b_plus_name), context.getString(R.string.anime4k_mode_b_plus_desc), context.getString(R.string.anime4k_mode_b_plus_hint))
            Anime4KManager.Mode.C_PLUS -> ModeInfo(context.getString(R.string.anime4k_mode_c_plus_name), context.getString(R.string.anime4k_mode_c_plus_desc), context.getString(R.string.anime4k_mode_c_plus_hint))
        }
        
        holder.tvModeIcon.text = icon
        holder.tvModeName.text = name
        holder.tvModeDescription.text = description
        
        // 选中状态
        holder.ivModeSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.cardView.setCardBackgroundColor(
            holder.itemView.context.getColor(
                if (isSelected) R.color.anime4k_mode_selected else R.color.anime4k_mode_normal
            )
        )
        
        // 点击事件
        holder.cardView.setOnClickListener {
            val oldPosition = modes.indexOf(selectedMode)
            selectedMode = mode
            notifyItemChanged(oldPosition)
            notifyItemChanged(position)
            onModeSelected(mode)
        }
    }

    override fun getItemCount(): Int = modes.size
    
    private data class ModeInfo(
        val icon: String,
        val name: String,
        val description: String
    )
}
