package pl.patrykheciak.tapetkagl

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.content.ComponentName
import android.app.WallpaperManager
import android.content.Intent



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, MyWallpaperService::class.java!!))
        intent.putExtra("SET_LOCKSCREEN_WALLPAPER", true)
        startActivity(intent)
    }
}
