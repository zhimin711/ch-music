package com.ch.music.activities

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import com.ch.appthemehelper.util.MaterialUtil
import com.ch.music.App
import com.ch.music.R
import com.ch.music.activities.base.AbsThemeActivity
import com.ch.music.billing.BillingManager
import com.ch.music.databinding.ActivityProVersionBinding
import com.ch.music.extensions.accentColor
import com.ch.music.extensions.setLightStatusBar
import com.ch.music.extensions.setStatusBarColor
import com.ch.music.extensions.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PurchaseActivity : AbsThemeActivity() {

    private lateinit var binding: ActivityProVersionBinding
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProVersionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setStatusBarColor(Color.TRANSPARENT)
        setLightStatusBar(false)
        binding.toolbar.navigationIcon?.setTint(Color.WHITE)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        billingManager = App.getContext().billingManager

        MaterialUtil.setTint(binding.purchaseButton, true)

        binding.restoreButton.setOnClickListener {
            restorePurchase()
        }
        binding.purchaseButton.setOnClickListener {
            billingManager.launchBillingFlow(this@PurchaseActivity)
        }
        binding.bannerContainer.backgroundTintList =
            ColorStateList.valueOf(accentColor())
    }

    private fun restorePurchase() {
        billingManager.restorePurchases {
            if (App.isProVersion()) {
                showToast(R.string.restored_previous_purchase_please_restart)
                setResult(RESULT_OK)
            } else {
                showToast(R.string.no_purchase_found)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        billingManager.release()
        super.onDestroy()
    }
}