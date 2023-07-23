package com.aferi.imagecomparisonworkmanager

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.aferi.imagecomparisonworkmanager.ui.theme.ImageComparisonWorkManagerTheme

class MainActivity : ComponentActivity() {

    private lateinit var workManager: WorkManager
    private val viewModel by viewModels<ImageComparisonViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        setContent {
            ImageComparisonWorkManagerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val workResult = viewModel.workId?.let { id ->
                        workManager.getWorkInfoByIdLiveData(id).observeAsState().value
                    }
                    LaunchedEffect(key1 = workResult?.outputData) {
                        if (workResult?.outputData != null) {
                            val filePath = workResult.outputData.getString(KEY_RESULT_PATH)
                            filePath?.let {
                                val bitmap = BitmapFactory.decodeFile(it)
                                viewModel.updateCompressedBitmap(bitmap)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        viewModel.unCompressedUri?.let {
                            Text(text = "UnCompressed Image:")
                            AsyncImage(model = it, contentDescription = null)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        viewModel.compressedBitmap?.let {
                            Text(text = "Compressed Image:")
                            AsyncImage(model = it, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    //This fun will be invoked when the launchMode is defined as 'SingleTop'
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: return
        viewModel.updateUnCompressedUri(uri)

        val request = OneTimeWorkRequestBuilder<ImageComparisonWorker>()
            .setInputData(
                workDataOf(
                    KEY_CONTENT_URI to uri.toString(),
                    KEY_COMPARISION_Threshold to 1024 * 20L
                )
            ).build()
        viewModel.updateWorkId(request.id)
        workManager.enqueue(request)
    }
}