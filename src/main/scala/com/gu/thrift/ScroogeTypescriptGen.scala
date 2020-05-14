package com.gu.thrift

import com.twitter.scrooge.ScroogeSBT.autoImport.{scroogeThriftSourceFolder, scroogeUnpackDeps}
import sbt.Keys.{baseDirectory, description, name, sLog, scmInfo, target, version, compile, resourceGenerators}
import sbt.io.IO
import sbt._

import scala.sys.process.Process

object ScroogeTypescriptGen extends AutoPlugin {

  object autoImport {

    // the tasks intended for most projects
    lazy val scroogeTypescriptCompile = taskKey[Unit]("Use the tsc command line to compile the generated files. This would spot any issue in the generated files")
    lazy val scroogeTypescriptNPMPublish = taskKey[Unit]("Use the tsc command line to compile the generated files. This would spot any issue in the generated files")

    // the tasks intended for the intrepid debuggers
    lazy val scroogeTypescriptGenPackageJson = taskKey[File]("Generate the package.json file")
    lazy val scroogeTypescriptGenTypescript = taskKey[Seq[File]]("Run the thrift command line")
    lazy val scroogeTypescriptGenEpisodeFiles = taskKey[Seq[File]]("Generates the list of episode files from the dependencies resolved by scrooge")
    lazy val scroogeTypescriptGenNPMPackage = taskKey[Seq[File]]("Generate the npm package")

    // the settings to customise the generation process
    lazy val scroogeTypescriptDevDependencies = settingKey[Map[String, String]]("The node devDependencies to include in the package.json")
    lazy val scroogeTypescriptDependencies = settingKey[Map[String, String]]("The node dependencies to include in the package.json")
    lazy val scroogeTypescriptPackageDirectory = settingKey[File]("The directory where the node package will be generated")
    lazy val scroogeTypescriptPackageLicense = settingKey[String]("The license used to publish the package")
    lazy val scroogeTypescriptThriftGenOptions = settingKey[Seq[String]]("The list of generation options passed to the thrift cmd line")
    lazy val scroogeTypescriptPackageMapping = settingKey[Map[String, String]]("The mapping between the thrift jar dependency name to the npm module name")
    lazy val scroogeTypescriptNpmPackageName = settingKey[String]("The name of the package in the package.json")
    lazy val scroogeTypescriptDryRun = settingKey[Boolean]("Whether to try all the step without actually publishing the library on NPM")
  }

  import autoImport._

  private def runCmd(cmd: String, dir: File, logger: Logger, expected: Int = 0, onError: String): Int = {
    logger.info(s"Running ${cmd}")
    val npmReturnCode = Process(cmd, dir).!
    if (npmReturnCode != expected) {
      throw new Exception("Unable to install npm dependencies")
    }
    npmReturnCode
  }

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    scroogeTypescriptDevDependencies := Map("typescript" -> "^3.8.3"),
    scroogeTypescriptDependencies := Map(
      "@types/node-int64" -> "^0.4.29",
      "@types/thrift" -> "^0.10.9",
      "node-int64" -> "^0.4.0",
      "thrift" -> "^0.13.0"
    ),
    scroogeTypescriptPackageDirectory := target.value / "typescript",
    scroogeTypescriptThriftGenOptions := Seq("ts", "node", "es6"),
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


    scroogeTypescriptGenEpisodeFiles := {
      // If you're wondering what are episode files:
      // They are flat files consumed by the thrift command line, each line represents a potential javascript dependency
      // On each line there are two parts delimited by `:`
      // On the left of the `:` is the name of the thrift file
      // On the right of the `:` is the path to the npm package
      // This isn't well documented, I had to read the C source files to understand
      (Compile / scroogeUnpackDeps).value.map { dependency =>
        sLog.value.info(s"Generating the episode file for ${dependency.name}")
        val npmModuleName = scroogeTypescriptPackageMapping.value.getOrElse(dependency.name, dependency.name)

        val episodeFileContent = (dependency ** "*.thrift").get().map { thriftFile =>
          val cleanedName = thriftFile.name.replaceAll(""".thrift$""", "")
          // The thrift command line only allow npm dependencies without prefix.
          // Using the `..` is a way to work around their limitation so npmModuleName can be prefixed with @guardian/...
          s"${cleanedName}_types:../$npmModuleName/${cleanedName}_types"
        }.mkString("\n")

        val episodeDirectory = target.value / "episodes" / npmModuleName
        val episodeFile = episodeDirectory / "thrift.js.episode"
        IO.write(episodeFile, episodeFileContent)
        episodeDirectory
      }
    },


    scroogeTypescriptGenTypescript := {
      val log = sLog.value

      val outputDirOption = s"-out ${scroogeTypescriptPackageDirectory.value}"
      IO.createDirectory(scroogeTypescriptPackageDirectory.value)

      val episodeFileOption = Some(scroogeTypescriptGenEpisodeFiles.value)
        .filter(_.nonEmpty)
        .map(_.mkString("imports=", ":", ""))
        .toSeq

      val generationOptions = (scroogeTypescriptThriftGenOptions.value ++ episodeFileOption ).mkString("--gen js:", ",", "")

      val importDirectoriesOptions = (Compile / scroogeUnpackDeps).value.map { dependency =>
        s"-I ${dependency.getPath}"
      }.mkString(" ")

      val appsRenderingFiles = (Compile / scroogeThriftSourceFolder).value ** "*.thrift"
      val returnCodes = appsRenderingFiles.get().map(thriftFile => {
        val cmdline = s"thrift ${generationOptions} ${importDirectoriesOptions} ${outputDirOption} ${thriftFile.getPath}"
        log.info(s"Generating definitions for ${thriftFile.getName}")
        log.info(cmdline)
        Process(cmdline, baseDirectory.value).!
      })

      if (returnCodes.sum != 0) {
        throw new Exception("Error during thrift compilation, check the output above")
      }

      val definitions = scroogeTypescriptPackageDirectory.value * "*.d.ts"
      val js = scroogeTypescriptPackageDirectory.value * "*.js"
      (definitions +++ js).get
    },


    scroogeTypescriptGenNPMPackage := {
      val generatedSources = scroogeTypescriptGenTypescript.value
      val generatedPackageJson = scroogeTypescriptGenPackageJson.value

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
          |tsc --init
          |tsc
          |
          |To publish the package:
          |npm publish --access public
          |""".stripMargin

      sLog.value.info(message)
      generatedSources ++ generatedReadme :+ generatedPackageJson
    },

    Compile / resourceGenerators += scroogeTypescriptGenNPMPackage,

    scroogeTypescriptCompile := {
      scroogeTypescriptGenNPMPackage.value
      val logger: Logger = sLog.value
      val packageDir = scroogeTypescriptPackageDirectory.value

      runCmd("npm install", packageDir, logger = logger, onError = "Unable to install npm dependencies")

      if (!(packageDir / "tsconfig.json").exists()) {
        runCmd("tsc --init", packageDir, logger = logger, onError = "Unable to initialise the typescript compiler")
      }

      runCmd("tsc", packageDir, logger = logger, onError = "There are compilation errors, check the output above")
    },

    compile := ((compile in Compile) dependsOn scroogeTypescriptCompile).value,

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
