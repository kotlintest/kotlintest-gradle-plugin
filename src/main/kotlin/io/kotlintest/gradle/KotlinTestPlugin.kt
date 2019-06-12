package io.kotlintest.gradle

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

open class KotlinTestPlugin : Plugin<Project> {
  override fun apply(project: Project) {

    val kotlintestJvm = project.tasks.create("kotlintest-jvm")
    kotlintestJvm.actions.add(KotlinTestAction() as Action<in Task>)

    val kotlintestJs = project.tasks.create("kotlintest-js")
    val action: Action<in Task> = KotlinTestJsAction() as Action<in Task>
    kotlintestJs.actions.add(action)
  }
}