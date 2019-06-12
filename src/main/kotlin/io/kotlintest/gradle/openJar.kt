package io.kotlintest.gradle

import java.io.File
import java.util.zip.ZipFile

data class JsModule(val name: String, val deps: List<String>, val code: String)

fun extractJsModules(file: File): List<JsModule> {
  require(file.name.endsWith(".jar"))
  val zipFile = ZipFile(file)
  val regex = "if \\(typeof this\\['(.*?)'] === 'undefined'\\)".toRegex()
  val entries = zipFile.entries()

  val modules = mutableListOf<JsModule>()
  while (entries.hasMoreElements()) {
    val entry = entries.nextElement()
    if (!entry.isDirectory && entry.name.endsWith(".js")) {
      val contents = zipFile.getInputStream(entry).use {
        it.bufferedReader().readText()
      }
      val name = entry.name.removeSuffix(".js")
      val deps = regex.findAll(contents).map { it.groupValues[1] }.toList()
      modules.add(JsModule(name, deps, contents))
    }
  }
  return modules
}