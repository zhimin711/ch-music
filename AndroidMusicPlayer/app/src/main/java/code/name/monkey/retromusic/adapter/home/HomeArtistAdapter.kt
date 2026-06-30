package code.name.monkey.retromusic.adapter.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import code.name.monkey.retromusic.databinding.ItemHomeArtistBinding
import code.name.monkey.retromusic.extensions.showToast
import code.name.monkey.retromusic.network.models.HotArtist
import com.bumptech.glide.Glide

/**
 * 首页热门歌手横向滚动适配器
 */
class HomeArtistAdapter(
    private var artists: List<HotArtist>
) : RecyclerView.Adapter<HomeArtistAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeArtistBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(artists[position])
    }

    override fun getItemCount(): Int = artists.size

    fun updateData(newArtists: List<HotArtist>) {
        artists = newArtists
        notifyDataSetChanged()
    }

    inner class ViewHolder(
        private val binding: ItemHomeArtistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(artist: HotArtist) {
            binding.artistName.text = artist.name

            // 使用 picUrl 或 img1v1Url
            val imageUrl = artist.picUrl.takeIf { !it.isNullOrEmpty() }
                ?: artist.img1v1Url.takeIf { !it.isNullOrEmpty() }

            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .circleCrop()
                    .into(binding.artistAvatar)
            }

            itemView.setOnClickListener {
                // TODO: 跳转到歌手详情
                itemView.context.showToast("歌手: ${artist.name}")
            }
        }
    }
}
