package com.gu.thrift

import com.twitter.scrooge.ScroogeSBT.autoImport.{scroogeThriftDependencies, scroogeThriftOutputFolder, scroogeDefaultJavaNamespace}
import sbt.Keys.{baseDirectory, compile, description, libraryDependencies, name, resourceGenerators, sLog, scmInfo, target, version}
import sbt.io.IO
import sbt._
import com.gu.scrooge.backend.typescript.NPMLibraries

import scala.sys.process.Process

object ScroogeTypescriptGen extends AutoPlugin {

  object autoImport {

    // the tasks intended for most projects
    lazy val scroogeTypescriptCompile = taskKey[Unit]("Use the tsc command line to compile the generated files. This would spot any issue in the generated files")
    lazy val scroogeTypescriptNPMPublish = taskKey[Unit]("This will publish your package to NPM with npm publish")

    // the tasks intended for the intrepid debuggers
    lazy val scroogeTypescriptGenPackageJson = taskKey[File]("Generate the package.json file")
    lazy val scroogeTypescriptGenNPMPackage = taskKey[Seq[File]]("Generate the npm package")
    lazy val scroogeTypescriptGenTsConf = taskKey[File]("Generates the tsconfig.json")

    // the settings to customise the generation process
    lazy val scroogeTypescriptDevDependencies = settingKey[Map[String, String]]("The node devDependencies to include in the package.json")
    lazy val scroogeTypescriptDependencies = settingKey[Map[String, String]]("The node dependencies to include in the package.json")
    lazy val scroogeTypescriptPackageDirectory = settingKey[File]("The directory where the node package will be generated")
    lazy val scroogeTypescriptPackageLicense = settingKey[String]("The license used to publish the package")
    lazy val scroogeTypescriptPackageMapping = settingKey[Map[String, String]]("The mapping between the thrift jar dependency name to the npm module name")
    lazy val scroogeTypescriptNpmPackageName = settingKey[String]("The name of the package in the package.json")
    lazy val scroogeTypescriptDryRun = settingKey[Boolean]("Whether to try all the step without actually publishing the library on NPM")
  }

  import autoImport._

  private def runCmd(cmd: String, dir: File, logger: Logger, expected: Int = 0, onError: String): Int = {
    logger.info(s"Running ${cmd}")
    val returnCode = Process(cmd, dir).!
    if (returnCode != expected) {
      throw new Exception(s"Return code: $returnCode. $onError")
    }
    returnCode
  }

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    scroogeTypescriptDevDependencies := NPMLibraries.devDependencies,
    scroogeTypescriptDependencies := {
      val dependencies = (Compile / scroogeThriftDependencies).value.flatMap { dependency =>
        for {
          nodeName <- scroogeTypescriptPackageMapping.value.get(dependency)
          version <- libraryDependencies.value.find(_.name == dependency).map(module => s"^${module.revision}")
        } yield nodeName -> version
      }.toMap
      NPMLibraries.dependencies ++ dependencies
    },
    scroogeTypescriptPackageDirectory := (Compile / scroogeThriftOutputFolder).value,
    scroogeTypescriptPackageMapping := Map(),
    scroogeTypescriptNpmPackageName := name.value,
    scroogeTypescriptDryRun := false,

    scroogeTypescriptGenPackageJson := {
      def asHash(map: Map[String, String]): String = map.map {case (k, v) => s""""$k": "$v"""" }.mkString("{\n",",\n","\n}")

      sLog.value.info("Generating package.json")
      val packageJson = scroogeTypescriptPackageDirectory.value / "package.json"
      val content = s"""
        |{
        |  "name": "${scroogeTypescriptNpmPackageName.value}",
        |  "version": "${version.value}",
        |  "description": "${description.value}",
        |  "repository": {
        |    "type": "git",
        |    "url": "${scmInfo.value.map(_.browseUrl.toString).getOrElse("")}"
        |  },
        |  "author": "",
        |  "license": "${scroogeTypescriptPackageLicense.value}",
        |  "devDependencies": ${asHash(scroogeTypescriptDevDependencies.value)},
        |  "dependencies": ${asHash(scroogeTypescriptDependencies.value)}
        |}""".stripMargin
      IO.write(packageJson, content)
      packageJson
    },

    scroogeTypescriptGenTsConf := {
      val content =
        """
          |{
          |  "compilerOptions": {
          |    "target": "es5",
          |    "module": "commonjs",
          |    "strict": true,
          |    "esModuleInterop": true,
          |    "forceConsistentCasingInFileNames": true
          |  }
          |}
          |
          |""".stripMargin
      val tsConfig = scroogeTypescriptPackageDirectory.value / "tsconfig.json"
      IO.write(tsConfig, content)
      tsConfig
    },


    scroogeTypescriptGenNPMPackage := {
      val generatedPackageJson = scroogeTypescriptGenPackageJson.value
      val generatedTsConfig = scroogeTypescriptGenTsConf.value

      val readmeFrom = baseDirectory.value / "README.md"
      val readmeTo = scroogeTypescriptPackageDirectory.value / "README.md"
      val generatedReadme = {
        if (readmeFrom.exists()) {
          IO.copyFile(readmeFrom, readmeTo)
          Seq(readmeTo)
        } else {
          Nil
        }
      }

      val message =
        s"""
          |
          |The NPM package ${scroogeTypescriptNpmPackageName.value} has been generated.
          |
          |To check it compiles correctly
          |cd ${scroogeTypescriptPackageDirectory.value}
          |npm install
          |tsc
          |
          |To publish the package:
          |npm publish --access public
          |""".stripMargin

      sLog.value.info(message)
      generatedReadme :+ generatedPackageJson :+ generatedTsConfig
    },

    Compile / resourceGenerators += scroogeTypescriptGenNPMPackage,

    scroogeTypescriptCompile := {
      scroogeTypescriptGenNPMPackage.value
      val logger: Logger = sLog.value
      val packageDir = scroogeTypescriptPackageDirectory.value

      runCmd("npm install", packageDir, logger = logger, onError = "Unable to install npm dependencies")

      runCmd("tsc", packageDir, logger = logger, onError = "There are compilation errors, check the output above")
    },

    Compile / compile := (Compile / compile).dependsOn(scroogeTypescriptCompile).value,
    Test / compile := (Test / compile).dependsOn(scroogeTypescriptCompile).value,

    scroogeTypescriptNPMPublish := {
      scroogeTypescriptCompile.value

      if (scroogeTypescriptDryRun.value) {
        sLog.value.info("Would have run npm publish --access public but we're in dry-mode")
      } else {
        runCmd(
          cmd = "npm publish --access public",
          dir = scroogeTypescriptPackageDirectory.value,
          logger = sLog.value,
          onError = "Unable to publish package to NPM, check the output above"
        )
      }
    }
  )

}
