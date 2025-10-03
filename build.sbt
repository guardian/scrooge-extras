import sbt.Defaults.sbtPluginExtra
import sbtrelease.ReleaseStateTransformations.{setReleaseVersion, _}
import com.twitter.scrooge.{ScroogeConfig, Compiler}
import sbt.url
import sbtversionpolicy.withsbtrelease.ReleaseVersion

name := "scrooge-extras"

ThisBuild / organization := "com.gu"
ThisBuild / scalaVersion := "2.12.18"
ThisBuild / licenses := Seq(License.Apache2)

ThisBuild / scalacOptions := Seq("-release:11")

val scroogeVersion = "22.1.0" // remember to also update plugins.sbt if this version changes

lazy val artifactProductionSettings = Seq(
  organization := "com.gu",
  Test / testOptions +=
    Tests.Argument(TestFrameworks.ScalaTest, "-u", s"test-results/scala-${scalaVersion.value}", "-o"),
  resolvers ++= Resolver.sonatypeOssRepos("public")
)

lazy val sbtScroogeTypescript = project.in(file("sbt-scrooge-typescript"))
  .dependsOn(typescript)
  .settings(artifactProductionSettings)
  .settings(
    name := "sbt-scrooge-typescript",
    sbtPlugin := true,

    // this plugin depends on the scrooge plugin
    libraryDependencies += sbtPluginExtra(
      "com.twitter" % "scrooge-sbt-plugin" % scroogeVersion,
      (pluginCrossBuild / sbtBinaryVersion).value,
      (update / scalaBinaryVersion).value
    )
  )

lazy val typescript = project.in(file("scrooge-generator-typescript"))
  .settings(artifactProductionSettings)
  .settings(
    name := "scrooge-generator-typescript",
    libraryDependencies ++= Seq(
      "com.twitter" %% "scrooge-generator" % scroogeVersion,
      "com.twitter" %% "scrooge-core" % scroogeVersion % "test",
      "com.github.spullara.mustache.java" % "compiler" % "0.9.14",
      "org.scalatest" %% "scalatest" % "3.2.14" % "test",
      //Update vulnerable dependencies
      "org.codehaus.plexus" % "plexus-utils" % "3.5.0",
      "org.apache.thrift" % "libthrift" % "0.17.0"
    ),
    Test / sourceGenerators += { () =>
      val scroogeConfig = ScroogeConfig(
        destFolder = ((Compile / sourceManaged).value / "generated").getAbsolutePath,
        thriftFiles = ((Test / resourceDirectory).value / "school" ** "*.thrift").get().map(_.getAbsolutePath).toList,
        language = "scala"
      )
      val compiler = new Compiler(scroogeConfig)
      compiler.run()
      ((Compile / sourceManaged).value / "generated" ** "*.scala").get()
    }
  )

lazy val root = Project(id = "root", base = file("."))
  .aggregate(sbtScroogeTypescript, typescript)
  .settings(
    publish / skip := true,
    releaseVersion := ReleaseVersion.fromAggregatedAssessedCompatibilityWithLatestRelease().value,
    releaseProcess := Seq(
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion
    )
  )