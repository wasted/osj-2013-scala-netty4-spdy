import sbt._
import Keys._

object BuildSettings {
  val buildOrganization = "com.zero"
  val buildScalaVersion = "2.10.3"
  val buildVersion = "0.1"

  val scalacOptions = Seq("-target:jvm-1.7", "-deprecation", "-encoding", "utf8")

  val buildSettings = Defaults.defaultSettings ++ Seq(organization := buildOrganization,
    scalaVersion := buildScalaVersion,
    version := buildVersion)
}

object Resolvers {
  val sonatypeRepo = "Sonatype Releases"  at "http://oss.sonatype.org/content/repositories/releases"
  val scalaResolvers = Seq(sonatypeRepo)
}

object Dependencies {
  val slf4j = "org.slf4j" % "slf4j-api" % "1.6.4"
  val logback = "ch.qos.logback" % "logback-classic" % "1.0.6"

  val npmVersion = "1.1.0.v20120525"
  val npnApi = "org.eclipse.jetty.npn" % "npn-api" % npmVersion
  val npmBootVersion = "1.1.5.v20130313"
  val npnBoot = "org.mortbay.jetty.npn" % "npn-boot" % npmBootVersion

  val netty = "io.netty" % "netty-all" % "4.0.7.Final"

}

object ZeroBuild extends Build {

  import Resolvers._
  import Dependencies._
  import BuildSettings._

  def paths(attList: Seq[Attributed[File]]) = {
    for {
      file <- attList.map(_.data)
      path = file.getAbsolutePath
      if path.contains("npn-boot")
    } yield "-Xbootclasspath/p:" + path
  }

  val commonDeps = Seq(slf4j, logback, npnApi, npnBoot, netty)

  lazy val idefix = Project("Zero", file("."),
    settings = buildSettings ++ Seq (logLevel in run := Level.Info,
      fork := true,
      javaOptions <++= (managedClasspath in Runtime) map { attList => paths(attList) })
      ++ Seq(resolvers := scalaResolvers,
             libraryDependencies := commonDeps,
             libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _)
            )
  )
}


