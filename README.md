# sbt-scrooge-typescript

An SBT plugin to generate typescript definitions from thrift files. 

It leverages Twitter's Scrooge plugin to resolve thrift dependencies.

## Why

The Guardian already uses Scrooge to generate scala classes from thrift files, and we also heavily relies on Scrooge to model the dependencies between projects (thrift files are published to maven).

We're now using typescript more and more, so the intent of this plugin is to allow us to hit the ground running and keep our project structure.

## How

This plugin depends on the Scrooge SBT plugin. The dependency resolution and jar unpacking is left to the scrooge plugin, but the typescript generation is handled by this plugin.

## Unsupported features (PRs  welcomed!)

 - Thrift services. By lack of time, it's a matter of implementing it.
 - Thrift's `typedef`s will work as expected, however they really should match Typescript's type aliasing feature. I was unable to declare type aliases as in the backend of the scrooge compiler that information is missing already. As far as I understand it's resolved in the frontend.
 - Keyword protection isn't implemented as I haven't found any case where it was necessary yet.

## Supported features

 - Simple types are mapped to typescript's native types where possible.
 - `struct`s are represented as `intereface`s
 - `union`s are mapped to their corresponding union of types
 - `enum`s are mapped to `enum`s

## Usage

You need the following installed and configured:
 - tsc
 - npm

_Assuming your project is already set up with scrooge to generate scala files_

In `project/plugins.sbt`
```sbt
addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % "20.4.0")
addSbtPlugin("com.gu" % "sbt-scrooge-typescript" % "1.0.0") // latest version here
```

In `build.sbt`
```sbt
enablePlugins(ScroogeTypescriptGen)
scroogeTypescriptNpmPackageName := "@guardian/mymodule",
scroogeTypescriptPackageLicense := "Apache-2.0",
scroogeTypescriptPackageMapping := Map(
  "scala-library-name" -> "@org/npm-module-name"
)
```

Then you're ready to generate and compile your models
```sbtshell
compile
```

You can control more finely what part of the generation is triggered with the following tasks:

 - scroogeTypescriptGenEpisodeFiles: Generates the episode files (files that model the dependencies between each thrift module)
 - scroogeTypescriptGenTypescript: Generates the typescript files using the `thrift` CLI
 - scroogeTypescriptGenPackageJson: Generates the `package.json`
 - scroogeTypescriptGenNPMPackage: Generates the typescript, package.json and copy the README.md (if any)
 - scroogeTypescriptCompile: Runs `npm install` and `tsc` to check the generated files are compiling
 - scroogeTypescriptNPMPublish: Runs `npm publish --access public` to publish the package on NPM
 
Here are some settings you may want to use:
 
 - scroogeTypescriptNpmPackageName: The name of the package in `package.json`
 - scroogeTypescriptPackageLicense: The license of the package on NPM
 - scroogeTypescriptPackageMapping: The mapping between scala libraries and their NPM equivalent
 - scroogeTypescriptDevDependencies: The dev dependencies to put in the `package.json`
 - scroogeTypescriptDependencies: The dependencies to put in the `package.json`
 - scroogeTypescriptDryRun: To be able to run everything without publishing to NPM
 