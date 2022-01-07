import sbt.Defaults.sbtPluginExtra
import sbtrelease.ReleaseStateTransformations.{setReleaseVersion, _}
import com.twitter.scrooge.Compiler
import sbt.url
import sbtrelease.{Version, versionFormatError}

name := "scrooge-extras"

ThisBuild / organization := "com.gu"
ThisBuild / scalaVersion := "2.12.11"
ThisBuild / licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

val scroogeVersion = "20.4.1"

val candidateReleaseType = "candidate"
val candidateReleaseSuffix = "-RC1"

lazy val versionSettingsMaybe = {
  // For a non-prod release, start sbt with e.g. sbt -DRELEASE_TYPE=candidate
  // For a production release, just start sbt without that
  sys.props.get("RELEASE_TYPE").map {
    case v if v == candidateReleaseType => candidateReleaseSuffix
  }.map { suffix =>
    releaseVersion := {
      ver => Version(ver).map(_.withoutQualifier.string).map(_.concat(suffix)).getOrElse(versionFormatError(ver))
    }
  }.toSeq
}

lazy val checkReleaseType: ReleaseStep = ReleaseStep({ st: State =>
  val releaseType = sys.props.get("RELEASE_TYPE").map {
    case v if v == candidateReleaseType => candidateReleaseType.toUpperCase
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

  // candidateSteps will take effect if sbt was started with -DRELEASE_TYPE=candidate
  lazy val candidateSteps: Seq[ReleaseStep] = Seq(
    publishArtifacts,
    releaseStepCommandAndRemaining("+publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion
  )

  // detect if a RELEASE_TYPE param was specified when sbt was started, and choose release type based on that
  commonSteps ++ (sys.props.get("RELEASE_TYPE") match {
    case Some(v) if v == candidateReleaseType => candidateSteps // this is a release candidate build to sonatype and Maven
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
  resolvers += Resolver.sonatypeRepo("public"),
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

lazy val root = Project(id = "root", base = file("."))
  .aggregate(sbtScroogeTypescript, typescript)
  .settings(commonSettings, versionSettingsMaybe)
  .settings(
    publishArtifact := false
  )