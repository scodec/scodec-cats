import com.typesafe.tools.mima.core._
import sbtcrossproject.crossProject

ThisBuild / tlBaseVersion := "1.2"

ThisBuild / organization := "org.scodec"
ThisBuild / organizationName := "Scodec"
ThisBuild / tlSonatypeUseLegacyHost := true

ThisBuild / homepage := Some(url("https://github.com/scodec/scodec-cats"))
ThisBuild / startYear := Some(2013)

ThisBuild / crossScalaVersions := Seq("2.12.18", "2.13.12", "3.3.1")
ThisBuild / tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "1.1.0").toMap

ThisBuild / licenses := List(
  ("BSD-3-Clause", url("https://github.com/scodec/scodec-cats/blob/main/LICENSE"))
)

ThisBuild / developers := List(
  tlGitHubDev("mpilquist", "Michael Pilquist"),
  tlGitHubDev("durban", "Daniel Urban")
)

ThisBuild / tlFatalWarnings := false

lazy val root = tlCrossRootProject.aggregate(core)

lazy val core = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("."))
  .settings(name := "scodec-cats")
  .settings(
    libraryDependencies ++= Seq(
      "org.scodec" %%% "scodec-bits" % "1.1.38",
      "org.scodec" %%% "scodec-core" % (if (tlIsScala3.value) "2.2.2" else "1.11.10"),
      "org.typelevel" %%% "cats-core" % "2.10.0",
      "org.typelevel" %%% "cats-laws" % "2.10.0" % Test,
      "org.typelevel" %%% "discipline-munit" % "2.0.0-M3" % Test
    )
  )
  .nativeSettings(
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "1.2.0").toMap
  )
