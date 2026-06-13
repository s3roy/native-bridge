package com.bridge

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebResourceRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Drop-in replacement for [WebView]. Use this in XML or code and the bridge is
 * fully wired automatically — no other native code required.
 */
class BridgeWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : WebView(context, attrs) {

    private var delegate: WebViewClient? = null
    private var backPressedCallback: OnBackPressedCallback? = null
    private var interceptBackPress = true
    private var applySafeAreaPadding = true
    private var lastInsets = SafeAreaInsets(0, 0, 0, 0)

    init {
        NativeBridge.attach(this)
        super.setWebViewClient(InternalClient())
        webChromeClient = InternalChromeClient()
        settings.mediaPlaybackRequiresUserGesture = false
        setupInsetsListener()
        BridgeWebViewCache.applyOnAttach(this)
    }

    override fun loadUrl(url: String) {
        BridgeWebViewCache.loadUrl(this, url)
    }

    override fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) {
        BridgeWebViewCache.loadUrl(this, url, additionalHttpHeaders)
    }

    /** Load document bypassing HTML cache (static chunks still cached in smart mode). */
    fun loadUrlFresh(url: String) {
        BridgeWebViewCache.loadUrl(this, url, BridgeWebViewCache.documentHeaders())
    }

    fun setWebViewCachePolicy(params: org.json.JSONObject) {
        BridgeWebViewCache.setPolicy(this, params)
    }

    /** Set an HTTP cookie visible to the web page (e.g. session token). */
    fun setCookie(name: String, value: String, url: String? = null) {
        val p = org.json.JSONObject().put("name", name).put("value", value)
        if (url != null) p.put("url", url)
        BridgeCookies.setCookie(this, p)
    }

    /** Remove every cookie from this WebView. */
    fun clearAllCookies() {
        BridgeCookies.clearCookies(this, org.json.JSONObject())
    }

    /** Push a custom event with any JSON-serializable payload to all WebViews. */
    fun publishEvent(event: String, payload: Any? = null) {
        NativeBridge.publishEvent(event, payload)
    }

    /** Handle `NativeBridge.send` from this WebView only. */
    fun setOnWebEvent(listener: ((event: String, payload: Any?) -> Unit)?) {
        NativeBridge.setWebViewEventListener(this, listener)
    }

    /** Handle [WebEvents.WEBVIEW_LOADED] from this WebView only. */
    fun setOnWebViewLoaded(listener: ((payload: WebViewLoadedPayload) -> Unit)?) {
        NativeBridge.setWebViewLoadedListener(this, listener)
    }

    private fun setupInsetsListener() {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            lastInsets = SafeAreaInsets(
                maxOf(bars.top, cutout.top),
                maxOf(bars.bottom, cutout.bottom),
                maxOf(bars.left, cutout.left),
                maxOf(bars.right, cutout.right),
            )
            AppStateMonitor.updateInsets(
                lastInsets.top,
                lastInsets.bottom,
                lastInsets.left,
                lastInsets.right,
            )
            applySafeAreaPadding()
            val imeVisible = ime.bottom > 0
            AppStateMonitor.updateKeyboard(imeVisible, ime.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(this)
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        AppStateMonitor.setWebViewVisible(visibility == VISIBLE)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView === this) {
            AppStateMonitor.setWebViewVisible(visibility == VISIBLE)
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        AppStateMonitor.setWebViewFocused(hasWindowFocus && hasFocus())
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        AppStateMonitor.setWebViewFocused(gainFocus)
    }

    fun setDelegateClient(client: WebViewClient) {
        delegate = client
    }

    /** When true (default), hardware back is routed to the web page first. */
    fun setInterceptBackPress(enabled: Boolean) {
        interceptBackPress = enabled
    }

    /**
     * When true (default), WebView content is padded below the status bar / notch
     * and above the navigation bar so pages don't draw under system UI.
     */
    fun setApplySafeAreaPadding(enabled: Boolean) {
        applySafeAreaPadding = enabled
        applySafeAreaPadding()
    }

    fun isApplySafeAreaPadding(): Boolean = applySafeAreaPadding

    private fun applySafeAreaPadding() {
        if (applySafeAreaPadding) {
            setPadding(lastInsets.left, lastInsets.top, lastInsets.right, lastInsets.bottom)
        } else {
            setPadding(0, 0, 0, 0)
        }
    }

    /**
     * Call from Activity if not using auto-registration.
     * Returns true if back was consumed by web or WebView history.
     */
    fun handleBackPress(onComplete: ((Boolean) -> Unit)? = null) {
        BridgeBackPress.handle(this) { consumed ->
            onComplete?.invoke(consumed)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerBackCallback()
    }

    override fun onDetachedFromWindow() {
        backPressedCallback?.remove()
        backPressedCallback = null
        super.onDetachedFromWindow()
    }

    private fun registerBackCallback() {
        if (!interceptBackPress) return
        val activity = findActivity() as? ComponentActivity ?: return
        backPressedCallback?.remove()
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                BridgeBackPress.handle(this@BridgeWebView) { consumed ->
                    if (!consumed) {
                        isEnabled = false
                        activity.onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        }
        activity.onBackPressedDispatcher.addCallback(activity, backPressedCallback!!)
    }

    private fun findActivity(): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private inner class InternalChromeClient : WebChromeClient() {
        override fun onPermissionRequest(request: PermissionRequest?) {
            if (request == null) return
            val ctx = context.applicationContext
            val needsCamera = request.resources.any {
                it == PermissionRequest.RESOURCE_VIDEO_CAPTURE
            }
            val needsMic = request.resources.any {
                it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
            }
            val cameraOk = !needsCamera || BridgePermissions.isGranted(ctx, BridgePermissionNames.CAMERA)
            val micOk = !needsMic || BridgePermissions.isGranted(ctx, BridgePermissionNames.MICROPHONE)
            if (cameraOk && micOk) {
                request.grant(request.resources)
            } else {
                request.deny()
            }
        }
    }

    private inner class InternalClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val url = request.url?.toString() ?: return false
                if (BridgeWebViewCache.shouldInterceptMainDocument(
                        view,
                        url,
                        request.isForMainFrame,
                        request.method,
                    )
                ) {
                    if (!BridgeWebViewCache.hasBypassHeader(request.requestHeaders)) {
                        BridgeWebViewCache.loadUrl(view, url, request.requestHeaders)
                        return true
                    }
                }
            }
            return delegate?.shouldOverrideUrlLoading(view, request) ?: false
        }

        @Suppress("DEPRECATION", "OverridingDeprecatedMember")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (BridgeWebViewCache.shouldInterceptMainDocument(view, url, true, "GET")) {
                BridgeWebViewCache.loadUrl(view, url)
                return true
            }
            return delegate?.shouldOverrideUrlLoading(view, url) ?: false
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            NativeBridge.inject(view)
            delegate?.onPageStarted(view, url, favicon)
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            AppStateMonitor.setWebViewUrl(url)
            NativeBridge.inject(view)
            delegate?.onPageFinished(view, url)
            super.onPageFinished(view, url)
        }
    }
}
