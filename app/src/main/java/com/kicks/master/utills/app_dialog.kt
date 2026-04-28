package com.kicks.master.utills

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.kicks.master.R
import androidx.core.graphics.drawable.toDrawable
import com.kicks.master.Constant
import com.kicks.master.SplashActivity


object AppDialog {

    var currentDialog: Dialog? = null
    private lateinit var dialog_lv: Dialog
    private var megaOfferDialog: Dialog? = null




    fun dailyLimitDialog(activity: Activity) {
        val dialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.limit_dialog)
            setCancelable(true)
            window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT
                )
                setGravity(Gravity.CENTER)
                attributes = attributes?.apply {
                    dimAmount = 0.6f
                    flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
                }
                setWindowAnimations(R.style.BottomDialogAnimation)
            }
        }
        val rootView = dialog.findViewById<View>(R.id.dialog_root)
        val closeButton = dialog.findViewById<View>(R.id.linearLayout13)
        val okButton = dialog.findViewById<View>(R.id.linearLayout12)
        rootView?.let { view ->
            view.alpha = 0f
            view.scaleX = 0.85f
            view.scaleY = 0.85f
            view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(350)
                .setInterpolator(DecelerateInterpolator()).start()
        }

        val dismissWithAnimation = {
            rootView?.animate()?.alpha(0f)?.scaleX(0.85f)?.scaleY(0.85f)?.setDuration(250)
                ?.setInterpolator(DecelerateInterpolator())?.withEndAction { dialog.dismiss() }
                ?.start() ?: dialog.dismiss()
        }

        closeButton?.setOnClickListener { dismissWithAnimation() }
        okButton?.setOnClickListener {
            dismissWithAnimation()
        }

        try {
            if (!activity.isFinishing && !dialog.isShowing) {
                dialog.show()
            } else {
                Log.d("Dialog", "Dialog is already showing or activity finishing")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    fun timeMegaLimitDialog(activity: Activity, dueTime: String) {
        val dialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.time_limit_dialog)
            setCancelable(true)
            window?.apply {
                setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT
                )
                setGravity(Gravity.CENTER)
                attributes = attributes?.apply {
                    dimAmount = 0.6f
                    flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
                }
                setWindowAnimations(R.style.BottomDialogAnimation)
            }
        }
        val rootView = dialog.findViewById<View>(R.id.dialog_root)
        val closeButton = dialog.findViewById<View>(R.id.linearLayout13)
        val okButton = dialog.findViewById<View>(R.id.linearLayout12)
        val tvWaitingLimit = dialog.findViewById<TextView>(R.id.tv_waiting_time)
        rootView?.let { view ->
            view.alpha = 0f
            view.scaleX = 0.85f
            view.scaleY = 0.85f
            view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(350)
                .setInterpolator(DecelerateInterpolator()).start()
        }

        val dismissWithAnimation = {
            rootView?.animate()?.alpha(0f)?.scaleX(0.85f)?.scaleY(0.85f)?.setDuration(250)
                ?.setInterpolator(DecelerateInterpolator())?.withEndAction { dialog.dismiss() }
                ?.start() ?: dialog.dismiss()
        }


        tvWaitingLimit.text = "${dueTime}"

        closeButton?.setOnClickListener { dismissWithAnimation() }
        okButton?.setOnClickListener {
            dismissWithAnimation()
        }

        try {
            if (!activity.isFinishing && !dialog.isShowing) {
                dialog.show()
            } else {
                Log.d("Dialog", "Dialog is already showing or activity finishing")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }






    fun refresh_adx(
        activity: Activity, mandatory: Boolean
    ) {

        val dialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.rf_adx_dialog)
            setCancelable(!mandatory)
            setCanceledOnTouchOutside(!mandatory)
        }

        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
            attributes = attributes?.apply {
                dimAmount = 0.6f
                flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            }
            setWindowAnimations(R.style.BottomDialogAnimation)
        }

        val rootView = dialog.findViewById<ConstraintLayout>(R.id.dialog_root)
        val yesBtn = dialog.findViewById<LinearLayout>(R.id.yesBtn)
      //  val closeBtn = dialog.findViewById<LinearLayout?>(R.id.closeBtn)

        // Entry animation
        rootView?.apply {
            alpha = 0f
            translationY = 50f
            animate().alpha(1f).translationY(0f).setDuration(350)
                .setInterpolator(DecelerateInterpolator()).start()
        }

        val dismissWithAnimation = {
            rootView?.animate()?.alpha(0f)?.translationY(50f)?.setDuration(250)
                ?.withEndAction { dialog.dismiss() }?.start() ?: dialog.dismiss()
        }



        yesBtn.setOnClickListener {
            Constant.setBoolean(activity, Constant.LIMIT_REACHED, false)
            Constant.setBoolean(activity, Constant.SWITCH_ADX, false)
            Constant.setString(activity, Constant.ADX_IMP_COUNT, "")


            val intent = Intent(activity, SplashActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
            }
            activity.startActivity(intent)
        }

        try {
            if (!activity.isFinishing && !dialog.isShowing) {
                dialog.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    fun notEnoughGemsDialog(activity: Activity) {
        val dialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.rf_adx_dialog)
            setCancelable(true)
        }

        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
            attributes = attributes?.apply {
                dimAmount = 0.6f
                flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            }
            setWindowAnimations(R.style.BottomDialogAnimation)
        }

        val rootView = dialog.findViewById<ConstraintLayout>(R.id.dialog_root)
        val tvTitle = dialog.findViewById<TextView>(R.id.textViw1)
        val descLayout = dialog.findViewById<LinearLayout>(R.id.textView2)
        val tvDesc = descLayout?.getChildAt(0) as? TextView
        val yesBtn = dialog.findViewById<LinearLayout>(R.id.yesBtn)
        val btnText = yesBtn?.getChildAt(0) as? TextView

        tvTitle?.text = "Not Enough Gems!"
        tvDesc?.text = "You do not have enough gems to unlock the Mega Offer Box yet. Play more and collect gems to unlock it."
        btnText?.text = "Okay"

        // Entry animation (re-use from refresh_adx)
        rootView?.apply {
            alpha = 0f
            translationY = 50f
            animate().alpha(1f).translationY(0f).setDuration(350)
                .setInterpolator(DecelerateInterpolator()).start()
        }

        val dismissWithAnimation = {
            rootView?.animate()?.alpha(0f)?.translationY(50f)?.setDuration(250)
                ?.withEndAction { dialog.dismiss() }?.start() ?: dialog.dismiss()
        }

        yesBtn?.setOnClickListener {
            dismissWithAnimation()
        }

        try {
            if (!activity.isFinishing && !dialog.isShowing) {
                dialog.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun alreadyUnlockedDialog(activity: Activity, onClaimClick: () -> Unit) {
        val dialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.rf_adx_dialog)
            setCancelable(true)
        }

        dialog.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.BOTTOM)
            attributes = attributes?.apply {
                dimAmount = 0.6f
                flags = flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            }
            setWindowAnimations(R.style.BottomDialogAnimation)
        }

        val rootView = dialog.findViewById<ConstraintLayout>(R.id.dialog_root)
        val tvTitle = dialog.findViewById<TextView>(R.id.textViw1)
        val descLayout = dialog.findViewById<LinearLayout>(R.id.textView2)
        val tvDesc = descLayout?.getChildAt(0) as? TextView
        val yesBtn = dialog.findViewById<LinearLayout>(R.id.yesBtn)
        val btnText = yesBtn?.getChildAt(0) as? TextView

        tvTitle?.text = "Offer Unlocked!"
        tvDesc?.text = "You have already unlocked this Mega Offer. Click below to proceed to claiming your reward!"
        btnText?.text = "Claim Reward"

        // Entry animation
        rootView?.apply {
            alpha = 0f
            translationY = 50f
            animate().alpha(1f).translationY(0f).setDuration(350)
                .setInterpolator(DecelerateInterpolator()).start()
        }

        val dismissWithAnimation = {
            rootView?.animate()?.alpha(0f)?.translationY(50f)?.setDuration(250)
                ?.withEndAction { dialog.dismiss() }?.start() ?: dialog.dismiss()
        }

        yesBtn?.setOnClickListener {
            dismissWithAnimation()
            onClaimClick()
        }

        try {
            if (!activity.isFinishing && !dialog.isShowing) {
                dialog.show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}