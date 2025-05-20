scalaVersion := "2.13.14"

scalacOptions ++= Seq(
  "-feature",
  "-language:reflectiveCalls",
)

Compile / unmanagedSourceDirectories += baseDirectory.value / "hello-morse/src"
Compile / unmanagedSourceDirectories += baseDirectory.value / "leros/src"

val chiselVersion = "6.7.0"
addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % chiselVersion cross CrossVersion.full)
libraryDependencies += "org.chipsalliance" %% "chisel" % chiselVersion
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "6.0.0"
libraryDependencies += "com.fazecast" % "jSerialComm" % "2.11.0"