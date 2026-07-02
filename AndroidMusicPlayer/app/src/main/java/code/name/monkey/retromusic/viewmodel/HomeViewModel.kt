package code.name.monkey.retromusic.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import code.name.monkey.retromusic.network.Result
import code.name.monkey.retromusic.network.models.*
import code.name.monkey.retromusic.repository.NeteaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel
 * 处理网易云音乐的首页数据加载
 */
class HomeViewModel(
    private val neteaseRepository: NeteaseRepository
) : ViewModel() {

    // 各模块数据 LiveData
    private val _banners = MutableLiveData<Result<List<BannerItem>>>()
    val banners: LiveData<Result<List<BannerItem>>> = _banners

    private val _personalizedPlaylists = MutableLiveData<Result<List<PersonalizedPlaylist>>>()
    val personalizedPlaylists: LiveData<Result<List<PersonalizedPlaylist>>> = _personalizedPlaylists

    private val _hotArtists = MutableLiveData<Result<List<HotArtist>>>()
    val hotArtists: LiveData<Result<List<HotArtist>>> = _hotArtists

    private val _newSongs = MutableLiveData<Result<List<NewSong>>>()
    val newSongs: LiveData<Result<List<NewSong>>> = _newSongs

    private val _newAlbums = MutableLiveData<Result<List<NeteaseAlbum>>>()
    val newAlbums: LiveData<Result<List<NeteaseAlbum>>> = _newAlbums

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // 歌单分类
    private val _playlistCategories = MutableLiveData<Result<NeteasePlaylistCategoryResponse>>()
    val playlistCategories: LiveData<Result<NeteasePlaylistCategoryResponse>> = _playlistCategories

    // 歌单列表（分类页用）
    private val _playlistList = MutableLiveData<Result<NeteasePlaylistListResponse>>()
    val playlistList: LiveData<Result<NeteasePlaylistListResponse>> = _playlistList

    // 排行榜
    private val _toplist = MutableLiveData<Result<List<ToplistItem>>>()
    val toplist: LiveData<Result<List<ToplistItem>>> = _toplist

    // 歌单详情
    private val _playlistDetail = MutableLiveData<Result<PlaylistDetail>>()
    val playlistDetail: LiveData<Result<PlaylistDetail>> = _playlistDetail

    init {
        loadHomeData()
    }

    /**
     * 加载首页所有数据（并行加载，提升性能）
     */
    fun loadHomeData() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)

            awaitAll(
                async { loadBanners() },
                async { loadPersonalizedPlaylists() },
                async { loadHotArtists() },
                async { loadNewSongs() },
                async { loadNewAlbums() }
            )

            _isLoading.postValue(false)
        }
    }

    /**
     * 刷新首页数据（下拉刷新用）
     */
    fun refreshHomeData() {
        loadHomeData()
    }

    // ==================== 单独加载方法 ====================

    suspend fun loadBanners() {
        val result = neteaseRepository.getBanners()
        _banners.postValue(result)
    }

    suspend fun loadPersonalizedPlaylists(limit: Int = 18) {
        val result = neteaseRepository.getPersonalizedPlaylist(limit)
        _personalizedPlaylists.postValue(result)
    }

    suspend fun loadHotArtists(offset: Int = 0, limit: Int = 30) {
        val result = neteaseRepository.getHotSinger(offset, limit)
        _hotArtists.postValue(result)
    }

    suspend fun loadNewSongs(limit: Int = 12) {
        val result = neteaseRepository.getRecommendMusic(limit)
        _newSongs.postValue(result)
    }

    suspend fun loadNewAlbums() {
        val result = neteaseRepository.getNewAlbum()
        _newAlbums.postValue(result)
    }

    // ==================== 歌单分类 ====================

    fun loadPlaylistCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = neteaseRepository.getPlaylistCategory()
            _playlistCategories.postValue(result)
        }
    }

    /**
     * 按分类加载歌单列表
     */
    fun loadPlaylistByCategory(cat: String = "", limit: Int = 30, offset: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = neteaseRepository.getListByCat(cat, limit, offset)
            _playlistList.postValue(result)
        }
    }

    /**
     * 加载更多歌单（分页加载）
     */
    fun loadMorePlaylistByCategory(cat: String, currentOffset: Int, limit: Int = 30) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = neteaseRepository.getListByCat(cat, limit, currentOffset)
            _playlistList.postValue(result)
        }
    }

    // ==================== 排行榜 ====================

    fun loadToplist() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = neteaseRepository.getToplist()
            _toplist.postValue(result)
        }
    }

    // ==================== 歌单详情 ====================

    fun loadPlaylistDetail(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = neteaseRepository.getPlaylistDetail(playlistId)
            _playlistDetail.postValue(result)
        }
    }
}
