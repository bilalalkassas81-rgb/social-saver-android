package com.socialsaver

import android.app.Application
import com.socialsaver.data.HistoryStore
import java.io.File

class App : Application() {
    val history: HistoryStore by lazy { HistoryStore(File(filesDir, "history.json")) }
}
