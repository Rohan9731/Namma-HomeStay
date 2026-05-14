package com.namma.homestay.ui

import android.net.Uri
import android.util.Base64
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import okio.Buffer

/**
 * Custom Coil Fetcher that handles data: URIs (base64-encoded images).
 * Used because Firebase Storage is unavailable on the free tier,
 * so images are stored in Firestore/listing documents as base64 data URIs.
 *
 * IMPORTANT: must be Fetcher.Factory<Uri> (not String) because Coil 2.x runs
 * StringMapper before checking factories, converting every String model into a
 * android.net.Uri before factory lookup. Registering as Factory<String> means
 * the factory is never invoked.
 */
class DataUriFetcher(
    private val dataUri: String,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val base64Part = dataUri.substringAfter("base64,", missingDelimiterValue = "")
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        val buffer = Buffer().write(bytes)
        return SourceResult(
            source = ImageSource(source = buffer, context = options.context),
            mimeType = "image/jpeg",
            dataSource = DataSource.MEMORY
        )
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Only handle data: URIs — let all other URI types fall through to built-in fetchers
            if (data.scheme != "data") return null
            return DataUriFetcher(data.toString(), options)
        }
    }
}
