/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.acra

import android.content.Context
import android.net.Uri
import com.flowcrypt.email.api.retrofit.request.model.CrashReportModel
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.gson.GsonBuilder
import org.acra.ReportField
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData
import org.acra.data.StringFormat
import org.acra.sender.HttpSender
import java.net.URL

/**
 * It's a custom realization of [HttpSender]. Here we can filter data which will be sent to a server
 *
 * @author Denys Bondarenko
 */
class CustomReportSender(config: CoreConfiguration) : HttpSender(config, null, null) {
  override fun send(context: Context, errorContent: CrashReportData) {
    for (entry in errorContent.toMap()) {
      if (entry.key == ReportField.LOGCAT.name || entry.key == ReportField.STACK_TRACE.name) {
        val element = (entry.value as? String) ?: continue
        errorContent.put(entry.key, filterFileNames(element))
      }
    }

    super.send(context, errorContent)
  }

  override fun convertToString(report: CrashReportData?, format: StringFormat): String {
    val stackTrace = report?.getString(ReportField.STACK_TRACE) ?: ""
    val firstLinePattern = "(^.*)(: )(.*)\$".toRegex(RegexOption.MULTILINE)
    val secondLinePattern = "( )(.*)(:)(\\d*)(\\))\$".toRegex(RegexOption.MULTILINE)
    val name = findByGroupPosition(
      pattern = firstLinePattern,
      input = stackTrace,
      position = 1,
      defaultValue = "NO NAME"
    )
    val message = findByGroupPosition(
      pattern = firstLinePattern,
      input = stackTrace,
      position = 3,
      defaultValue = "NO MESSAGE"
    )
    val line = secondLinePattern.find(stackTrace)?.groups?.get(4)?.value?.toIntOrNull()
    val col = if (line == null) null else 1
    val url = secondLinePattern.find(stackTrace)?.groups?.get(0)?.value?.trim() ?: "-"

    val crashReportModel = CrashReportModel(
      name = name,
      message = message,
      line = line,
      col = col,
      trace = stackTrace,
      url = url
    )
    return GsonBuilder().create().toJson(crashReportModel)
  }

  override fun sendHttpRequests(
    configuration: CoreConfiguration,
    context: Context,
    method: Method,
    contentType: String,
    login: String?,
    password: String?,
    connectionTimeOut: Int,
    socketTimeOut: Int,
    headers: Map<String, String>?,
    content: String,
    url: URL,
    attachments: List<Uri>
  ) {
    //add Authorization
    val finalHeaders = (headers ?: emptyMap()).toMutableMap().apply {
      val googleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleApiClientHelper.generateGoogleSignInOptions()
      )
      val silentSignIn = googleSignInClient.silentSignIn()
      if (!silentSignIn.isSuccessful || silentSignIn.result.isExpired) {
        throw IllegalStateException("Could not receive idToken")
      }

      val idToken = silentSignIn.result.idToken
      put("Authorization", "Bearer $idToken")
    }

    super.sendHttpRequests(
      configuration,
      context,
      method,
      contentType,
      login,
      password,
      connectionTimeOut,
      socketTimeOut,
      finalHeaders,
      content,
      url,
      attachments
    )
  }

  /**
   * Remove all file names from the given text
   * @param text The given text
   * @return the filtered text
   */
  private fun filterFileNames(text: String?): String {
    val regex = ("(?i)\\b([^\\s:.-]+)[.](" + listOfExtensions.joinToString("|") + ")\\b").toRegex()
    return text?.replace(regex, "example.file") ?: ""
  }

  private fun findByGroupPosition(
    pattern: Regex,
    input: String,
    position: Int,
    defaultValue: String
  ) = try {
    pattern.find(input)?.groups?.get(position)?.value ?: defaultValue
  } catch (e: Exception) {
    defaultValue
  }

  companion object {
    val listOfExtensions = listOf(
      "PGP",
      "ASC",
      "DOC",
      "DOCX",
      "LOG",
      "MSG",
      "ODT",
      "PAGES",
      "RTF",
      "TEX",
      "TXT",
      "WPD",
      "WPS",
      "CSV",
      "DAT",
      "GED",
      "KEY",
      "KEYCHAIN",
      "PPS",
      "PPT",
      "PPTX",
      "SDF",
      "TAR",
      "TAX2016",
      "TAX2018",
      "VCF",
      "XML",
      "AIF",
      "IFF",
      "M3U",
      "M4A",
      "MID",
      "MP3",
      "MPA",
      "WAV",
      "WMA",
      "3G2",
      "3GP",
      "ASF",
      "AVI",
      "FLV",
      "M4V",
      "MOV",
      "MP4",
      "MPG",
      "RM",
      "SRT",
      "SWF",
      "VOB",
      "WMV",
      "3DM",
      "3DS",
      "MAX",
      "OBJ",
      "BMP",
      "DDS",
      "GIF",
      "HEIC",
      "JPG",
      "JPEG",
      "PNG",
      "PSD",
      "PSPIMAGE",
      "TGA",
      "THM",
      "TIF",
      "TIFF",
      "YUV",
      "EPS",
      "SVG",
      "INDD",
      "PCT",
      "PDF",
      "XLR",
      "XLS",
      "XLSX",
      "ACCDB",
      "DB",
      "DBF",
      "MDB",
      "PDB",
      "SQL",
      "APK",
      "BAT",
      "CGI",
      "COM",
      "EXE",
      "GADGET",
      "JAR",
      "WSF",
      "DEM",
      "GAM",
      "NES",
      "ROM",
      "SAV",
      "DWG",
      "DXF",
      "GPX",
      "KML",
      "KMZ",
      "ASP",
      "ASPX",
      "CER",
      "CFM",
      "CSR",
      "CSS",
      "DCR",
      "HTM",
      "HTML",
      "JS",
      "JSP",
      "PHP",
      "RSS",
      "XHTML",
      "CRX",
      "PLUGIN",
      "FNT",
      "FON",
      "OTF",
      "TTF",
      "CAB",
      "CPL",
      "CUR",
      "DESKTHEMEPACK",
      "DLL",
      "DMP",
      "DRV",
      "ICNS",
      "ICO",
      "LNK",
      "SYS",
      "CFG",
      "INI",
      "PRF",
      "HQX",
      "MIM",
      "UUE",
      "7Z",
      "CBR",
      "DEB",
      "PKG",
      "RAR",
      "RPM",
      "SITX",
      "TARGZ",
      "ZIP",
      "ZIPX",
      "BIN",
      "CUE",
      "DMG",
      "ISO",
      "MDF",
      "TOAST",
      "VCD",
      "CLASS",
      "CPP",
      "DTD",
      "FLA",
      "LUA",
      "SLN",
      "SWIFT",
      "VCXPROJ",
      "XCODEPROJ",
      "BAK",
      "TMP",
      "CRDOWNLOAD",
      "ICS",
      "MSI",
      "PART",
      "TORRENT"
    )
  }
}
