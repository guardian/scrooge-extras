import sbt.Defaults.sbtPluginExtra
import sbtrelease.ReleaseStateTransformations._
import com.twitter.scrooge.Compiler

name := "scrooge-extras"

ThisBuild / organization := "com.gu"
ThisBuild / scalaVersion := "2.12.11"
ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

// don't publish the root project
publish / skip := true

val scroogeVersion = "20.4.1"

lazy val mavenSettings = Seq(
  pomExtra := (
    <url>https://github.com/guardian/scrooge-extras</url>
      <scm>
        <connection>scm:git:git@github.com:guardian/scrooge-extras.git</connection>
        <developerConnection>scm:git:git@github.com:guardian/scrooge-extras.git</developerConnection>
        <url>git@github.com:guardian/scrooge-extras.git</url>
      </scm>
      <developers>
        <developer>
          <id>alexduf</id>
          <name>Alex Dufournet</name>
          <url>https://github.com/alexduf</url>
        </developer>
        <developer>
          <id>JamieB-gu</id>
          <name>Jamie B</name>
          <url>https://github.com/JamieB-gu</url>
        </developer>
        <developer>
          <id>JustinPinner</id>
          <name>Justin Pinner</name>
          <url>https://github.com/JustinPinner</url>
        </developer>
      </developers>
    ),
  publishTo := sonatypePublishToBundle.value,
  publishConfiguration := publishConfiguration.value.withOverwrite(false),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false }
)

lazy val standardReleaseSteps: Seq[ReleaseStep] = Seq(
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  releaseStepCommandAndRemaining("+publishSigned"),
  releaseStepCommand("sonatypeBundleRelease"),
  setNextVersion,
  commitNextVersion //,
  // pushChanges    // <-- only to main branch
)

lazy val sbtScroogeTypescript = project.in(file("sbt-scrooge-typescript"))
  .dependsOn(typescript)
  .settings(
    name := "sbt-scrooge-typescript",
    sbtPlugin := true,
    organization := "com.gu",
    resolvers += Resolver.sonatypeRepo("public"),
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,

    // this plugin depends on the scrooge plugin
    libraryDependencies += sbtPluginExtra(
      "com.twitter" % "scrooge-sbt-plugin" % scroogeVersion,
      (pluginCrossBuild / sbtBinaryVersion).value,
      (update / scalaBinaryVersion).value
    ),
    releaseProcess := standardReleaseSteps
  )

lazy val typescript = project.in(file("scrooge-generator-typescript"))
  .settings(
    name := "scrooge-generator-typescript",
    organization := "com.gu",
    resolvers += Resolver.sonatypeRepo("public"),
    libraryDependencies ++= Seq(
      "com.twitter" %% "scrooge-generator" % scroogeVersion,
      "com.twitter" %% "scrooge-core" % scroogeVersion % "test",
      "com.github.spullara.mustache.java" % "compiler" % "0.8.18",
      "org.scalatest" %% "scalatest" % "3.1.1" % "test"
    ),
    Test / sourceGenerators += { () =>
      val compiler = new Compiler()
      compiler.destFolder = ((Compile / sourceManaged).value / "generated").getAbsolutePath
      compiler.thriftFiles ++= ((Test / resourceDirectory).value / "school" ** "*.thrift").get().map(_.getAbsolutePath)
      compiler.language = "scala"
      compiler.run()
      ((Compile / sourceManaged).value / "generated" ** "*.scala").get()
    },
    releaseProcess := standardReleaseSteps
  )

