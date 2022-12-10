import sbt.Defaults.sbtPluginExtra
import sbtrelease.ReleaseStateTransformations.{setReleaseVersion, _}
import com.twitter.scrooge.{ScroogeConfig, Compiler}
import sbt.url
import sbtrelease.{Version, versionFormatError}

name := "scrooge-extras"

ThisBuild / organization := "com.gu"
ThisBuild / scalaVersion := "2.12.17"
ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

val scroogeVersion = "22.7.0"   // remember to also update plugins.sbt if this version changes

val betaReleaseType = "beta"
val betaReleaseSuffix = "-beta.0"

lazy val versionSettingsMaybe = {
  // For a beta release, start sbt with sbt -DRELEASE_TYPE=beta
  // For a production release, just start sbt without that
  sys.props.get("RELEASE_TYPE").map {
    case v if v == betaReleaseType => betaReleaseSuffix
  }.map { suffix =>
    releaseVersion := {
      ver => Version(ver).map(_.withoutQualifier.string).map(_.concat(suffix)).getOrElse(versionFormatError(ver))
    }
  }.toSeq
}

lazy val checkReleaseType: ReleaseStep = ReleaseStep({ st: State =>
  val releaseType = sys.props.get("RELEASE_TYPE").map {
    case v if v == betaReleaseType => betaReleaseType.toUpperCase
  }.getOrElse("PRODUCTION")

  SimpleReader.readLine(s"This will be a $releaseType release. Continue? (y/n) [N]: ") match {
    case Some(v) if Seq("Y", "YES").contains(v.toUpperCase) => // we don't care about the value - it's a flow control mechanism
    case _ => sys.error(s"Release aborted by user!")
  }
  // we haven't changed state, just pass it on if we haven't thrown an error from above
  st
})

lazy val releaseProcessSteps: Seq[ReleaseStep] = {
  val commonSteps = Seq(
    checkReleaseType,
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion
  )

  // prodSteps will take effect if sbt was started without any -DRELEASE_TYPE param
  lazy val prodSteps: Seq[ReleaseStep] = Seq(
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )

  // betaSteps will take effect if sbt was started with -DRELEASE_TYPE=beta
  lazy val betaSteps: Seq[ReleaseStep] = Seq(
    publishArtifacts,
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion
  )

  // detect if a RELEASE_TYPE param was specified when sbt was started, and choose release type based on that
  commonSteps ++ (sys.props.get("RELEASE_TYPE") match {
    case Some(v) if v == betaReleaseType => betaSteps // this is a beta build to sonatype and Maven
    case _ => prodSteps  // it's a production release
  })

}

lazy val commonSettings = Seq(
  organization := "com.gu",
  publishTo := sonatypePublishToBundle.value,
  scmInfo := Some(ScmInfo(
    url("https://github.com/guardian/scrooge-extras"),
    "scm:git:git@github.com:guardian/scrooge-extras.git"
  )),
  homepage := Some(url("https://github.com/guardian/scrooge-extras")),
  developers := List(Developer(
    id = "Guardian",
    name = "Guardian",
    email = null,
    url = url("https://github.com/guardian")
  )),
  resolvers ++= Resolver.sonatypeOssRepos("public"),
  releaseProcess := releaseProcessSteps
)

lazy val sbtScroogeTypescript = project.in(file("sbt-scrooge-typescript"))
  .dependsOn(typescript)
  .settings(commonSettings)
  .settings(
    name := "sbt-scrooge-typescript",
    sbtPlugin := true,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,

    // this plugin depends on the scrooge plugin
    libraryDependencies += sbtPluginExtra(
      "com.twitter" % "scrooge-sbt-plugin" % scroogeVersion,
      (pluginCrossBuild / sbtBinaryVersion).value,
      (update / scalaBinaryVersion).value
    )
  )

lazy val typescript = project.in(file("scrooge-generator-typescript"))
  .settings(commonSettings)
  .settings(
    name := "scrooge-generator-typescript",
    libraryDependencies ++= Seq(
      "com.twitter" %% "scrooge-generator" % scroogeVersion,
      "com.twitter" %% "scrooge-core" % scroogeVersion % "test",
      "com.github.spullara.mustache.java" % "compiler" % "0.9.10",
      "org.scalatest" %% "scalatest" % "3.2.14" % "test"
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
  .settings(commonSettings, versionSettingsMaybe)
  .settings(
    publishArtifact := false
  )