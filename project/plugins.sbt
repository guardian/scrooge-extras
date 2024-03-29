addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.10.0")

addSbtPlugin("ch.epfl.scala" % "sbt-version-policy" % "3.2.0")

// to generate scala classes for tests only
libraryDependencies += "com.twitter" %% "scrooge-generator" % "22.7.0"


