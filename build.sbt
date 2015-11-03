import com.typesafe.tools.mima.core._
import com.typesafe.tools.mima.plugin.MimaKeys._

val commonSettings = Seq(
  scodecModule := "scodec-cats",
  rootPackage := "scodec.cats",
  contributors ++= Seq(Contributor("mpilquist", "Michael Pilquist"))
)

lazy val root = project.in(file(".")).aggregate(coreJVM, coreJS).settings(commonSettings: _*).settings(
  publishArtifact := false
)

val catsVersion = "0.3.0-SNAPSHOT"

lazy val core = crossProject.in(file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(commonSettings: _*).
  settings(scodecPrimaryModule: _*).
  jvmSettings(scodecPrimaryModuleJvm: _*).
  settings(
    libraryDependencies ++= Seq(
      "org.scodec" %%% "scodec-core" % "1.9.0-SNAPSHOT",
      "org.spire-math" %%% "cats-core" % catsVersion,
      "org.scalatest" %%% "scalatest" % "3.0.0-M7" % "test",
      "org.spire-math" %%% "cats-laws" % catsVersion % "test",
      "org.typelevel" %%% "discipline" % "0.4" % "test"
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
    binaryIssueFilters ++= Seq(
    )
  ).
  jsSettings(commonJsSettings: _*)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

