package com.gu.scrooge.backend.typescript

object NPMLibraries {
  val dependencies = Map(
    "@types/node-int64" -> "^0.4.29",
    "@types/thrift" -> "^0.10.12",
    "node-int64" -> "^0.4.0",
    "thrift" -> "^0.15.0"
  )

  val devDependencies = Map("typescript" -> "4.9.5")
  val peerDependencies = Map("typescript" -> "~4.9.5")
}
