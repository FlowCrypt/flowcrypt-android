/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.acra

import android.content.Context
import org.acra.ReportField
import org.acra.collector.CrashReportData
import org.acra.config.ACRAConfiguration
import org.acra.model.StringElement
import org.acra.sender.HttpSender

/**
 * It's a custom realization of [HttpSender]. Here we can filter data which will be sent to a server
 *
 * @author Denis Bondarenko
 *         Date: 7/25/19
 *         Time: 3:15 PM
 *         E-mail: DenBond7@gmail.com
 */
class CustomReportSender(config: ACRAConfiguration,
                         method: Method,
                         type: Type,
                         formUri: String? = null,
                         mapping: MutableMap<ReportField, String>? = null)
  : HttpSender(config, method, type, formUri, mapping) {
  override fun send(context: Context, report: CrashReportData) {

    val iterator = report.iterator()
    while (iterator.hasNext()) {
      val item = iterator.next()
      when (item.key) {
        ReportField.LOGCAT, ReportField.STACK_TRACE -> {
          val element = item.value as? StringElement
          report.put(item.key, StringElement(filterFileNames(element.toString())))
        }
        else -> {
        }
      }
    }

    super.send(context, report)
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

  companion object {
    val listOfExtensions = listOf(
        "PGP", "ASC",
        "DOC", "DOCX", "LOG", "MSG", "ODT", "PAGES", "RTF", "TEX", "TXT",
        "WPD", "WPS", "CSV", "DAT", "GED", "KEY", "KEYCHAIN", "PPS", "PPT", "PPTX", "SDF", "TAR", "TAX2016", "TAX2018",
        "VCF", "XML", "AIF", "IFF", "M3U", "M4A", "MID", "MP3", "MPA", "WAV", "WMA", "3G2", "3GP", "ASF", "AVI", "FLV",
        "M4V", "MOV", "MP4", "MPG", "RM", "SRT", "SWF", "VOB", "WMV", "3DM", "3DS", "MAX", "OBJ", "BMP", "DDS", "GIF",
        "HEIC", "JPG", "JPEG", "PNG", "PSD", "PSPIMAGE", "TGA", "THM", "TIF", "TIFF", "YUV", "EPS", "SVG",
        "INDD", "PCT", "PDF", "XLR", "XLS", "XLSX", "ACCDB", "DB", "DBF", "MDB", "PDB", "SQL", "APK", "BAT",
        "CGI", "COM", "EXE", "GADGET", "JAR", "WSF", "DEM", "GAM", "NES", "ROM", "SAV", "DWG", "DXF", "GPX", "KML",
        "KMZ", "ASP", "ASPX", "CER", "CFM", "CSR", "CSS", "DCR", "HTM", "HTML", "JS", "JSP", "PHP", "RSS", "XHTML",
        "CRX", "PLUGIN", "FNT", "FON", "OTF", "TTF", "CAB", "CPL", "CUR", "DESKTHEMEPACK", "DLL", "DMP", "DRV", "ICNS",
        "ICO", "LNK", "SYS", "CFG", "INI", "PRF", "HQX", "MIM", "UUE", "7Z", "CBR", "DEB", "PKG", "RAR", "RPM", "SITX",
        "TARGZ", "ZIP", "ZIPX", "BIN", "CUE", "DMG", "ISO", "MDF", "TOAST", "VCD", "CLASS", "CPP", "DTD", "FLA", "LUA",
        "SLN", "SWIFT", "VCXPROJ", "XCODEPROJ", "BAK", "TMP", "CRDOWNLOAD", "ICS", "MSI", "PART", "TORRENT")
  }
}