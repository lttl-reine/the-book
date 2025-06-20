package com.example.thebook.ui.reader

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.thebook.R
import com.example.thebook.databinding.FragmentReaderBinding
import com.example.thebook.utils.EpubCacheManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.siegmann.epublib.domain.Resource
import nl.siegmann.epublib.epub.EpubReader
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ReaderFragment : Fragment() {
    private val TAG = "ReaderFragment"
    private var _binding: FragmentReaderBinding? = null
    private val binding get() = _binding!!

    private val args: ReaderFragmentArgs by navArgs()
    private lateinit var webView: WebView
    private var currentBook: nl.siegmann.epublib.domain.Book? = null
    private var currentChapterIndex: Int = 3
    private var extractedEpubDir: File? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupWebView()
        loadEpubBook()
        setupBottomControls()
    }

    private fun setupToolbar() {
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbarReader)
        (activity as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }
        binding.toolbarReader.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.ivToc.setOnClickListener {
            showTableOfContents()
        }

        binding.ivSettings.setOnClickListener {
            showReaderSettings()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = binding.epubContentContainer.findViewById(R.id.book_content_webview)
        webView.settings.apply {
            javaScriptEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }

        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                // Xử lý navigation trong EPUB
                url?.let {
                    if (it.contains("#")) {
                        // Anchor link trong cùng trang
                        return false
                    } else if (it.endsWith(".html") || it.endsWith(".xhtml")) {
                        // Link đến chapter khác
                        navigateToChapterByHref(it)
                        return true
                    }
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Có thể thêm JavaScript để customize hiển thị
                injectCustomCSS()
            }
        }
    }

    private fun loadEpubBook() {
        val epubFileUrl = args.epubFileUrl
        Log.d(TAG, "loadEpubBook: bookUrl = $epubFileUrl")

        //binding.progressBar?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val downloadedFile = downloadEpubFile(epubFileUrl, requireContext())
                if (downloadedFile != null) {
                    // Giải nén EPUB để có thể truy cập resources (ảnh, CSS, fonts...)
                    extractedEpubDir = extractEpubFile(downloadedFile, requireContext())

                    currentBook = withContext(Dispatchers.IO) {
                        EpubReader().readEpub(FileInputStream(downloadedFile))
                    }

                    Log.d(TAG, "Book loaded: ${currentBook?.title}")

                    withContext(Dispatchers.Main) {
                        //binding.progressBar?.visibility = View.GONE
                        displayChapter(currentChapterIndex)
                        (activity as AppCompatActivity).supportActionBar?.title =
                            currentBook?.title ?: "Sách"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        //binding.progressBar?.visibility = View.GONE
                        Toast.makeText(context, "Không thể tải xuống sách EPUB", Toast.LENGTH_LONG).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading EPUB: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    //binding.progressBar?.visibility = View.GONE
                    Toast.makeText(context, "Lỗi khi đọc sách: ${e.message}", Toast.LENGTH_LONG).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    private fun displayChapter(index: Int) {
        currentBook?.let { book ->
            if (index >= 0 && index < book.spine.spineReferences.size) {
                val spineReference = book.spine.spineReferences[index]
                val chapterResource: Resource = spineReference.resource

                try {
                    var chapterHtml = String(chapterResource.data, Charsets.UTF_8)

                    // Xử lý relative paths cho resources (ảnh, CSS...)
                    chapterHtml = processHtmlContent(chapterHtml, chapterResource.href)

                    val baseUrl = if (extractedEpubDir != null) {
                        "file://${extractedEpubDir!!.absolutePath}/"
                    } else {
                        "file:///android_asset/"
                    }

                    webView.loadDataWithBaseURL(
                        baseUrl,
                        chapterHtml,
                        "text/html",
                        "UTF-8",
                        null
                    )

                    currentChapterIndex = index
                    updateProgressControls()

                } catch (e: Exception) {
                    Log.e(TAG, "Error displaying chapter: ${e.message}", e)
                    Toast.makeText(context, "Lỗi hiển thị chương", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Không có chương nào để hiển thị", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processHtmlContent(html: String, chapterHref: String): String {
        // Xử lý relative paths trong HTML
        var processedHtml = html

        // Lấy base path của chapter hiện tại
        val chapterBasePath = if (chapterHref.contains("/")) {
            chapterHref.substring(0, chapterHref.lastIndexOf("/") + 1)
        } else {
            ""
        }

        // Xử lý các đường dẫn tương đối cho ảnh và CSS
        processedHtml = processedHtml.replace(
            Regex("""src\s*=\s*["'](?!http)([^"']+)["']"""),
            """src="$chapterBasePath$1""""
        )

        processedHtml = processedHtml.replace(
            Regex("""href\s*=\s*["'](?!http)([^"'#]+)["']"""),
            """href="$chapterBasePath$1""""
        )

        return processedHtml
    }

    private fun navigateToChapterByHref(href: String) {
        currentBook?.let { book ->
            book.spine.spineReferences.forEachIndexed { index, spineRef ->
                if (spineRef.resource.href.endsWith(href) ||
                    spineRef.resource.href.contains(href.substringBefore("#"))) {
                    displayChapter(index)
                    return
                }
            }
        }
    }

    private fun injectCustomCSS() {
        val css = """
            javascript:(function() {
                var style = document.createElement('style');
                style.innerHTML = `
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        line-height: 1.6;
                        margin: 16px;
                        background-color: #ffffff;
                        color: #333333;
                    }
                    img { 
                        max-width: 100% !important; 
                        height: auto !important; 
                    }
                    p { 
                        margin-bottom: 1em; 
                        text-align: justify;
                    }
                `;
                document.head.appendChild(style);
            })()
        """.trimIndent()

        webView.evaluateJavascript(css, null)
    }

    private fun setupBottomControls() {
        binding.seekBarPageProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && currentBook != null) {
                    val totalChapters = currentBook!!.spine.spineReferences.size
                    val newChapterIndex = (progress * (totalChapters - 1)) / 100
                    if (newChapterIndex != currentChapterIndex) {
                        displayChapter(newChapterIndex)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Navigation buttons
//        binding.btnPrevChapter?.setOnClickListener {
//            if (currentChapterIndex > 0) {
//                displayChapter(currentChapterIndex - 1)
//            }
//        }
//
//        binding.btnNextChapter?.setOnClickListener {
//            currentBook?.let { book ->
//                if (currentChapterIndex < book.spine.spineReferences.size - 1) {
//                    displayChapter(currentChapterIndex + 1)
//                }
//            }
//        }

        binding.epubContentContainer.setOnClickListener {
            toggleControlsVisibility()
        }
    }

    private fun toggleControlsVisibility() {
        if (binding.appBarLayout.visibility == View.VISIBLE) {
            binding.appBarLayout.visibility = View.GONE
            binding.bottomControlsOverlay.visibility = View.GONE
        } else {
            binding.appBarLayout.visibility = View.VISIBLE
            binding.bottomControlsOverlay.visibility = View.VISIBLE
        }
    }

    private fun updateProgressControls() {
        currentBook?.let { book ->
            val totalChapters = book.spine.spineReferences.size
            if (totalChapters > 0) {
                val progress = (currentChapterIndex.toFloat() / (totalChapters - 1)) * 100
                binding.seekBarPageProgress.progress = progress.toInt()
                binding.tvPageNumber.text = "Chương ${currentChapterIndex + 1} / $totalChapters"
            }
        }
    }

    private fun showTableOfContents() {
        currentBook?.let { book ->
            val tableOfContents = book.tableOfContents.tocReferences
            if (tableOfContents.isNotEmpty()) {
                // TODO: Implement TOC dialog
                val tocItems = tableOfContents.map { it.title }
                // Hiển thị dialog với danh sách chapters
                Toast.makeText(context, "TOC: ${tocItems.joinToString(", ")}", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Sách không có mục lục", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showReaderSettings() {
        // TODO: Implement reader settings (font size, theme, etc.)
        Toast.makeText(context, "Cài đặt đọc sách", Toast.LENGTH_SHORT).show()
    }

    private suspend fun downloadEpubFile(url: String, context: Context): File? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/epub+zip, application/octet-stream, */*")
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream

                    // Sử dụng EpubCacheManager để lấy file tạm
                    val tempEpubFile = EpubCacheManager.getTempEpubFile(context)

                    // Xóa file cũ nếu tồn tại
                    EpubCacheManager.clearTempEpubFile(context)

                    val outputStream = FileOutputStream(tempEpubFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytes = 0L
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytes += bytesRead
                    }

                    inputStream.close()
                    outputStream.close()
                    connection.disconnect()

                    Log.d(TAG, "Downloaded EPUB to temp file: ${tempEpubFile.absolutePath}, size: ${totalBytes} bytes")
                    return@withContext tempEpubFile
                } else {
                    Log.e(TAG, "Server returned: ${connection.responseCode} ${connection.responseMessage}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading file: ${e.message}", e)
                null
            }
        }
    }

    private suspend fun extractEpubFile(epubFile: File, context: Context): File? {
        return withContext(Dispatchers.IO) {
            try {
                // Sử dụng EpubCacheManager để lấy thư mục giải nén
                val extractDir = EpubCacheManager.getTempExtractDir(context)

                // Xóa thư mục cũ nếu tồn tại
                EpubCacheManager.clearTempExtractDir(context)

                // Tạo thư mục mới
                extractDir.mkdirs()

                ZipInputStream(FileInputStream(epubFile)).use { zipInputStream ->
                    var entry: ZipEntry?
                    while (zipInputStream.nextEntry.also { entry = it } != null) {
                        val entryFile = File(extractDir, entry!!.name)

                        if (entry!!.isDirectory) {
                            entryFile.mkdirs()
                        } else {
                            entryFile.parentFile?.mkdirs()
                            FileOutputStream(entryFile).use { output ->
                                zipInputStream.copyTo(output)
                            }
                        }
                        zipInputStream.closeEntry()
                    }
                }

                Log.d(TAG, "Extracted EPUB to temp folder: ${extractDir.absolutePath}")
                return@withContext extractDir
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting EPUB: ${e.message}", e)
                null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webView.destroy()

        // Không cần cleanup vì chúng ta sử dụng file/folder tạm cố định
        // File tạm sẽ được overwrite khi đọc sách mới
        // Nếu muốn cleanup ngay lập tức (không khuyến khích):
        // cleanupTempFiles()

        _binding = null
    }

    /**
     * Hàm cleanup temp files (optional - chỉ gọi khi cần thiết)
     * Thường không cần gọi vì file tạm sẽ được tái sử dụng
     */
    private fun cleanupTempFiles() {
        try {
            val tempEpubFile = File(requireContext().cacheDir, EpubCacheManager.TEMP_EPUB_FILENAME)
            val tempExtractDir = File(requireContext().cacheDir, EpubCacheManager.TEMP_EXTRACT_DIRNAME)

            if (tempEpubFile.exists()) {
                tempEpubFile.delete()
                Log.d(TAG, "Cleaned up temp EPUB file")
            }

            if (tempExtractDir.exists()) {
                tempExtractDir.deleteRecursively()
                Log.d(TAG, "Cleaned up temp extract directory")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up temp files: ${e.message}")
        }
    }
}