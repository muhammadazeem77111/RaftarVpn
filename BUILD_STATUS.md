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
e: file:///home/runner/work/RaftarVpn/RaftarVpn/settings.gradle.kts:1:1: Unresolved reference: cGx1Z2luTWFuYWdlbWVudCB7CiAgICByZXBvc2l0b3JpZXMgewogICAgICAgIGdvb2dsZSgpCiAgICAgICAgbWF2ZW5DZW50cmFsKCkKICAgICAgICBncmFkbGVQbHVnaW5Qb3J0YWwoKQogICAgfQp9CmRlcGVuZGVuY3lSZXNvbHV0aW9uTWFuYWdlbWVudCB7CiAgICByZXBvc2l0b3JpZXNNb2RlLnNldChSZXBvc2l0b3JpZXNNb2RlLkZBSUxfT05fUFJPSkVDVF9SRVBPUykKICAgIHJlcG9zaXRvcmllcyB7CiAgICAgICAgZ29vZ2xlKCkKICAgICAgICBtYXZlbkNlbnRyYWwoKQogICAgfQp9CnJvb3RQcm9qZWN0Lm5hbWUgPSAiUmFmdGFyVnBuIgppbmNsdWRlKCI6YXBwIikK

FAILURE: Build failed with an exception.

* Where:
Settings file '/home/runner/work/RaftarVpn/RaftarVpn/settings.gradle.kts' line: 1

* What went wrong:
Script compilation error:

  Line 1: cGx1Z2luTWFuYWdlbWVudCB7CiAgICByZXBvc2l0b3JpZXMgewogICAgICAgIGdvb2dsZSgpCiAgICAgICAgbWF2ZW5DZW50cmFsKCkKICAgICAgICBncmFkbGVQbHVnaW5Qb3J0YWwoKQogICAgfQp9CmRlcGVuZGVuY3lSZXNvbHV0aW9uTWFuYWdlbWVudCB7CiAgICByZXBvc2l0b3JpZXNNb2RlLnNldChSZXBvc2l0b3JpZXNNb2RlLkZBSUxfT05fUFJPSkVDVF9SRVBPUykKICAgIHJlcG9zaXRvcmllcyB7CiAgICAgICAgZ29vZ2xlKCkKICAgICAgICBtYXZlbkNlbnRyYWwoKQogICAgfQp9CnJvb3RQcm9qZWN0Lm5hbWUgPSAiUmFmdGFyVnBuIgppbmNsdWRlKCI6YXBwIikK
          ^ Unresolved reference: cGx1Z2luTWFuYWdlbWVudCB7CiAgICByZXBvc2l0b3JpZXMgewogICAgICAgIGdvb2dsZSgpCiAgICAgICAgbWF2ZW5DZW50cmFsKCkKICAgICAgICBncmFkbGVQbHVnaW5Qb3J0YWwoKQogICAgfQp9CmRlcGVuZGVuY3lSZXNvbHV0aW9uTWFuYWdlbWVudCB7CiAgICByZXBvc2l0b3JpZXNNb2RlLnNldChSZXBvc2l0b3JpZXNNb2RlLkZBSUxfT05fUFJPSkVDVF9SRVBPUykKICAgIHJlcG9zaXRvcmllcyB7CiAgICAgICAgZ29vZ2xlKCkKICAgICAgICBtYXZlbkNlbnRyYWwoKQogICAgfQp9CnJvb3RQcm9qZWN0Lm5hbWUgPSAiUmFmdGFyVnBuIgppbmNsdWRlKCI6YXBwIikK

1 error

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 22s
```
