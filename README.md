# sbt-scrooge-typescript

An SBT plugin to generate javascript and typescript definition from thrift files. 

It leverages Twitter's Scrooge plugin to resolve thrift dependencies, and calls the `thrift` command line.

## Why

The Guardian already uses Scrooge to generate scala classes from thrift files, and we also heavily relies on Scrooge to model the dependencies between projects (thrift files are published to maven).

We're now using typescript more and more, so the intent of this plugin is to allow us to hit the ground running and keep our project structure.

## How

This plugin depends on the Scrooge SBT plugin. The dependency resolution and jar unpacking is left to the scrooge plugin. Once these steps are done, the sbt-scrooge-typescript plugin builds the `thrift` command line with the correct parameters in order to generate the typescript files with the correct imports.

## Limits

This plugin relies on the user having the `thrift` CLI installed on their machine. The `thrift` CLI does have a few issues:

 - Dependencies are modeled with "Episode" files in a process called "Episodic compilation". Aside from a pull request and reading the code this isn't documented.
 - It's harder to ensure the CLI version and the plugin version remain aligned as they are installed separately. It's not impossible to be faced with an issue in the future if the CLI introduce breaking changes.
 - Namespace is done at the file name level rather than the full path. So if you have one file called `shared.thrift` in one of your dependencies, and `src/thrift/someFolder/shared.thrift` in your project you will have a name collision. So we have to ensure no two files are named the same across our whole dependency tree.
 - The thrift CLI has a stricter set of reserved words that Scrooge doesn't have. For instance Scrooge is perfectly happy with naming a property `from`, but the `thrift` CLI will raise a compilation error.

## Going forward

We should probably use a proper json library to generate the package.json, something like uPickle would be appropriate.

I'd like to see this plugin generate the typescript source code directly rather than relying on the thrift command line. Scrooge has already got a few nice examples of code generation in scala using Mustache for templating. However upon investigation it turned out to be a larger piece of work than we could allocate for.

So if anyone's keen on extending this plugin, PRs welcome! 

## Usage

You need the following installed and configured:
 - tsc
 - npm
 - thrift

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
 