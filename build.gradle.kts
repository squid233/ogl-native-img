plugins {
    id("java")
    id("org.graalvm.buildtools.native") version "0.11.1"
}

group = "io.github.squid233"
version = "1.0-SNAPSHOT"

val overrunglVersion = "0.2.0-SNAPSHOT"
val overrunglOs = System.getProperty("os.name")!!.let { name ->
    when {
        "FreeBSD" == name -> "freebsd"
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } -> "linux"
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } -> "macos"
        arrayOf("Windows").any { name.startsWith(it) } -> "windows"
        else -> throw Error("Unrecognized platform $name. Please set \"overrunglOs\" manually")
    }
}
val overrunglArch = System.getProperty("os.arch")!!.let { arch ->
    when (overrunglOs) {
        "freebsd" -> "x64"
        "linux" -> if (arrayOf("arm", "aarch64").any { arch.startsWith(it) }) {
            if (arch.contains("64") || arch.startsWith("armv8")) "arm64" else "arm32"
        } else if (arch.startsWith("ppc")) "ppc64le"
        else if (arch.startsWith("riscv")) "riscv64"
        else "x64"

        "macos" -> if (arch.startsWith("aarch64")) "arm64" else "x64"
        "windows" -> if (arch.startsWith("aarch64")) "arm64" else "x64"
        else -> throw Error("Unrecognized architecture $arch for platform $overrunglOs. Please set \"overrunglArch\" manually")
    }
}

val overrunglNatives = "natives-$overrunglOs-$overrunglArch"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform("io.github.over-run:overrungl-bom:$overrunglVersion"))
    implementation("io.github.over-run:overrungl")
    implementation("io.github.over-run:overrungl-glfw")
    implementation("io.github.over-run:overrungl-opengl")
    runtimeOnly("io.github.over-run:overrungl-glfw::$overrunglNatives")
}

graalvmNative {
    binaries {
        named("main") {
            mainClass = "io.github.squid233.test.OglNativeImg"
            buildArgs("--enable-native-access=ALL-UNNAMED", "-O3", "-J-Xmx4g", "-march=native")
            if (overrunglOs == "windows") {
                buildArgs("-H:NativeLinkerOption=/SUBSYSTEM:WINDOWS", "-H:NativeLinkerOption=/ENTRY:mainCRTStartup")
            }
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
                vendor.set(JvmVendorSpec.matching("Oracle Corporation"))
            })
        }
    }
}

// https://github.com/oracle/graal/issues/11795
// pass -Djava.io.tmpdir=rootDir/build/native-img-tmp
