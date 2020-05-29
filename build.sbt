import sbt.Defaults.sbtPluginExtra
import sbtrelease.ReleaseStateTransformations._
import com.twitter.scrooge.Compiler

name := "sbt-scrooge-extras"

ThisBuild / organization := "com.gu"
ThisBuild / scalaVersion := "2.12.11"
ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

val scroogeVersion = "20.4.1"

lazy val sbtScroogeTypescript = project.in(file("sbt-scrooge-typescript"))
  .settings(
    sbtPlugin := true,
    bintrayOrganization := Some("guardian"),
    bintrayRepository := "sbt-plugins",
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,

    // this plugin depends on the scrooge plugin
    libraryDependencies += sbtPluginExtra(
      "com.twitter" % "scrooge-sbt-plugin" % scroogeVersion,
      (pluginCrossBuild / sbtBinaryVersion).value,
      (update / scalaBinaryVersion).value
    ),

    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      publishArtifacts,
      releaseStepTask(bintrayRelease),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )

lazy val typescript = project.in(file("scrooge-generator-typescript"))
  .settings(
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
    }
  )
