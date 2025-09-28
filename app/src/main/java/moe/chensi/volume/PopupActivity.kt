package moe.chensi.volume

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class PopupActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sendBroadcast(Intent(Service.ACTION_SHOW_VIEW).setPackage(packageName))

        finish()
    }

}
