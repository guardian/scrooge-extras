addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.10")

// to generate scala classes for tests only
libraryDependencies += "com.twitter" %% "scrooge-generator" % "22.1.0"
