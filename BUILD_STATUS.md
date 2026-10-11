# Build status: NAKAM

Build step ka outcome: failure (agar 'skipped' hai to SDK setup step fail hua tha)

Akhri 80 lines (build.log):
```

Welcome to Gradle 8.7!

Here are the highlights of this release:
 - Compiling and testing with Java 22
 - Cacheable Groovy script compilation
 - New methods in lazy collection properties

For more details see https://docs.gradle.org/8.7/release-notes.html

To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.7/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 

> Configure project :app
e: file:///home/runner/work/RaftarVpn/RaftarVpn/app/build.gradle.kts:37:40: Unresolved reference: util

FAILURE: Build completed with 2 failures.

1: Task failed with an exception.
-----------
* Where:
Build file '/home/runner/work/RaftarVpn/RaftarVpn/app/build.gradle.kts' line: 37

* What went wrong:
Script compilation error:

  Line 37:                 ksFile.writeBytes(java.util.Base64.getDecoder().decode(testKeyB64))
                                                  ^ Unresolved reference: util

1 error

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.
==============================================================================

2: Task failed with an exception.
-----------
* Where:
Build file '/home/runner/work/RaftarVpn/RaftarVpn/app/build.gradle.kts' line: 37

* What went wrong:
Script compilation error:

  Line 37:                 ksFile.writeBytes(java.util.Base64.getDecoder().decode(testKeyB64))
                                                  ^ Unresolved reference: util

1 error

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.
==============================================================================

BUILD FAILED in 53s
```
