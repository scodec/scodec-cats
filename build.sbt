import com.typesafe.tools.mima.core._
import sbtcrossproject.crossProject

addCommandAlias("fmt", "; scalafmtAll; scalafmtSbt")
addCommandAlias("fmtCheck", "; scalafmtCheckAll; scalafmtSbtCheck")

ThisBuild / baseVersion := "1.1"

ThisBuild / organization := "org.scodec"
ThisBuild / organizationName := "Scodec"

ThisBuild / homepage := Some(url("https://github.com/scodec/scodec-cats"))
ThisBuild / startYear := Some(2013)

ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.11", "2.13.3", "3.0.0-M1")

ThisBuild / strictSemVer := false

ThisBuild / versionIntroduced := Map(
  "3.0.0-M1" -> "1.1.99"
)

ThisBuild / githubWorkflowJavaVersions := Seq("adopt@1.8")

ThisBuild / githubWorkflowPublishTargetBranches := Seq(
  RefPredicate.Equals(Ref.Branch("main")),
  RefPredicate.StartsWith(Ref.Tag("v"))
)

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("fmtCheck", "test", "+mimaReportBinaryIssues"))
)

ThisBuild / githubWorkflowEnv ++= Map(
  "SONATYPE_USERNAME" -> s"$${{ secrets.SONATYPE_USERNAME }}",
  "SONATYPE_PASSWORD" -> s"$${{ secrets.SONATYPE_PASSWORD }}",
  "PGP_SECRET" -> s"$${{ secrets.PGP_SECRET }}"
)

ThisBuild / githubWorkflowTargetTags += "v*"

ThisBuild / githubWorkflowPublishPreamble +=
  WorkflowStep.Run(
    List("echo $PGP_SECRET | base64 -d | gpg --import"),
    name = Some("Import signing key")
  )

ThisBuild / githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("release")))

ThisBuild / scmInfo := Some(
  ScmInfo(url("https://github.com/scodec/scodec-cats"), "git@github.com:scodec/scodec-cats.git")
)

ThisBuild / licenses := List(
  ("BSD-3-Clause", url("https://github.com/scodec/scodec-cats/blob/main/LICENSE"))
)

ThisBuild / testFrameworks += new TestFramework("munit.Framework")

ThisBuild / publishGithubUser := "mpilquist"
ThisBuild / publishFullName := "Michael Pilquist"
ThisBuild / developers ++= List(
  "durban" -> "Daniel Urban"
).map { case (username, fullName) =>
  Developer(username, fullName, s"@$username", url(s"https://github.com/$username"))
}

ThisBuild / fatalWarningsInCI := false

ThisBuild / mimaBinaryIssueFilters ++= Seq(
)

lazy val root = project.in(file(".")).aggregate(coreJVM, coreJS).settings(noPublishSettings)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("."))
  .enablePlugins(SbtOsgi)
  .settings(dottyLibrarySettings)
  .settings(dottyJsSettings(ThisBuild / crossScalaVersions))
  .settings(
    libraryDependencies ++= Seq(
      "org.scodec" %%% "scodec-bits" % "1.1.21",
      "org.scodec" %%% "scodec-core" % (if (isDotty.value) "2.0-48e93de" else "1.11.7"),
      "org.typelevel" %%% "cats-core" % "2.3.0-M2",
      "org.typelevel" %%% "cats-laws" % "2.3.0-M2" % Test,
      "org.typelevel" %%% "discipline-munit" % "1.0.1" % Test
    )
  )
  .jvmSettings(osgiSettings)
  .jvmSettings(
    OsgiKeys.exportPackage := Seq("scodec.interop.cats.*;version=${Bundle-Version}"),
    OsgiKeys.importPackage := Seq(
      """scodec.*;version="$<range;[==,=+);$<@>>"""",
      """scala.*;version="$<range;[==,=+);$<@>>"""",
      """cats.*;version="$<range;[==,=+);$<@>>"""",
      "*"
    )
  )

lazy val coreJVM = core.jvm
lazy val coreJS =
  core.js.settings(crossScalaVersions := crossScalaVersions.value.filter(_.startsWith("2.")))
