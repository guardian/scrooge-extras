import sbtrelease.ReleaseStateTransformations._

name := "sbt-scrooge-typescript"
organization := "com.gu"

scalaVersion := "2.12.11"

sbtPlugin := true

// this plugin depends on scrooge
addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % "20.4.0")

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))

publishMavenStyle := false

bintrayOrganization := Some("guardian")
bintrayRepository := "sbt-plugins"
releasePublishArtifactsAction := PgpKeys.publishSigned.value

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