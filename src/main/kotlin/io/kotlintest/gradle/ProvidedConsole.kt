package io.kotlintest.gradle;

class ProvidedConsole {

  fun dir(obj: Any?) {
    System.out.println("[DIR] $obj");
  }

  fun error(obj: Any?) {
    System.out.println("[ERROR] $obj");
  }

  fun info(obj: Any?) {
    System.out.println("[INFO] $obj");
  }

  fun log(obj: Any?) {
    System.out.println("[LOG] $obj");
  }

  fun warn(obj: Any?) {
    System.out.println("[WARN] $obj");
  }
}
