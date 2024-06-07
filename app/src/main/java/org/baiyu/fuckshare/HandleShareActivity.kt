package org.baiyu.fuckshare

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.chooser.ChooserAction
import androidx.core.app.ShareCompat.IntentBuilder
import org.baiyu.fuckshare.utils.AppUtils
import org.baiyu.fuckshare.utils.FileUtils
import org.baiyu.fuckshare.utils.IntentUtils
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger

class HandleShareActivity : Activity() {
    @SuppressLint("WorldReadableFiles")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppUtils.timberPlantTree(this)
        val prefs = AppUtils.getPrefs(this)
        settings = Settings.getInstance(prefs)

        if (settings.toastTimeMS > 0) {
            val applicationName = applicationInfo.loadLabel(packageManager).toString()
            AppUtils.showToast(this, applicationName, settings.toastTimeMS)
        }

        handleIntent()
        finish()
    }

    override fun finish() {
        AppUtils.scheduleClearCacheWorker(this)
        super.finish()
    }

    private fun handleIntent() {
        val uris = IntentUtils.getUrisFromIntent(intent)
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val ib = IntentBuilder(this).apply {
            setType(this@HandleShareActivity.intent.type)
            text?.let { setText(it) }
        }

        val nullCount = AtomicInteger(0)
        uris.parallelStream()
            .map { FileUtils.refreshUri(this, settings, it) }
            .forEachOrdered {
                it?.let { ib.addStream(it) }
                    ?: nullCount.incrementAndGet()
            }

        nullCount.get().let {
            if (it > 0) {
                Timber.e("Failed to process $it of ${uris.size}")
                AppUtils.showToast(
                    this,
                    resources.getString(R.string.fail_to_process).format(it, uris.size),
                    settings.toastTimeMS
                )
            }
        }

        val chooserIntent = ib.setChooserTitle(R.string.app_name).createChooserIntent()
        chooserIntent.putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            listOf(ComponentName(this, this::class.java)).toTypedArray()
        )
        setOf(Intent.EXTRA_INITIAL_INTENTS, Intent.EXTRA_ALTERNATE_INTENTS).forEach {
            IntentUtils.restoreArrayExtras<Intent>(
                intent,
                chooserIntent,
                it
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            IntentUtils.restoreArrayExtras<ChooserAction>(
                intent,
                chooserIntent,
                Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS
            )
            if (settings.enableTextToLinkAction() && AppUtils.hasOverlayPermission(this)) {
                val exists = IntentUtils.getParcelableArrayExtra<ChooserAction>(
                    chooserIntent,
                    Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS
                )?.toMutableList() ?: mutableListOf()
                text?.let {
                    extractUrls(text)
                        .takeIf {
                            it.isNotEmpty()
                        }?.let {
                            IntentUtils.createOpenLinkAction(this, it)
                        }?.let {
                            exists.addAll(it)
                            exists
                        }?.let {
                            chooserIntent.putExtra(
                                Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS,
                                it.toTypedArray()
                            )
                        }
                }
            }
        }
        Timber.d("chooser intent: $chooserIntent")
        startActivity(chooserIntent)
    }

    /**
     * extract urls from text
     */
    private fun extractUrls(text: String): List<String> {
        val urlPattern = """https?://[^\s，,“”"()（）【】\[\]]+(?<!\.)""".toRegex()
        val urls = urlPattern.findAll(text).map { it.value }.toList()
        return urls
    }

    companion object {
        private lateinit var settings: Settings
    }
}