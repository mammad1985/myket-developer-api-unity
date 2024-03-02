package ir.myket.developerapi.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.PublicKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale

object Security {
    private const val MYKET_SIGN_CERTIFICATE =
        "30:81:9F:30:0D:06:09:2A:86:48:86:F7:0D:01:01:01:05:00:03:81:8D:00:30:81:89:02:81:81:00:A7:AA:4A:9A:72:EB:78:C4:FC:87:F8:AA:B9:10:21:00:49:8C:85:88:FC:3E:2A:E3:B0:B3:CF:32:42:B9:6B:24:04:81:14:07:13:C0:40:C9:04:C7:F7:76:9B:B5:0B:9D:8C:A5:0F:AC:E5:78:8A:C7:71:0E:0C:B6:0E:5B:82:7A:9C:16:48:94:9A:89:04:0E:34:9C:30:B7:DD:96:A1:A7:09:1E:10:A8:7B:EB:D3:EC:93:9B:F8:44:B7:86:98:58:B6:F4:62:BE:EF:34:54:52:39:02:CA:1B:55:5C:12:7C:CC:CA:53:4A:E5:FE:FF:46:4B:4A:92:65:F5:78:15:69:02:03:01:00:01"

    fun verifyMyketClient(context: Context): Boolean {
        val packageManager: PackageManager = context.packageManager
        val packageName = "ir.mservices.market"
        val packageInfo: PackageInfo
        val signatures: Array<Signature> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of((PackageManager.GET_SIGNING_CERTIFICATES).toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            }
            packageInfo.signingInfo.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }

        signatures.forEach {
            val input: InputStream = ByteArrayInputStream(it.toByteArray())
            val certificateFactory: CertificateFactory = CertificateFactory.getInstance("X509")
            val certificate: X509Certificate =
                certificateFactory.generateCertificate(input) as X509Certificate
            val publicKey: PublicKey = certificate.publicKey
            val certificateHex = byte2HexFormatted(publicKey.encoded)
            if (MYKET_SIGN_CERTIFICATE != certificateHex) {
                return false
            }
        }

        return true
    }

    private fun byte2HexFormatted(array: ByteArray): String {
        val stringBuilder = StringBuilder(array.size * 2)
        for (index in array.indices) {
            var suggestedHex = Integer.toHexString(array[index].toInt())
            val length = suggestedHex.length
            if (length == 1) {
                suggestedHex = "0$suggestedHex"
            } else if (length > 2) {
                suggestedHex = suggestedHex.substring(length - 2, length)
            }
            @Suppress("DEPRECATION")
            stringBuilder.append(suggestedHex.toUpperCase(Locale.getDefault()))
            if (index < array.size - 1) {
                stringBuilder.append(':')
            }
        }
        return stringBuilder.toString()
    }
}