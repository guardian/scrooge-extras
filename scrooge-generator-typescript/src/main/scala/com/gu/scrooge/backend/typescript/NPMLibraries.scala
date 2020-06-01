package com.gu.scrooge.backend.typescript

object NPMLibraries {
  val dependencies = Map(
    "@types/node-int64" -> "^0.4.29",
    "@types/thrift" -> "^0.10.9",
    "node-int64" -> "^0.4.0",
    "thrift" -> "^0.12.0"
  )

  val devDependencies = Map("typescript" -> "^3.8.3")
}
