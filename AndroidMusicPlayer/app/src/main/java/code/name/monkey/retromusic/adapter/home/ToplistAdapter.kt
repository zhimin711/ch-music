package code.name.monkey.retromusic.adapter.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.databinding.ItemToplistCardBinding
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.network.models.ToplistItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

/**
 * 排行榜卡片适配器
 */
class ToplistAdapter(
    private var items: List<ToplistItem>,
    private val onItemClick: (playlistId: Long, playlistName: String) -> Unit
) : RecyclerView.Adapter<ToplistAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemToplistCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ToplistItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemToplistCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ToplistItem) {
            binding.toplistTitle.text = item.name
            binding.updateFrequency.text = item.updateFrequency ?: "实时更新"

            Glide.with(itemView.context)
                .load(item.coverImgUrl)
                .transform(CenterCrop(), RoundedCorners(16))
                .into(binding.toplistCover)

            itemView.setOnClickListener {
                onItemClick(item.id, item.name)
            }
        }
    }
}
