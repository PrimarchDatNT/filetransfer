package com.file.zip

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import com.azip.archiver.core.AZArchive
import com.file.transfer.core.ConnectUtils
import com.file.transfer.core.SelectFileTransferActivity
import com.file.transfer.core.util.AppKeyConstant
import com.file.transfer.databinding.ActivityZipBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

const val REQ_SELECT_UNZIP_FILE = 300
const val REQ_SELECT_ZIP_FILE = 301

class ZipAcitvity : AppCompatActivity() {

    private fun getAppDstFile() = File(ZipUtil.getUnzipDir())

    private lateinit var binding: ActivityZipBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityZipBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initListener()
    }

    private fun initView() = run {
        binding = ActivityZipBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun initListener() {
        binding.btnUnZipFie.setOnClickListener {
            onSelectUnzipFile()
        }

        binding.btnZipFie.setOnClickListener {
            onSelectZipFile()
        }
    }

    private fun onSelectUnzipFile() {
        val intent = Intent(this, SelectFileTransferActivity::class.java)
        intent.putExtra(ConnectUtils.EXTRA_SELECT_ONLY_FILE, true)
        this.startActivityForResult(intent, REQ_SELECT_UNZIP_FILE)
    }

    private fun onSelectZipFile() {
        val intent = Intent(this, SelectFileTransferActivity::class.java)
        intent.putExtra(ConnectUtils.EXTRA_SELECT_ONLY_FILE, false)
        this.startActivityForResult(intent, REQ_SELECT_ZIP_FILE)
    }

    private fun unzip(src: File, dst: File) = runCatching {
        AZArchive.extractArchive(src.absolutePath, dst.absolutePath)
    }

    private fun zipFile(src: List<String>, dst: File) = CoroutineScope(Main).launch {
        val result = withContext(IO) {
            AZArchive.compressArchive(src, dst.absolutePath, "zip")
        }

        Toast.makeText(this@ZipAcitvity, result.toString(), LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQ_SELECT_UNZIP_FILE -> {
                    data?.getStringExtra(AppKeyConstant.EXTRA_BROWSE_RESULT)?.let {
                        unzip(File(it), getAppDstFile())
                    }
                }

                REQ_SELECT_ZIP_FILE -> {
                    val list = data?.getStringArrayListExtra(ConnectUtils.EXTRA_SHARE_PATH) ?: return
                    val dst = File(ZipUtil.getZipDir(), System.currentTimeMillis().toString())
                    zipFile(list, dst)
                }
            }
        }
    }


}