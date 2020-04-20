package oupson.apngcreator.activities

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_creator.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import oupson.apng.encoder.ApngEncoder
import oupson.apngcreator.BuildConfig
import oupson.apngcreator.R
import oupson.apngcreator.adapter.ImageAdapter
import java.io.File
import java.io.FileOutputStream

class CreatorActivity : AppCompatActivity() {
    companion object {
        private const val PICK_IMAGE = 1
        private const val WRITE_REQUEST_CODE = 2
        private const val TAG = "CreatorActivity"
    }
    private var items : ArrayList<Uri> = ArrayList()
    private var adapter : ImageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_creator)

        fabAddImage.setOnClickListener {
            val getIntent = Intent(Intent.ACTION_GET_CONTENT)
            getIntent.type = "image/*"

            startActivityForResult(getIntent,
                PICK_IMAGE
            )
        }

        adapter = ImageAdapter(this, items)

        imageRecyclerView.layoutManager = LinearLayoutManager(this)
        imageRecyclerView.setHasFixedSize(true)
        imageRecyclerView.setItemViewCacheSize(20)
        if (adapter != null)
            ItemTouchHelper(SwipeToDeleteCallback(adapter!!)).attachToRecyclerView(imageRecyclerView)

        setSupportActionBar(creatorBottomAppBar)
        imageRecyclerView.adapter = adapter
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.creator_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menu_create_apng -> {
                if (items.size > 0) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val f = File(filesDir, "images/apng.png").apply {
                            if (!exists()) {
                                parentFile.mkdirs()
                                println(createNewFile())
                            }
                        }
                        val out = FileOutputStream(f)
                        var maxWidth = 0
                        var maxHeight = 0
                        items.forEach {
                            val str = contentResolver.openInputStream(it)
                            val btm = BitmapFactory.decodeStream(str)
                            if (btm.width > maxWidth)
                                maxWidth = btm.width
                            if (btm.height > maxHeight)
                                maxHeight = btm.height
                            str?.close()
                        }

                        if (BuildConfig.DEBUG)
                            Log.i(TAG, "MaxWidth : $maxWidth; MaxHeight : $maxHeight")

                        val encoder = ApngEncoder(out, maxWidth, maxHeight, items.size)
                        items.forEachIndexed { i, uri ->
                            // println("delay : ${adapter?.delay?.get(i)?.toFloat() ?: 1000f}ms")
                            val str = contentResolver.openInputStream(uri) ?: return@forEachIndexed
                            encoder.writeFrame(
                                str,
                                delay = adapter?.delay?.get(i)?.toFloat() ?: 1000f
                            )
                        }

                        encoder.writeEnd()
                        out.close()

                        withContext(Dispatchers.Main) {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = FileProvider.getUriForFile(
                                this@CreatorActivity,
                                "${BuildConfig.APPLICATION_ID}.provider",
                                f
                            )
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivity(intent)
                        }
                    }
                }
                true
            }
            R.id.menu_share_apng -> {
                if (items.size > 0) {
                    GlobalScope.launch(Dispatchers.IO) {
                        val f = File(filesDir, "images/apng.png").apply {
                            if (!exists()) {
                                parentFile.mkdirs()
                                println(createNewFile())
                            }
                        }
                        val out = FileOutputStream(f)
                        var maxWidth = 0
                        var maxHeight = 0
                        items.forEach {
                            val str = contentResolver.openInputStream(it)
                            val btm = BitmapFactory.decodeStream(str)
                            if (btm.width > maxWidth)
                                maxWidth = btm.width
                            if (btm.height > maxHeight)
                                maxHeight = btm.height
                            str?.close()
                        }

                        val encoder = ApngEncoder(out, maxWidth, maxHeight, items.size)
                        items.forEachIndexed { i, uri ->
                            println("delay : ${adapter?.delay?.get(i)?.toFloat() ?: 1000f}ms")
                            val str = contentResolver.openInputStream(uri) ?: return@forEachIndexed
                            encoder.writeFrame(
                                str,
                                delay = adapter?.delay?.get(i)?.toFloat() ?: 1000f
                            )
                        }

                        encoder.writeEnd()
                        out.close()

                        withContext(Dispatchers.Main) {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                        this@CreatorActivity,
                                        "${BuildConfig.APPLICATION_ID}.provider",
                                        f
                                    )
                                )
                                type = "image/png"
                            }
                            startActivity(
                                Intent.createChooser(
                                    intent,
                                    resources.getText(R.string.share)
                                )
                            )
                        }
                    }
                }
                true
            }
            R.id.menu_save_apng -> {
                if (items.size > 0) {
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        // Filter to only show results that can be "opened", such as
                        // a file (as opposed to a list of contacts or timezones).
                        addCategory(Intent.CATEGORY_OPENABLE)

                        // Create a file with the requested MIME type.
                        type = "image/png"
                        putExtra(Intent.EXTRA_TITLE, "${items[0].lastPathSegment}.png")
                    }
                    startActivityForResult(intent, WRITE_REQUEST_CODE)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            PICK_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.data != null) {
                        items.add(data.data!!)
                        adapter?.notifyDataSetChanged()
                    }
                }
            }
            WRITE_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (data?.data != null) {
                        if (BuildConfig.DEBUG)
                            Log.i(TAG, "Intent data : ${data.data}")
                        GlobalScope.launch(Dispatchers.IO) {
                            val out = contentResolver.openOutputStream(data.data!!) ?: return@launch
                            var maxWidth = 0
                            var maxHeight = 0
                            items.forEach {
                                val str = contentResolver.openInputStream(it)
                                val btm = BitmapFactory.decodeStream(str)
                                if (btm.width > maxWidth)
                                    maxWidth = btm.width
                                if (btm.height > maxHeight)
                                    maxHeight = btm.height
                                str?.close()
                            }

                            if (BuildConfig.DEBUG)
                                Log.i(TAG, "MaxWidth : $maxWidth; MaxHeight : $maxHeight")

                            val encoder = ApngEncoder(out, maxWidth, maxHeight, items.size)
                            items.forEachIndexed { i, uri ->
                                // println("delay : ${adapter?.delay?.get(i)?.toFloat() ?: 1000f}ms")
                                val str = contentResolver.openInputStream(uri) ?: return@forEachIndexed
                                encoder.writeFrame(
                                    str,
                                    delay = adapter?.delay?.get(i)?.toFloat() ?: 1000f
                                )
                            }

                            encoder.writeEnd()
                            out.close()

                            withContext(Dispatchers.Main) {
                                Snackbar.make(imageRecyclerView, R.string.done, Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    inner class SwipeToDeleteCallback(private val adapter: ImageAdapter) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ) : Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            adapter.delay.removeAt(position)
            items.removeAt(position)
            adapter.notifyDataSetChanged()
            adapter.listeners.forEachIndexed { index, listener ->
                if (index >= position)
                    listener.position = index
            }
        }

        override fun isItemViewSwipeEnabled() = true
    }
}
