# Building on Windows

Requires **Java 17** and an internet connection for the first build.

1. Open the project folder and run `BUILD_MAGICITE.bat`.
2. The script finds Java 17, downloads the official Forge 1.20.1 (47.4.20) development kit if necessary, and runs Gradle.
3. If the build succeeds, the JAR will be in `READY_FOR_TEST/` (the file name includes the version from `gradle.properties`).
4. Remove any older Magicite JARs and install the same new JAR on a test client and test server. Back up the world before trying it on the main server.

If the build fails, see `BUILD_LOG.txt` in the project folder. For a game crash, see the instance's `logs/latest.log`.

The source includes tests that can be run on Linux/macOS with Java 17 and Python 3:

```sh
sh run_pure_tests.sh
```

Those tests do **not** replace a full Forge build or in-game testing.
