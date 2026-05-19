### Use Gradle as the default build tool

* Status: final                                    
* Who:  finalised by STO                               
* When: finalised on 2022-07-21
* Related: no related ADRs


# Decision

Use Gradle as the default build tool.
Bootstrap Gradle with gradle-wrapper.

But you can override locally with tools that make sense:
* aws-cdk using esbuild
* raid-agency-app using Vite

Avoid writing lots of imperative logic in build scripts.  Factor it out to 
`/buildSrc` ASAP.

## STO opinion 

### Gradle vs Maven

Opinion: Gradle for applications and infrastructure, Maven for libraries

Raido has lots of build logic.  Gradle has good support for this, solves the
"chicken-egg" problem of "build the build" logic, including easy sharing of 
logic.

Gradle solves this with a nice spectrum of build techniques from quick'n'dirty
 to full plugin publising:

* imperative code in the build script
  * can't do easily in Maven, because XML based
* imperative code in /buildSrc
* plugin code in /buildSrc
* publishing to the plugin portal or local repo

Addresses scalability of code too, with ability to start with Groovy and move to 
Java, or these days just start with Kotlin.


# Context

The obvious choices are:
* Gradle
* Maven
* NPM
  * or other sub-tooling (Grunt, Gulp, etc.)


# Consequences

* Maven people annoyed, NPM people annoyed :)
* Gradle is easy to abuse by writing too much dodgy imperative logic in scripts
* this means that building requires JDK to be pre-isntalled



