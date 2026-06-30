package code.name.monkey.retromusic.adapter.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.databinding.ItemHomePlaylistCardBinding
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.network.models.PersonalizedPlaylist
import code.name.monkey.retromusic.network.models.PlaylistItem
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

/**
 * 首页推荐歌单卡片适配器
 * 支持显示 PersonalizedPlaylist 和 PlaylistItem 两种类型
 */
class HomePlaylistCardAdapter(
    private var items: List<Any>,
    private val onItemClick: (playlistId: Long, playlistName: String) -> Unit
) : RecyclerView.Adapter<HomePlaylistCardAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomePlaylistCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemHomePlaylistCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Any) {
            when (item) {
                is PersonalizedPlaylist -> bindPersonalized(item)
                is PlaylistItem -> bindPlaylistItem(item)
            }

            itemView.setOnClickListener {
                val (id, name) = when (item) {
                    is PersonalizedPlaylist -> item.id to item.name
                    is PlaylistItem -> item.id to item.name
                    else -> return@setOnClickListener
                }
                onItemClick(id, name)
            }
        }

        private fun bindPersonalized(item: PersonalizedPlaylist) {
            binding.playlistTitle.text = item.name
            binding.playCount.text = formatPlayCount(item.playCount)

            Glide.with(itemView.context)
                .load(item.picUrl)
                .transform(CenterCrop(), RoundedCorners(16))
                .into(binding.playlistCover)
        }

        private fun bindPlaylistItem(item: PlaylistItem) {
            binding.playlistTitle.text = item.name
            binding.playCount.text = formatPlayCount(item.playCount)

            Glide.with(itemView.context)
                .load(item.coverImgUrl)
                .transform(CenterCrop(), RoundedCorners(16))
                .into(binding.playlistCover)
        }

        private fun formatPlayCount(count: Long): String {
            return when {
                count >= 100_000_000 -> "${count / 100_000_000}亿"
                count >= 10_000 -> "${count / 10_000}万"
                else -> count.toString()
            }
        }
    }
}
