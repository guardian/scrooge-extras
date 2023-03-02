# scrooge-extras

An SBT plugin to generate typescript definitions from thrift files. 

It leverages Twitter's Scrooge plugin to resolve thrift dependencies.

## Why

The Guardian already uses Scrooge to generate scala classes from thrift files, and we also heavily relies on Scrooge to model the dependencies between projects (thrift files are published to maven).

We're now using typescript more and more, so the intent of this plugin is to allow us to hit the ground running and keep our project structure.

## How

This plugin depends on the Scrooge SBT plugin. The dependency resolution and jar unpacking is left to the scrooge plugin, but the typescript generation is handled by this plugin (via scrooge). We've essentially implemented a new backend for the scrooge compiler, and wrapped it into an sbt plugin for convenience.

## Unsupported features (PRs  welcomed!)

 - Thrift services. By lack of time, it's a matter of implementing it.
 - Thrift annotations. Similarly by lack of time.
 - Thrift's `typedef`s will work as expected, however they really should match Typescript's type aliasing feature. I was unable to declare type aliases as in the backend of the scrooge compiler that information is missing.
 - Keyword protection isn't implemented as I haven't found any case where it was necessary yet.

## Supported features

 - Generate typescript source from thrift files
   - Type mapping
   - Thrift namespaces are mapped to npm packages
 - Generate a `package.json`
 - Generate a `tsconfig.json`
 - Publish to NPM

## Namespaces

Namespaces are mapped to npm packages. There are limitations on what characters a thrift namespace can contains, so the namespace string gets transformed to generate the package name. The transformations are the following:

 - `_at_` is replaced by `@`
 - `_` is replaced by `-` to be closer the the NPM naming style
 - `.` are replaced by `/`
 
For instance the namespace `_at_guardian.school.common` will point to the folder `common` of the package `@guardian/school`.

In order to be backward compatible with the `thrift` CLI, the scrooge namespaces are prefixed by the `#` symbol, making them look like a comment to the CLI, but they are indeed processed by scrooge.

```
#@namespace typescript _at_guardian.school.common
```

## Type mapping

### Simple types

|Thrift|Typescript|
|---|---|
|i16|number|
|i32|number|
|i64|Int64|
|double|number|
|byte|number|
|bool|boolean|
|string|string|
|binary|Buffer|
|list|[]|
|set|[]|
|map|{}|

### Enum

Enums are directly mapped to typescript, here's an example of generated enum:

```thrift
enum Type {
  ROBOT
  HUMAN = 4
  DOG
  CAT
}
```

```typescript
export enum Type {
    ROBOT = 0,
    HUMAN = 4,
    DOG = 5,
    CAT = 6,
}
```

### Struct

`struct`s are mapped to `interface`s.

```thrift
struct Student {
  1: required Denomination denomination
  2: required i32 age
  3: optional set<i32> grades = [0, 4]
  5: optional Type type
}
```

```typescript
export interface Student {
    denomination: Denomination
    age: number
    grades: number[] //non optional as there's a default value
    type?: Type
}
```

### Union

Unions are handled with typescript's union types of a set of anonymous interfaces.
An extra attribute named `kind` is added to these interfaces in order to discriminate with a `switch` statement in the client code.

```thrift
union Denomination {
  1: string fullName
  2: string nickName
  3: i32 barcode
}
```

```typescript
export type Denomination =
    {
        kind: "fullName",
        fullName: string
    } |
    {
        kind: "nickName",
        nickName: string
    } |
    {
        kind: "barcode",
        barcode: number
    } ;
```

### Service

Not supported yet

## Generating typescript from sbt

You need the following installed and configured:
 - tsc
 - npm

_Assuming your project is already set up with scrooge to generate scala files_

In `project/plugins.sbt`
```sbt
addSbtPlugin("com.twitter" % "scrooge-sbt-plugin" % "20.4.1")

addSbtPlugin("com.gu" % "sbt-scrooge-typescript" % "<latest_version>")
```

In `build.sbt`
```sbt
enablePlugins(ScroogeTypescriptGen)
scroogeLanguages in Compile := Seq("typescript"),
scroogeTypescriptNpmPackageName := "@guardian/mymodule",

// although it might appear odd at first, these two lines are use to tell the scrooge compiler what NPM package we are generating
// "scroogeDefaultJavaNamespace" is actually badly named as it is the "defaultNamespace" option passed to the compiler
Compile / scroogeDefaultJavaNamespace := scroogeTypescriptNpmPackageName.value,
Test / scroogeDefaultJavaNamespace := scroogeTypescriptNpmPackageName.value,
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

 - scroogeTypescriptGenPackageJson: Generates the `package.json`
 - scroogeTypescriptGenNPMPackage: Generates the typescript, package.json and copy the README.md (if any)
 - scroogeTypescriptCompile: Runs `npm install` and `tsc` to check the generated files are compiling
 - scroogeTypescriptNPMPublish: Runs `npm publish --access public` to publish the package on NPM
 
Here are some settings you may want to use:
 
 - scroogeTypescriptNpmPackageName: The name of the package in `package.json`
 - scroogeTypescriptPackageLicense: The license of the package on NPM
 - scroogeTypescriptPackageMapping: The mapping between scala libraries and their NPM equivalent
 - scroogeTypescriptDevDependencies: The dev dependencies to put in the `package.json`
 - scroogeTypescriptPeerDependencies: The peer dependencies to put in the `package.json`
 - scroogeTypescriptDependencies: The dependencies to put in the `package.json`
 - scroogeTypescriptDryRun: To be able to run everything without publishing to NPM
 - scroogeTypescriptPublishTag: An optional tag for your NPM release, primarily for beta.
 
 
## Using the generated files

With each `struct` or `union` in the thrift definitions, two types are generated.

For instance, for the `struct` `School`, you'll have a generated `interface` called `School`, and a generated `SchoolSerde` class that will be able to serialise and deserialise objects of type `School`. Hence the suffix "Serde" for SERialization-DEserialization.

```typescript
const protocol: TProtocol = ...;
const school = SchoolSerde.read(protocol);
```

```typescript
const protocol: TProtocol = ...;
SchoolSerde.write(protocol, school);
```

## Tests

We have an end-to-end tests that takes a scala model, serialises it from scala, deserialises it from typescript, serialises it from typescript and finally deserialises it from scala. This ensures the value sent and reveived are the same.

Any contribution should ensure the tests are running, in the sbt shell:
```sbtshell
test
```

## How to release this library

Beta releases from a WIP branch can be deployed Maven. To do this, start sbt with

`sbt -DRELEASE_TYPE=beta`

In beta mode, when you follow the remaining instructions, you'll be prompted to confirm that you intend to put out a
beta release and if so, the version number should take the form of `x.y.z.beta.n`. While making beta releases you 
should always update the next version to reflect the beta status of the code when prompted, i.e. don't just let it 
revert to -SNAPSHOT which it will want to do by default. 

When making a prod release once your changes have been through the PR process and are merged to the main/master branch, 
checkout the main/master branch and start sbt as normal (without the -DRELEASE_TYPE parameter) and run

```sbtshell
release cross
```

Following a successful production release, the version details are automatically committed and pushed back to github.
