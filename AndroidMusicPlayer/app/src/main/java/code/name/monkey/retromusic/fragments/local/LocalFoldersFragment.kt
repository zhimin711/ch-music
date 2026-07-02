package code.name.monkey.retromusic.fragments.local

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.databinding.FragmentLocalFoldersBinding
import code.name.monkey.retromusic.providers.BlacklistStore
import code.name.monkey.retromusic.util.FileUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import com.google.android.material.textview.MaterialTextView

/**
 * "本地-文件夹" 子页：展示扫描起始目录、存储根、黑名单概况，
 * 让用户一眼看得出"本地音乐从哪里扫、扫过哪些位置"。
 * 深度浏览仍走独立 FoldersFragment（点击"打开文件夹浏览器"）。
 */
class LocalFoldersFragment : Fragment(R.layout.fragment_local_folders) {

    private var _binding: FragmentLocalFoldersBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLocalFoldersBinding.bind(view)

        binding.openFolderBrowserButton.setOnClickListener {
            findNavController().navigate(R.id.action_folder)
        }
        renderContent()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) renderContent()
    }

    private fun renderContent() {
        val start = PreferenceUtil.startDirectory
        binding.startDirectoryText.text = FileUtil.safeGetCanonicalPath(start)

        val saveLast = PreferenceUtil.saveLastDirectory
        binding.lastDirectoryText.isVisible = true
        binding.lastDirectoryText.text = if (saveLast) {
            val last = FileUtil.safeGetCanonicalPath(PreferenceUtil.lastDirectory)
            getString(R.string.folder_scan_last_directory, last)
        } else {
            getString(R.string.folder_scan_last_directory_disabled)
        }

        renderStorageRoots()
        renderBlacklist()
    }

    private fun renderStorageRoots() {
        val container: LinearLayout = binding.storageRootsContainer
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)

        val roots = runCatching { FileUtil.listRoots() }.getOrNull().orEmpty()
        if (roots.isEmpty()) {
            addRow(inflater, container, getString(R.string.folder_scan_no_roots), null)
            return
        }
        roots.forEach { storage ->
            val title = runCatching { storage.title }.getOrNull().orEmpty()
            val path = runCatching { FileUtil.safeGetCanonicalPath(storage.file) }.getOrNull()
            addRow(inflater, container, title, path)
        }
    }

    private fun addRow(
        inflater: LayoutInflater,
        parent: ViewGroup,
        title: String,
        subtitle: String?
    ) {
        val row = inflater.inflate(R.layout.item_local_folder_row, parent, false)
        row.findViewById<MaterialTextView>(R.id.folderRowTitle).text = title
        val subtitleView = row.findViewById<MaterialTextView>(R.id.folderRowSubtitle)
        if (subtitle.isNullOrBlank()) {
            subtitleView.isVisible = false
        } else {
            subtitleView.isVisible = true
            subtitleView.text = subtitle
        }
        parent.addView(row)
    }

    private fun renderBlacklist() {
        val ctx = requireContext()
        val count = runCatching { BlacklistStore.getInstance(ctx).paths.size }.getOrDefault(0)
        binding.blacklistSummary.text = if (count == 0) {
            getString(R.string.folder_scan_blacklist_empty)
        } else {
            getString(R.string.folder_scan_blacklist_count, count)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
