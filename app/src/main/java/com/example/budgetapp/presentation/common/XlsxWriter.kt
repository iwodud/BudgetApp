package com.example.budgetapp.presentation.common

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object XlsxWriter {

    data class Sheet(val name: String, val rows: List<List<Any?>>)

    fun createAndShare(context: Context, filename: String, sheets: List<Sheet>): Uri {
        val file = File(context.cacheDir, filename)

        val allStrings = mutableListOf<String>()
        val stringIndex = mutableMapOf<String, Int>()

        fun idx(s: String): Int = stringIndex.getOrPut(s) {
            allStrings.add(s)
            allStrings.size - 1
        }

        for (sheet in sheets) {
            for (row in sheet.rows) {
                for (cell in row) {
                    if (cell is String) idx(cell)
                }
            }
        }

        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            zip.putEntry("[Content_Types].xml") {
                val overrides = sheets.indices.joinToString("\n") { i ->
                    """  <Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
                }
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
$overrides
  <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""
            }

            zip.putEntry("_rels/.rels") {
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
            }

            zip.putEntry("xl/workbook.xml") {
                val sheetEls = sheets.mapIndexed { i, s ->
                    """  <sheet name="${s.name.xmlEscape()}" sheetId="${i + 1}" r:id="rId${i + 1}"/>"""
                }.joinToString("\n")
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
$sheetEls
  </sheets>
</workbook>"""
            }

            zip.putEntry("xl/_rels/workbook.xml.rels") {
                val rels = sheets.indices.joinToString("\n") { i ->
                    """  <Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${i + 1}.xml"/>"""
                }
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$rels
  <Relationship Id="rId${sheets.size + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
  <Relationship Id="rId${sheets.size + 2}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
            }

            zip.putEntry("xl/styles.xml") {
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
  <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>"""
            }

            zip.putEntry("xl/sharedStrings.xml") {
                val items = allStrings.joinToString("\n") { s ->
                    "  <si><t>${s.xmlEscape()}</t></si>"
                }
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${allStrings.size}" uniqueCount="${allStrings.size}">
$items
</sst>"""
            }

            sheets.forEachIndexed { sheetIdx, sheet ->
                zip.putEntry("xl/worksheets/sheet${sheetIdx + 1}.xml") {
                    val rows = sheet.rows.mapIndexed { rIdx, row ->
                        val cells = row.mapIndexed { cIdx, cell ->
                            val ref = cellRef(rIdx, cIdx)
                            when (cell) {
                                null -> ""
                                is Number -> """<c r="$ref"><v>${cell.toDouble()}</v></c>"""
                                else -> {
                                    val si = idx(cell.toString())
                                    """<c r="$ref" t="s"><v>$si</v></c>"""
                                }
                            }
                        }.joinToString("")
                        """  <row r="${rIdx + 1}">$cells</row>"""
                    }.joinToString("\n")

                    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
$rows
  </sheetData>
</worksheet>"""
                }
            }
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    private fun cellRef(row: Int, col: Int): String {
        var c = col
        val sb = StringBuilder()
        while (c >= 0) {
            sb.insert(0, ('A' + c % 26))
            c = c / 26 - 1
        }
        return "${sb}${row + 1}"
    }

    private fun String.xmlEscape() = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun ZipOutputStream.putEntry(name: String, content: () -> String) {
        putNextEntry(ZipEntry(name))
        write(content().toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
