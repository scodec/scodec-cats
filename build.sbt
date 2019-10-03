import com.typesafe.tools.mima.core._
import sbtcrossproject.crossProject

val commonSettings = Seq(
  scodecModule := "scodec-cats",
  rootPackage := "scodec.cats",
  scmInfo := Some(ScmInfo(url("https://github.com/scodec/scodec-cats"), "git@github.com:scodec/scodec-cats.git")),
  contributors ++= Seq(
    Contributor("mpilquist", "Michael Pilquist"),
    Contributor("durban", "Daniel Urban")
  ),
  scalacOptions --= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v >= 13 =>
        Seq("-Yno-adapted-args", "-Ywarn-unused-import")
      case _ =>
        Seq()
    }
  }
)

lazy val root = project.in(file(".")).aggregate(coreJVM, coreJS).settings(commonSettings: _*).settings(
  publishArtifact := false
)

val catsVersion = "2.0.0"

lazy val core = crossProject(JSPlatform, JVMPlatform).in(file(".")).
  enablePlugins(BuildInfoPlugin).
  enablePlugins(ScodecPrimaryModuleSettings).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= Seq(
      "org.scodec" %%% "scodec-core" % "1.11.4",
      "org.typelevel" %%% "cats-core" % catsVersion,
      "org.typelevel" %%% "cats-laws" % catsVersion % "test",
      "org.typelevel" %%% "discipline-scalatest" % "1.0.0-RC1" % "test"
    )
  ).
  jvmSettings(
    docSourcePath := new File(baseDirectory.value, ".."),
    OsgiKeys.exportPackage := Seq("scodec.interop.cats.*;version=${Bundle-Version}"),
    OsgiKeys.importPackage := Seq(
      """scodec.*;version="$<range;[==,=+);$<@>>"""",
      """scala.*;version="$<range;[==,=+);$<@>>"""",
      """cats.*;version="$<range;[==,=+);$<@>>"""",
      "*"
    ),
    mimaBinaryIssueFilters ++= Seq(
    )
  ).
  jsSettings(commonJsSettings: _*)

lazy val coreJVM = core.jvm.enablePlugins(ScodecPrimaryModuleJVMSettings)
lazy val coreJS = core.js

