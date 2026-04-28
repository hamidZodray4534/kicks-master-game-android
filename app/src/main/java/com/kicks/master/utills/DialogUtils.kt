package com.kicks.master.utills

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.kicks.master.R

object DialogUtils {

    private var loadingDialog: AlertDialog? = null

    fun showLoading(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return

        if (loadingDialog != null && loadingDialog?.isShowing == true) {
            try { loadingDialog?.dismiss() } catch (e: Exception) {}
        }

        val builder = AlertDialog.Builder(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading, null)
        builder.setView(view)
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        loadingDialog?.show()
    }

    fun hideLoading() {
        try {
            if (loadingDialog?.isShowing == true) {
                loadingDialog?.dismiss()
            }
        } catch (e: Exception) {
        } finally {
            loadingDialog = null
        }
    }
}
