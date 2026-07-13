addSbtPlugin("com.github.sbt" % "sbt-release" % "1.4.0")

addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "3.3.0")

// to generate scala classes for tests only
libraryDependencies += "com.twitter" %% "scrooge-generator" % "22.1.0"
