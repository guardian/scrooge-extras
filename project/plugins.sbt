addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.5")

// to generate scala classes for tests only
libraryDependencies += "com.twitter" %% "scrooge-generator" % "22.1.0"