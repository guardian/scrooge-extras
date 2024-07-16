package com.gu.scrooge.backend.typescript

object NPMLibraries {
  val dependencies = Map(
    "@types/node-int64" -> "^0.4.29",
    "@types/thrift" -> "^0.10.11",
    "node-int64" -> "^0.4.0",
    "thrift" -> "^0.15.0"
  )

  val devDependencies = Map("typescript" -> "^4.5.4")

  val overrides = Map("ws" -> "^5.2.4") // Fixes a high security vulnerability in 5.2.3 (https://github.com/websockets/ws/commit/4abd8f6de4b0b65ef80b3ff081989479ed93377e)
}
