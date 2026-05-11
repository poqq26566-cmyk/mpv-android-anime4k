package com.fam4k007.videoplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.fam4k007.videoplayer.R
import com.fam4k007.videoplayer.domain.player.Anime4KManager

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
        
        // 使用真实的模式描述
        val (icon, name, description) = when (mode) {
            Anime4KManager.Mode.OFF -> ModeInfo("关", "原始画质", "禁用 Anime4K")
            Anime4KManager.Mode.A -> ModeInfo("A", "强力重建", "高模糊、重采样伪影")
            Anime4KManager.Mode.B -> ModeInfo("B", "柔和重建", "低模糊、下采样振铃")
            Anime4KManager.Mode.C -> ModeInfo("C", "降噪处理", "去噪点、提升清晰度")
            Anime4KManager.Mode.A_PLUS -> ModeInfo("A+", "双重强化", "最强线条还原（慢）")
            Anime4KManager.Mode.B_PLUS -> ModeInfo("B+", "双重柔和", "更自然效果（慢）")
            Anime4KManager.Mode.C_PLUS -> ModeInfo("C+", "降噪强化", "去噪+线条重建（慢）")
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
