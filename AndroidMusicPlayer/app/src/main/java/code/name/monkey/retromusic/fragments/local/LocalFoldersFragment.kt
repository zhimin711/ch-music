package code.name.monkey.retromusic.fragments.local

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentLocalListBinding

/**
 * "本地-文件夹" 子页：直接把 fragment_local_list 的空态区当作跳转入口，
 * 由于 FoldersFragment 依赖自己的 AppBar/ActionBar/BreadCrumb 等重资源，
 * 无法轻量内嵌进 ViewPager；用户点击后跳转到独立的文件夹页面。
 */
class LocalFoldersFragment : Fragment(R.layout.fragment_local_list) {

    private var _binding: FragmentLocalListBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLocalListBinding.bind(view)

        binding.recyclerView.isVisible = false
        binding.shuffleButton.isVisible = false
        binding.emptyContainer.isVisible = true
        binding.emptyText.setText(R.string.folders)

        binding.emptyContainer.setOnClickListener {
            findNavController().navigate(R.id.action_folder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
