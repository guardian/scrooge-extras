package com.gu.scrooge.backend.typescript

object NPMLibraries {
  val dependencies = Map(
    "@types/node-int64" -> "^0.4.29",
    "@types/thrift" -> "^0.10.17",
    "node-int64" -> "^0.4.0",
    "thrift" -> "^0.20.0"
  )

  val devDependencies = Map("typescript" -> "^4.5.4")
}
