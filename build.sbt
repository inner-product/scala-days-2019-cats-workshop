name := "Scala Days Cats Workshop"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.6.0",
  "org.typelevel" %% "cats-effect" % "1.3.1",
  "org.creativescala" %% "doodle" % "0.9.3"
)

Compile / fork := true
Test / fork := true

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")
