package io.kotlintest.gradle

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.testing.Test
import org.gradle.process.internal.DefaultExecActionFactory
import org.gradle.process.internal.DefaultJavaForkOptions
import org.gradle.process.internal.JavaExecAction
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class KotlinTestAction : Action<Test> {
  override fun execute(t: Test) {

    t.project.components

    fun args(): List<String> {
      return listOf("--writer", "io.kotlintest.runner.console.DefaultConsoleWriter")
    }

    fun exec(): JavaExecAction {
      val fileResolver = (t.project as ProjectInternal).services.get(FileResolver::class.java)
      val exec = DefaultExecActionFactory(fileResolver).newJavaExecAction()
      t.copyTo(exec)
      exec.main = "io.kotlintest.runner.console.LauncherKt"
      exec.classpath = t.classpath
      exec.jvmArgs = t.allJvmArgs
      exec.args = args()
      exec.isIgnoreExitValue = true
      return exec
    }

    val result = exec().execute()
    if (result.exitValue != 0) {
      throw GradleException("There were test failures")
    }
  }
}

class KotlinTestJsAction : Action<DefaultTask> {

  override fun execute(t: DefaultTask) {

//    val plugins = (t.project as ProjectInternal).convention.plugins
//    plugins.forEach { println(it.key + "->" + it.value) }

//    t.project.configurations.forEach { println(it.name) }
//    t.project.plugins.forEach { println(it) }

    // val kw = t.project.plugins.getPlugin(KotlinMultiplatformPluginWrapper::class.java)

    fun exec(): JavaExecAction {
      val fileResolver = (t.project as ProjectInternal).services.get(FileResolver::class.java)
      val exec = DefaultExecActionFactory(fileResolver).newJavaExecAction()
      val forkOptions = DefaultJavaForkOptions(fileResolver)
      exec.main = "io.kotlintest.runner.console.LauncherKt"
      //  exec.classpath = forkOptions.classpath
      exec.jvmArgs = forkOptions.allJvmArgs
      exec.isIgnoreExitValue = true
      return exec
    }

    val kotlin = t.project.extensions.findByName("kotlin") as KotlinMultiplatformExtension
    println("kotlin=$kotlin")

//    kotlin.sourceSets.filter { it.name.contains("Test") }.forEach {
//      println("name=" + it.name)
//      val files = it.kotlin.files
//      println("files=$files")
//    }

    // can take this from the source sets defined by kotlin
    val jsTest = t.project.configurations.first { it.name == "jsTestRuntimeClasspath" }

//    println("jsTestRuntimeClasspath.all=${jsTest.all}")
//    println("jsTestRuntimeClasspath.dependencies=${jsTest.dependencies}")
//    println("jsTestRuntimeClasspath.files=${jsTest.files}")
//    println("jsTestRuntimeClasspath.outgoing=${jsTest.outgoing}")
//    println("jsTestRuntimeClasspath.outgoing.artifacts.files.files=${jsTest.outgoing.artifacts.files.files}")
//    println("jsTestRuntimeClasspath.outputs.files.files=${t.outputs.files.files}")
//    println("jsTestRuntimeClasspath.t.project.buildDir=${t.project.buildDir}")
//    println("jsTestRuntimeClasspath.t.project.name=${t.project.name}")
//    println("jsTestRuntimeClasspath.t.project.buildFile=${t.project.buildFile}")
//    println("jsTestRuntimeClasspath.t.project.version=${t.project.version}")
//    println("jsTestRuntimeClasspath.t.project.components.asMap=${t.project.components.asMap}")
//    println("jsTestRuntimeClasspath.state=${jsTest.state}")

    val files = jsTest.resolvedConfiguration.files
    val modules = files.flatMap {
      println("Loading js files from jar $it")
      extractJsModules(it)
    }

    if (!modules.any { it.name == "kotlintest-runner" }) throw RuntimeException("KotlinTest Javascript Runner must be on classpath")

    println("All modules ${modules.joinToString(", ") { it.name }}")

    val manager = ScriptEngineManager()
    val engine = manager.getEngineByName("JavaScript")
    engine.put("console", ProvidedConsole())
    engine.put("kotlintestlistener", GradleTestRunnerListener())
    engine.eval("console.log('hello')")

    fun findModule(name: String) = modules.first { it.name == name }

    val loadedModules = mutableListOf<String>()
    fun loadModule(module: JsModule) {
      module.deps.forEach { loadModule(findModule(it)) }
      if (!loadedModules.contains(module.name)) {
        println("Loading module ${module.name}")
        engine.eval(module.code)
        loadedModules.add(module.name)
      }
    }

    loadModule(findModule("kotlin"))
    modules.forEach { loadModule(it) }

    fun ScriptEngine.eval(file: File) {
      println("Eval $file")
      file.inputStream().use {
        it.bufferedReader().readText()
      }.let { this.eval(it) }
    }

    // add in the js files from the module we're testing
    val main = t.project.buildDir.resolve("classes/kotlin/js/main/").resolve(t.project.name + ".js")
    if (main.exists()) engine.eval(main)
    val test = t.project.buildDir.resolve("classes/kotlin/js/test/").resolve(t.project.name + ".js")
    if (test.exists()) engine.eval(test)

    // we need to find all the spec classes located in the compiled test code
    val spectypes = listOf("FunSpec", "WordSpec", "StringSpec", "ShouldSpec", "FreeSpec")
    val regexes = spectypes.map { "([a-zA-Z0-9_]+).prototype = Object.create\\($it.prototype\\);".toRegex() }
    val testcode = main.inputStream().use { it.bufferedReader().readText() }
    val specs = regexes.flatMap { regex -> regex.findAll(testcode).map { it.groupValues[1] }.toList() }
    println("Specs=$specs")

    val constructors = specs.joinToString(", ", "[", "]") { "new this['${t.project.name}'].$it()" }
    println(constructors)

    val bootstrap = """
      var constructor = this['kotlintest-runner'].io.kotlintest.runner.js.TestRunner;
      new constructor($constructors);
      """.trimIndent()
    println("bootstrap=$bootstrap")

    engine.eval(bootstrap)

    val javaPlugin = (t.project as ProjectInternal).convention.getPlugin(JavaPluginConvention::class.java)
    println("javaPlugin=$javaPlugin")
    val sourceSets = javaPlugin.sourceSets

//    println("sourceSets=" + sourceSets.joinToString { it.name })
//    sourceSets.filter { it.name.contains("test") }.forEach {
//      println("name=" + it.name)
//      println("compileClasspath=" + it.compileClasspath)
//      println("runtimeClasspath=" + it.runtimeClasspath)
//      println("runtimeClasspath.files=" + it.runtimeClasspath.files)
//
//      val fileResolver = (t.project as ProjectInternal).services.get(FileResolver::class.java)
//      val exec = DefaultExecActionFactory(fileResolver).newJavaExecAction()
//      val forkOptions = DefaultJavaForkOptions(fileResolver)
//      forkOptions.copyTo(exec)
//      exec.classpath = it.runtimeClasspath
//      exec.main = "io.kotlintest.runner.console.LauncherKt"
//      exec.jvmArgs = forkOptions.jvmArgs
//      exec.args = args()
//      exec.isIgnoreExitValue = true
//      val result = exec.execute()
//      if (result.exitValue != 0) {
//        throw GradleException("There were test failures")
//      }
//    }
  }
}