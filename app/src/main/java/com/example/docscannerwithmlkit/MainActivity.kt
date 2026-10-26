package com.example.docscannerwithmlkit

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.docscannerwithmlkit.ui.theme.DocScannerWithMLKitTheme
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(5)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)


        enableEdgeToEdge()
        setContent {
            DocScannerWithMLKitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
                    var scanMessage by remember { mutableStateOf<String?>(null) }
                    var isStartingScan by remember { mutableStateOf(false) }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                    ) { activityResult ->
                        isStartingScan = false
                        when (activityResult.resultCode) {
                            Activity.RESULT_OK -> {
                                val result = runCatching {
                                    GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
                                }.getOrNull()

                                if (result == null) {
                                    scanMessage = "The scan finished, but its results couldn't be read. Please try again."
                                } else {
                                    val scannedPages = result.pages.orEmpty().map { it.imageUri }
                                    val pdfSaveError = result.pdf?.let { pdf ->
                                        runCatching {
                                            val input = contentResolver.openInputStream(pdf.uri)
                                                ?: error("Unable to open the scanned PDF")
                                            input.use { source ->
                                                File(filesDir, "scan.pdf").outputStream().use { destination ->
                                                    source.copyTo(destination)
                                                }
                                            }
                                        }.isFailure
                                    } ?: false

                                    if (scannedPages.isEmpty() && result.pdf == null) {
                                        scanMessage = "No pages were returned. Please try scanning again."
                                    } else {
                                        imageUris = scannedPages
                                        scanMessage = when {
                                            pdfSaveError -> "Scan complete, but the PDF couldn't be saved. Please try again."
                                            scannedPages.isEmpty() -> "Scan complete. The PDF was saved on this device."
                                            else -> "Scan complete: ${scannedPages.size} page(s) saved as a PDF."
                                        }
                                    }
                                }
                            }
                            Activity.RESULT_CANCELED -> {
                                scanMessage = "Scan cancelled. Your previous scan is unchanged."
                            }
                            else -> {
                                scanMessage = "The scanner couldn't complete the scan. Please try again."
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        imageUris.forEach { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            enabled = !isStartingScan,
                            onClick = {
                                isStartingScan = true
                                scanMessage = "Opening document scanner…"
                                scanner.getStartScanIntent(this@MainActivity)
                                    .addOnSuccessListener { intentSender ->
                                        runCatching {
                                            scannerLauncher.launch(
                                                IntentSenderRequest.Builder(intentSender).build()
                                            )
                                        }.onFailure {
                                            isStartingScan = false
                                            scanMessage = "The scanner couldn't be opened. Please try again."
                                        }
                                    }
                                    .addOnFailureListener { error ->
                                        isStartingScan = false
                                        scanMessage = error.localizedMessage?.let {
                                            "The scanner couldn't be opened: $it"
                                        } ?: "The scanner couldn't be opened. Please try again."
                                    }
                            }
                        ) {
                            Text(text = if (isStartingScan) "Opening scanner…" else "Scan PDF")
                        }
                        scanMessage?.let { message ->
                            Text(
                                text = message,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}