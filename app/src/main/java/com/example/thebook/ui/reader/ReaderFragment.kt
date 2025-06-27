package com.example.thebook.ui.reader

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.thebook.R
import com.example.thebook.databinding.FragmentReaderBinding
import com.example.thebook.data.repository.ReadingProgressRepository
import com.example.thebook.data.model.ReadingProgress
import com.example.thebook.data.repository.BookRepository
import com.example.thebook.utils.EpubCacheManager
import com.example.thebook.utils.Resources
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
    private var currentChapterIndex: Int = 0
    private var extractedEpubDir: File? = null

    // Book Repository for update total page
    private lateinit var bookRepository: BookRepository

    // Reading Progress
    private lateinit var readingProgressRepository: ReadingProgressRepository
    private var currentReadingProgress: ReadingProgress? = null
    private var progressSaveJob: Job? = null
    private var lastSavedPage: Int = -1

    // TOC Panel variables
    private var isTocPanelVisible = false
    private lateinit var tocAdapter: TocAdapter
    private var currentChapterHref: String? = null

    // Reader settings
    private var isSettingsPanelVisible = false
    private var currentFontSize = 16 // sp
    private var currentTheme = "light" // light, dark, sepia
    private var currentFontFamily = "default" // default, serif, sans-serif

    // Scroll tracking
    private var isUserScrolling = false
    private var currentScrollPosition = 0
    private var maxScrollPosition = 100

    private lateinit var gestureDetector: GestureDetector

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize repository
        readingProgressRepository = ReadingProgressRepository()
        bookRepository = BookRepository()

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                // Handle when tap the screen
                if (!isTocPanelVisible && !isSettingsPanelVisible) {
                    toggleControlsVisibility()
                }
                return true
            }
        })

        // Set OnTouchListener to transfer touch event from WebView to GestureDetector
        binding.bookContentWebview.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        setupSystemUI()
        setupToolbar()
        setupWebView()
        setupTocPanel()
        setupSettingsPanel()
        loadEpubBook()
        setupBottomControls()
        observeReadingProgress()
    }

    private fun setupSystemUI() {
        requireActivity().window.apply {
            statusBarColor = ContextCompat.getColor(requireContext(), R.color.primary_500)
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            binding.appBarLayout.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0
            )

            binding.bottomControlsOverlay.setPadding(
                binding.bottomControlsOverlay.paddingLeft,
                binding.bottomControlsOverlay.paddingTop,
                binding.bottomControlsOverlay.paddingRight,
                systemBars.bottom + 16
            )

            insets
        }
    }

    private fun setupToolbar() {
        (activity as AppCompatActivity).setSupportActionBar(binding.toolbarReader)
        (activity as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = ""
        }
        binding.toolbarReader.setNavigationOnClickListener {
            // Save progress before leaving
            saveCurrentProgress()
            findNavController().navigateUp()
        }

        binding.ivToc.setOnClickListener {
            toggleTocPanel()
        }

        binding.ivSettings.setOnClickListener {
            toggleSettingsPanel()
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
        }

        webView.addJavascriptInterface(JavaScriptInterface(), "Android")
        Log.d(TAG, "JavaScriptInterface 'Android' added in setupWebView().")

        webView.webViewClient = object : WebViewClient() {
            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let {
                    if (it.contains("#")) {
                        return false
                    } else if (it.endsWith(".html") || it.endsWith(".xhtml")) {
                        navigateToChapterByHref(it)
                        return true
                    }
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "WebViewClient: onPageFinished for URL: $url")
                injectCustomCSS()
                setupScrollTracking()

                // Auto-save progress after page loads
                scheduleProgressSave()
            }
        }
    }

    private fun setupScrollTracking() {
        val js = """
        javascript:(function() {
            console.log("Checking if Android is defined at script start:", typeof Android !== 'undefined');

            function updateScrollPosition() {
                var scrollTop = window.pageYOffset || document.documentElement.scrollTop;
                var scrollHeight = document.documentElement.scrollHeight - window.innerHeight;
                var scrollPercent = scrollHeight > 0 ? Math.round((scrollTop / scrollHeight) * 100) : 0;

                console.log("Scroll: scrollTop=" + scrollTop + ", scrollHeight=" + scrollHeight + ", window.innerHeight=" + window.innerHeight + ", scrollPercent=" + scrollPercent);

                if (typeof Android !== 'undefined' && Android.onScrollChanged) {
                    Android.onScrollChanged(scrollPercent, scrollTop, scrollHeight);
                } else {
                    console.error("Android.onScrollChanged is not available!");
                }

                if (scrollHeight <= 0 && typeof Android !== 'undefined' && Android.onReachedBottom) {
                    console.log("Chapter is too short to scroll. Automatically calling onReachedBottom().");
                    Android.onReachedBottom();
                }
            }

            window.addEventListener('scroll', function() {
                updateScrollPosition();

                var scrollTop = window.pageYOffset || document.documentElement.scrollTop;
                var scrollHeight = document.documentElement.scrollHeight - window.innerHeight;

                if (scrollHeight > 0 && (scrollTop >= scrollHeight - 5)) {
                    console.log("Reached bottom by scrolling! Calling Android.onReachedBottom()");
                    if (typeof Android !== 'undefined' && Android.onReachedBottom) {
                        Android.onReachedBottom();
                    } else {
                        console.error("Android.onReachedBottom is not available!");
                    }
                }
                else if (scrollTop <= 5) {
                    console.log("Reached top by scrolling! Calling Android.onReachedTop()");
                    if (typeof Android !== 'undefined' && Android.onReachedTop) {
                        Android.onReachedTop();
                    } else {
                        console.error("Android.onReachedTop is not available!");
                    }
                }
            });

            updateScrollPosition();
        })()
        """.trimIndent()

        webView.post {
            webView.evaluateJavascript(js, null)
            Log.d(TAG, "JavaScript for scroll tracking evaluated via webView.post().")
        }
    }

    inner class JavaScriptInterface {
        @android.webkit.JavascriptInterface
        fun onScrollChanged(percent: Int, scrollTop: Int, scrollHeight: Int) {
            activity?.runOnUiThread {
                Log.d(TAG, "JS: onScrollChanged - percent: $percent, scrollTop: $scrollTop, scrollHeight: $scrollHeight")
                if (!isUserScrolling) {
                    binding.seekBarPageProgress.progress = percent
                    currentScrollPosition = scrollTop
                    maxScrollPosition = scrollHeight
                }

                // Schedule progress save when user scrolls
                scheduleProgressSave()
            }
        }

        @android.webkit.JavascriptInterface
        fun onReachedBottom() {
            activity?.runOnUiThread {
                Log.d(TAG, "JS: onReachedBottom() called from WebView!")
                currentBook?.let { book ->
                    if (currentChapterIndex < book.spine.spineReferences.size - 1) {
                        Log.d(TAG, "Navigating to next chapter: ${currentChapterIndex + 1}")
                        displayChapter(currentChapterIndex + 1)
                    } else {
                        Log.d(TAG, "Reached end of book. Marking as finished.")
                        markBookAsFinished()
                    }
                }
            }
        }

        @android.webkit.JavascriptInterface
        fun onReachedTop() {
            activity?.runOnUiThread {
                currentBook?.let {
                    if (currentChapterIndex > 0) {
                        displayChapter(currentChapterIndex - 1)
                    } else {
                        Log.d(TAG, "Already at first chapter.")
                    }
                }
            }
        }
    }

    private fun observeReadingProgress() {
        val bookId = args.book.bookId
        lifecycleScope.launch {
            readingProgressRepository.getReadingProgress(bookId).collectLatest { resource ->
                when (resource) {
                    is Resources.Success-> {
                        currentReadingProgress = resource.data
                        resource.data?.let { progress ->
                            Log.d(TAG, "Reading progress loaded: page ${progress.lastReadPage}")
                            if (currentBook != null && progress.lastReadPage > 0) {
                                restoreReadingPosition(progress.lastReadPage)
                            }
                        }
                    }
                    is Resources.Error-> {
                        Log.e(TAG, "Error loading reading progress: ${resource.exception?.message}")
                    }
                    is Resources.Loading -> {
                        Log.d(TAG, "Loading reading progress...")
                    }
                }
            }
        }
    }

    private fun restoreReadingPosition(lastReadPage: Int) {
        // Convert page number back to chapter index
        // This is a simplified approach - you might want to store chapter index separately
        if (lastReadPage > 0 && lastReadPage != currentChapterIndex) {
            currentChapterIndex = lastReadPage.coerceAtMost(
                (currentBook?.spine?.spineReferences?.size ?: 1) - 1
            )
            displayChapter(currentChapterIndex)

            // Show toast to inform user
            Toast.makeText(
                context,
                "Đã khôi phục vị trí đọc: Chương ${currentChapterIndex + 1}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun scheduleProgressSave() {
        // Cancel previous save job
        progressSaveJob?.cancel()

        // Schedule new save job with delay to avoid frequent saves
        progressSaveJob = lifecycleScope.launch {
            delay(2000) // Wait 2 seconds before saving
            saveCurrentProgress()
        }
    }

    private fun saveCurrentProgress() {
        val bookId = args.book.bookId ?: return

        // Only save if page has changed
        if (lastSavedPage == currentChapterIndex) return

        lifecycleScope.launch {
            try {
                val result = readingProgressRepository.saveReadingProgress(
                    bookId = bookId,
                    lastReadPage = currentChapterIndex,
                    isCompleted = false
                )

                when (result) {
                    is com.example.thebook.utils.Resources.Success -> {
                        lastSavedPage = currentChapterIndex
                        Log.d(TAG, "Progress saved: chapter $currentChapterIndex")
                    }
                    is com.example.thebook.utils.Resources.Error -> {
                        Log.e(TAG, "Error saving progress: ${result.exception?.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception saving progress: ${e.message}")
            }
        }
    }

    private fun markBookAsFinished() {
        val bookId = args.book.bookId ?: return

        lifecycleScope.launch {
            try {
                val result = readingProgressRepository.markBookAsFinished(
                    bookId = bookId,
                    lastPage = currentChapterIndex
                )

                when (result) {
                    is com.example.thebook.utils.Resources.Success -> {
                        Toast.makeText(
                            context,
                            "Chúc mừng! Bạn đã hoàn thành cuốn sách này! 🎉",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d(TAG, "Book marked as finished")
                    }
                    is com.example.thebook.utils.Resources.Error -> {
                        Log.e(TAG, "Error marking book as finished: ${result.exception?.message}")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception marking book as finished: ${e.message}")
            }
        }
    }

    private fun setupTocPanel() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val panelWidth = (screenWidth * 0.5).toInt()

        val layoutParams = binding.tocPanel.layoutParams
        layoutParams.width = panelWidth
        binding.tocPanel.layoutParams = layoutParams

        tocAdapter = TocAdapter { tocItem ->
            displayChapter(tocItem.index)
            hideTocPanel()
        }

        binding.rvToc.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tocAdapter
        }

        binding.tocPanel.visibility = View.GONE
        binding.tocOverlay.visibility = View.GONE

        binding.tocOverlay.setOnClickListener {
            hideTocPanel()
        }

        binding.ivCloseToc.setOnClickListener {
            hideTocPanel()
        }

        binding.epubContentContainer.setOnClickListener {
            if (isTocPanelVisible) {
                hideTocPanel()
            } else if (isSettingsPanelVisible) {
                hideSettingsPanel()
            } else {
                toggleControlsVisibility()
            }
        }
    }

    private fun setupSettingsPanel() {
        // Font size controls
        binding.btnFontSizeDecrease.setOnClickListener {
            if (currentFontSize > 12) {
                currentFontSize -= 2
                updateFontSettings()
            }
        }

        binding.btnFontSizeIncrease.setOnClickListener {
            if (currentFontSize < 24) {
                currentFontSize += 2
                updateFontSettings()
            }
        }

        // Font family controls
        binding.rgFontFamily.setOnCheckedChangeListener { _, checkedId ->
            currentFontFamily = when (checkedId) {
                R.id.rb_font_serif -> "serif"
                R.id.rb_font_sans -> "sans-serif"
                else -> "default"
            }
            updateFontSettings()
        }

        // Theme controls
        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            currentTheme = when (checkedId) {
                R.id.rb_theme_dark -> "dark"
                R.id.rb_theme_sepia -> "sepia"
                else -> "light"
            }
            updateThemeSettings()
        }

        binding.settingsOverlay.setOnClickListener {
            hideSettingsPanel()
        }

        binding.ivCloseSettings.setOnClickListener {
            hideSettingsPanel()
        }
    }

    private fun updateFontSettings() {
        binding.tvCurrentFontSize.text = "${currentFontSize}sp"
        injectCustomCSS()
    }

    private fun updateThemeSettings() {
        injectCustomCSS()
    }

    private fun toggleTocPanel() {
        if (isTocPanelVisible) {
            hideTocPanel()
        } else {
            if (isSettingsPanelVisible) {
                hideSettingsPanel()
            }
            showTocPanel()
        }
    }

    private fun toggleSettingsPanel() {
        if (isSettingsPanelVisible) {
            hideSettingsPanel()
        } else {
            if (isTocPanelVisible) {
                hideTocPanel()
            }
            showSettingsPanel()
        }
    }

    private fun showTocPanel() {
        currentBook?.let { book ->
            val enhancedTocList = createEnhancedTocList()

            if (enhancedTocList.isNotEmpty()) {
                tocAdapter.updateTocList(enhancedTocList, currentChapterIndex)

                binding.tocOverlay.clearAnimation()
                binding.tocPanel.clearAnimation()

                binding.tocOverlay.alpha = 0f
                binding.tocOverlay.visibility = View.VISIBLE

                binding.tocPanel.visibility = View.VISIBLE
                binding.tocPanel.translationX = binding.tocPanel.width.toFloat()

                binding.root.post {
                    binding.tocOverlay.animate()
                        .alpha(0.5f)
                        .setDuration(300)
                        .setListener(null)
                        .start()

                    binding.tocPanel.animate()
                        .translationX(0f)
                        .setDuration(300)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                isTocPanelVisible = true
                            }
                        })
                        .start()
                }
            } else {
                Toast.makeText(context, "Không có nội dung để hiển thị", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideTocPanel() {
        binding.tocOverlay.clearAnimation()
        binding.tocPanel.clearAnimation()

        binding.tocOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.tocOverlay.visibility = View.GONE
                }
            })
            .start()

        binding.tocPanel.animate()
            .translationX(binding.tocPanel.width.toFloat())
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.tocPanel.visibility = View.GONE
                    isTocPanelVisible = false
                }
            })
            .start()
    }

    private fun showSettingsPanel() {
        binding.settingsOverlay.clearAnimation()
        binding.settingsPanel.clearAnimation()

        binding.settingsOverlay.alpha = 0f
        binding.settingsOverlay.visibility = View.VISIBLE

        binding.settingsPanel.visibility = View.VISIBLE
        binding.settingsPanel.translationY = binding.settingsPanel.height.toFloat()

        binding.root.post {
            binding.settingsOverlay.animate()
                .alpha(0.5f)
                .setDuration(300)
                .setListener(null)
                .start()

            binding.settingsPanel.animate()
                .translationY(0f)
                .setDuration(300)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        isSettingsPanelVisible = true
                    }
                })
                .start()
        }
    }

    private fun hideSettingsPanel() {
        binding.settingsOverlay.clearAnimation()
        binding.settingsPanel.clearAnimation()

        binding.settingsOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.settingsOverlay.visibility = View.GONE
                }
            })
            .start()

        binding.settingsPanel.animate()
            .translationY(binding.settingsPanel.height.toFloat())
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.settingsPanel.visibility = View.GONE
                    isSettingsPanelVisible = false
                }
            })
            .start()
    }

    private fun loadEpubBook() {
        val epubFileUrl = args.book.bookFileUrl
        Log.d(TAG, "loadEpubBook: bookUrl = $epubFileUrl")

        lifecycleScope.launch {
            try {
                val downloadedFile = EpubCacheManager.downloadEpubFile(epubFileUrl, requireContext())
                if (downloadedFile != null) {
                    extractedEpubDir = extractEpubFile(downloadedFile, requireContext())

                    currentBook = withContext(Dispatchers.IO) {
                        EpubReader().readEpub(FileInputStream(downloadedFile))
                    }

                    Log.d(TAG, "Book loaded: ${currentBook?.title}")

                    // Check if book already total page
                    currentBook?.let { book ->
                        val calculatedPageCount = book.spine?.spineReferences?.size ?: 0
                        Log.d(TAG, "Calculated pageCount: $calculatedPageCount")

                        if (args.book.pageCount == 0 && calculatedPageCount > 0) {
                            Log.d(TAG, "pageCount is 0. Updating book with calculated pageCount: $calculatedPageCount")
                            bookRepository.updateBookPageCount(args.book.bookId, calculatedPageCount).collectLatest { status ->
                                when (status) {
                                    is Resources.Success -> {
                                        Log.d(TAG, "Book pageCount updated successfully in DB.")
                                    }
                                    is Resources.Error -> {
                                        Log.e(TAG, "Failed to update book pageCount in DB: ${status.exception?.message}")
                                    }
                                    else -> { /* Loading */ }
                                }
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        // Skip cover page if it exists
                        val startIndex = findFirstContentChapter()
                        displayChapter(startIndex)
                        (activity as AppCompatActivity).supportActionBar?.title =
                            currentBook?.title ?: "Sách"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Không thể tải xuống sách EPUB", Toast.LENGTH_LONG).show()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading EPUB: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi khi đọc sách: ${e.message}", Toast.LENGTH_LONG).show()
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    private fun findFirstContentChapter(): Int {
        currentBook?.let { book ->
            book.spine.spineReferences.forEachIndexed { index, spineRef ->
                val href = spineRef.resource.href.lowercase()
                // Skip common cover page names
                if (!href.contains("cover") &&
                    !href.contains("title") &&
                    !href.contains("front") &&
                    !href.contains("copyright")) {
                    return index
                }
            }
        }
        return 0
    }

    private fun displayChapter(index: Int) {
        currentBook?.let { book ->
            if (index >= 0 && index < book.spine.spineReferences.size) {
                val spineReference = book.spine.spineReferences[index]
                val chapterResource: nl.siegmann.epublib.domain.Resource = spineReference.resource

                currentChapterHref = chapterResource.href

                try {
                    var chapterHtml = String(chapterResource.data, Charsets.UTF_8)
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
                    // Reset scroll position for new chapter
                    binding.seekBarPageProgress.progress = 0

                } catch (e: Exception) {
                    Log.e(TAG, "Error displaying chapter: ${e.message}", e)
                    Toast.makeText(context, "Lỗi hiển thị chương", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun processHtmlContent(html: String, chapterHref: String): String {
        var processedHtml = html

        val chapterBasePath = if (chapterHref.contains("/")) {
            chapterHref.substring(0, chapterHref.lastIndexOf("/") + 1)
        } else {
            ""
        }

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
        val backgroundColor = when (currentTheme) {
            "dark" -> "#1a1a1a"
            "sepia" -> "#f4f1e8"
            else -> "#ffffff"
        }

        val textColor = when (currentTheme) {
            "dark" -> "#e0e0e0"
            "sepia" -> "#5c4f3a"
            else -> "#333333"
        }

        val fontFamily = when (currentFontFamily) {
            "serif" -> "serif"
            "sans-serif" -> "sans-serif"
            else -> "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif"
        }

        val css = """
            javascript:(function() {
                var style = document.createElement('style');
                style.innerHTML = `
                    body { 
                        font-family: $fontFamily !important;
                        font-size: ${currentFontSize}px !important;
                        line-height: 1.6 !important;
                        margin: 16px !important;
                        background-color: $backgroundColor !important;
                        color: $textColor !important;
                        transition: all 0.3s ease !important;
                    }
                    img { 
                        max-width: 100% !important; 
                        height: auto !important; 
                    }
                    p { 
                        margin-bottom: 1em !important; 
                        text-align: justify !important;
                    }
                    h1, h2, h3, h4, h5, h6 {
                        color: $textColor !important;
                        font-family: $fontFamily !important;
                    }
                    a {
                        color: ${if (currentTheme == "dark") "#66b3ff" else "#0066cc"} !important;
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
                if (fromUser) {
                    isUserScrolling = true
                    // Scroll within current chapter
                    val scrollPosition = (progress * maxScrollPosition) / 100
                    webView.evaluateJavascript("window.scrollTo(0, $scrollPosition);", null)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserScrolling = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isUserScrolling = false
            }
        })
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

    @SuppressLint("SetTextI18n")
    private fun updateProgressControls() {
        currentBook?.let { book ->
            val totalChapters = book.spine.spineReferences.size
            if (totalChapters > 0) {
                binding.tvPageNumber.text = "Chương ${currentChapterIndex + 1} / $totalChapters"
            }
        }
    }

    private fun createEnhancedTocList(): List<EnhancedTocItem> {
        val enhancedTocList = mutableListOf<EnhancedTocItem>()

        currentBook?.let { book ->
            val originalTocMap = mutableMapOf<String, String>()
            book.tableOfContents.tocReferences.forEach { tocRef ->
                originalTocMap[tocRef.resourceId] = tocRef.title
            }

            book.spine.spineReferences.forEachIndexed { index, spineRef ->
                val href = spineRef.resource.href

                // Skip cover pages from TOC
                if (href.lowercase().contains("cover") ||
                    href.lowercase().contains("title") ||
                    href.lowercase().contains("front")) {
                    return@forEachIndexed
                }

                val originalTitle = originalTocMap[href]
                val title = if (originalTitle != null) {
                    originalTitle
                } else {
                    extractTitleFromResource(spineRef.resource) ?: "Chương ${index + 1}"
                }

                enhancedTocList.add(
                    EnhancedTocItem(
                        title = title,
                        href = href,
                        index = index,
                        isFromOriginalToc = originalTitle != null
                    )
                )
            }
        }

        return enhancedTocList
    }

    private fun extractTitleFromResource(resource: nl.siegmann.epublib.domain.Resource): String? {
        return try {
            val htmlContent = String(resource.data, Charsets.UTF_8)

            val h1Regex = "<h1[^>]*>([^<]+)</h1>".toRegex(RegexOption.IGNORE_CASE)
            val h1Match = h1Regex.find(htmlContent)
            if (h1Match != null) {
                return h1Match.groupValues[1].trim()
            }

            val h2Regex = "<h2[^>]*>([^<]+)</h2>".toRegex(RegexOption.IGNORE_CASE)
            val h2Match = h2Regex.find(htmlContent)
            if (h2Match != null) {
                return h2Match.groupValues[1].trim()
            }

            val filename = resource.href.substringAfterLast("/").substringBeforeLast(".")
            filename.replace("_", " ").replace("-", " ")
                .split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting title from resource: ${e.message}")
            null
        }
    }

//    suspend fun downloadEpubFile(url: String, context: Context): File? {
//        return withContext(Dispatchers.IO) {
//            try {
//                val connection = URL(url).openConnection() as HttpURLConnection
//                connection.requestMethod = "GET"
//                connection.setRequestProperty("Accept", "application/epub+zip, application/octet-stream, */*")
//                connection.connect()
//
//                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
//                    val inputStream = connection.inputStream
//                    val tempEpubFile = EpubCacheManager.getTempEpubFile(context)
//                    EpubCacheManager.clearTempEpubFile(context)
//
//                    val outputStream = FileOutputStream(tempEpubFile)
//
//                    val buffer = ByteArray(8192)
//                    var bytesRead: Int
//                    var totalBytes = 0L
//                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
//                        outputStream.write(buffer, 0, bytesRead)
//                        totalBytes += bytesRead
//                    }
//
//                    inputStream.close()
//                    outputStream.close()
//                    connection.disconnect()
//
//                    Log.d(TAG, "Downloaded EPUB to temp file: ${tempEpubFile.absolutePath}, size: ${totalBytes} bytes")
//                    return@withContext tempEpubFile
//                } else {
//                    Log.e(TAG, "Server returned: ${connection.responseCode} ${connection.responseMessage}")
//                    null
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error downloading file: ${e.message}", e)
//                null
//            }
//        }
//    }

    private suspend fun extractEpubFile(epubFile: File, context: Context): File? {
        return withContext(Dispatchers.IO) {
            try {
                val extractDir = EpubCacheManager.getTempExtractDir(context)
                EpubCacheManager.clearTempExtractDir(context)
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
        _binding = null
    }

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