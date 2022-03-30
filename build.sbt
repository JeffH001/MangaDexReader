name := "MangaDexReader"

version := "0.1"

scalaVersion := "2.12.15"

libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.30.0"
libraryDependencies += "org.apache.spark" %% "spark-core" % "3.1.3"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.1.3"
libraryDependencies += "org.apache.spark" %% "spark-hive" % "3.1.3"
libraryDependencies += "com.lihaoyi" %% "requests" % "0.7.0"
libraryDependencies += "com.lihaoyi" %% "upickle" % "0.9.5"
libraryDependencies += "org.jline" % "jline" % "3.21.0"

scalacOptions += "-deprecation"
