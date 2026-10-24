import os
import glob
import shutil
import zipfile
import subprocess
import tempfile
import argparse
from scripts.local_paths import settings

PROJECT_DIR = os.path.dirname(os.path.abspath(__file__))
SRC_JAVA = os.path.join(PROJECT_DIR, "src", "main", "java")
SRC_RESOURCES = os.path.join(PROJECT_DIR, "src", "main", "resources")
BUILD_DIR = os.path.join(PROJECT_DIR, "build")
CLASSES_DIR = os.path.join(BUILD_DIR, "classes")
OUTPUT_JAR = os.path.join(BUILD_DIR, "skyblock-toolkit-1.1.0.jar")

LOCAL = settings()
PRISM_DATA = os.environ.get("PRISM_DATA", os.path.join(os.path.expanduser("~"), ".var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher"))
def configured(key, variable, relative):
    return os.environ.get(variable, LOCAL.get(key, os.path.join(PRISM_DATA, relative)))
MC_JAR = configured("minecraft_jar", "MC_JAR", "libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar")
FAPI_JAR = configured("fabric_api_jar", "FAPI_JAR", "instances/Complex-Skyblock-2/.minecraft/mods/fabric-api-0.160.0+26.2.jar")
LOADER_JAR = configured("loader_jar", "LOADER_JAR", "libraries/net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar")
JAVAC = configured("javac", "JAVAC", "java/java-runtime-epsilon/bin/" + ("javac.exe" if os.name == "nt" else "javac"))
LIBRARIES_DIR = configured("libraries_dir", "LIBRARIES_DIR", "libraries")

def main():
    print("Building Skyblock Toolkit mod...")
    for path in (MC_JAR, FAPI_JAR, LOADER_JAR, JAVAC):
        if not os.path.isfile(path):
            raise SystemExit(f"Missing dependency: {path}. Configure local-settings.json (see README).")
    
    # 1. Clean build directory
    if os.path.exists(BUILD_DIR):
        shutil.rmtree(BUILD_DIR)
    os.makedirs(CLASSES_DIR, exist_ok=True)

    # 2. Extract Fabric API nested jars
    temp_dir = tempfile.mkdtemp()
    try:
        with zipfile.ZipFile(FAPI_JAR) as z:
            for name in z.namelist():
                if name.startswith("META-INF/jars/") and name.endswith(".jar"):
                    z.extract(name, temp_dir)
        
        jars_dir = os.path.join(temp_dir, "META-INF", "jars")
        fapi_sub_jars = [os.path.join(jars_dir, f) for f in os.listdir(jars_dir) if f.endswith(".jar")]
        
        prism_libs = LOCAL.get("library_jars") or [p for p in glob.glob(os.path.join(LIBRARIES_DIR, "**", "*.jar"), recursive=True) if zipfile.is_zipfile(p)]
        
        classpath = os.pathsep.join([MC_JAR, LOADER_JAR] + fapi_sub_jars + prism_libs)
        
        # 3. Find all java source files
        java_files = []
        for root, _, files in os.walk(SRC_JAVA):
            for file in files:
                if file.endswith(".java"):
                    java_files.append(os.path.join(root, file))
        
        print(f"Found {len(java_files)} Java files to compile:")
        for jf in java_files:
            print(" -", os.path.relpath(jf, SRC_JAVA))
        
        # 4. Compile with javac
        # An argument file avoids Windows' command-line length limit.
        compiler_args = ["-encoding", "UTF-8", "--release", "25", "-proc:none", "-cp", classpath, "-d", CLASSES_DIR] + java_files
        argfile = os.path.join(temp_dir, "javac.args")
        with open(argfile, "w", encoding="utf-8") as output:
            for arg in compiler_args:
                output.write('"' + arg.replace('\\', '/').replace('"', '\\"') + '"\n')
        cmd = [JAVAC, "@" + argfile]
        print("Running javac...")
        res = subprocess.run(cmd, capture_output=True, text=True)
        if res.returncode != 0:
            print("Compilation FAILED:")
            print(res.stderr)
            exit(1)
        print("Compilation SUCCEEDED!")

        # 5. Copy resources
        for root, _, files in os.walk(SRC_RESOURCES):
            for file in files:
                src_path = os.path.join(root, file)
                rel_path = os.path.relpath(src_path, SRC_RESOURCES)
                dest_path = os.path.join(CLASSES_DIR, rel_path)
                os.makedirs(os.path.dirname(dest_path), exist_ok=True)
                shutil.copy2(src_path, dest_path)
        
        # 6. Create jar archive
        print("Packaging jar...")
        with zipfile.ZipFile(OUTPUT_JAR, "w", compression=zipfile.ZIP_DEFLATED) as z:
            for root, _, files in os.walk(CLASSES_DIR):
                for file in files:
                    src_file = os.path.join(root, file)
                    rel_name = os.path.relpath(src_file, CLASSES_DIR)
                    z.write(src_file, rel_name)
        
        print(f"Jar created at: {OUTPUT_JAR} ({os.path.getsize(OUTPUT_JAR)} bytes)")
        
        print("Run python scripts/install.py to install safely, or add --wait to queue until Minecraft closes.")
    finally:
        shutil.rmtree(temp_dir)

if __name__ == "__main__":
    from scripts.local_paths import minecraft_version
    parser = argparse.ArgumentParser(description="Build the maintained Fabric pack target.")
    parser.add_argument("--minecraft", choices=("1.21.11", "26.2"), default=minecraft_version())
    args = parser.parse_args()
    if args.minecraft == "26.2":
        main()
    else:
        project = os.path.join(PROJECT_DIR, "versions", "1.21.11")
        wrapper = [os.path.join(project, "gradlew.bat")] if os.name == "nt" else ["bash", "gradlew"]
        env = dict(os.environ)
        if not env.get("JAVA_HOME") and os.path.isfile(JAVAC):
            env["JAVA_HOME"] = os.path.dirname(os.path.dirname(JAVAC))
        subprocess.run([*wrapper, "build", "--no-daemon", "--console=plain"], cwd=project, env=env, check=True)
