package code.name.monkey.retromusic.fragments.home

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import code.name.monkey.retromusic.databinding.FragmentHomeBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout

class HomeBinding(
    homeBinding: FragmentHomeBinding
) {
    val root = homeBinding.root
    val appBarLayout: AppBarLayout = homeBinding.appBarLayout
    val topTabLayout: TabLayout = homeBinding.topTabLayout
    val homePager: ViewPager2 = homeBinding.homePager

    /** Legacy references kept for callers that still read them. */
    val container: View = homeBinding.homePager
    val contentContainer: View = homeBinding.contentContainer
    val toolbar: com.google.android.material.appbar.MaterialToolbar = homeBinding.toolbar
    val userImage: code.name.monkey.retromusic.views.RetroShapeableImageView = homeBinding.userImage
    val bannerImage: ImageView? = null
    val lastAdded: MaterialButton? = null
    val topPlayed: MaterialButton? = null
    val actionShuffle: MaterialButton? = null
    val history: MaterialButton? = null
    val recyclerView: RecyclerView? = null
    val titleWelcome: TextView? = null
    val suggestions: View? = null
}
