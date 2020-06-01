addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.6")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")

// to generate scala classes for tests only
libraryDependencies += "com.twitter" %% "scrooge-generator" % "20.4.1"