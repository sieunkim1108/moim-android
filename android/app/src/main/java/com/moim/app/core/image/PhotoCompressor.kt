package com.moim.app.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/** 갤러리에서 고른 사진을 서버에 올리기 좋은 작은 JPEG로 바꾼다 */
interface PhotoCompressor {
    /** [uri]: 사진 선택기가 돌려준 content:// 주소. 실패하면 예외 */
    suspend fun toProfileJpeg(uri: String): ByteArray
}

/**
 * 프로필 사진용 압축.
 * - 가운데를 정사각형으로 잘라 [SIZE]×[SIZE]로 줄인다 (동그라미 아바타에 딱 맞게)
 * - 폰 카메라 사진의 회전 정보(EXIF)를 반영해 눕지 않게 한다
 * - JPEG 품질 85 → 보통 50~150KB (Storage 2MB 제한보다 훨씬 작다)
 */
@Singleton
class AndroidPhotoCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoCompressor {

    override suspend fun toProfileJpeg(uri: String): ByteArray = withContext(Dispatchers.IO) {
        val source = Uri.parse(uri)
        val resolver = context.contentResolver

        // 1) 크기만 먼저 읽어서, 너무 큰 사진은 줄여서 읽는다 (메모리 아끼기)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "사진을 읽지 못했어요" }

        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2
        val decoded = resolver.openInputStream(source).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("사진을 읽지 못했어요")

        // 2) 회전 정보 반영
        val rotation = resolver.openInputStream(source).use { input ->
            if (input == null) 0 else when (
                ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }

        // 3) 가운데 정사각형 자르기 + 회전 + SIZE로 줄이기를 한 번에
        val side = min(decoded.width, decoded.height)
        val scale = SIZE.toFloat() / max(side, 1)
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val square = Bitmap.createBitmap(
            decoded,
            (decoded.width - side) / 2,
            (decoded.height - side) / 2,
            side,
            side,
            matrix,
            true,
        )

        ByteArrayOutputStream().use { out ->
            square.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            if (square !== decoded) square.recycle()
            decoded.recycle()
            out.toByteArray()
        }
    }

    private companion object {
        const val SIZE = 512
        const val QUALITY = 85
    }
}
