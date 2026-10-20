import os
import glob
import shutil
import zipfile
import subprocess
import tempfile

PROJECT_DIR = os.path.dirname(os.path.abspath(__file__))
SRC_JAVA = os.path.join(PROJECT_DIR, "src", "main", "java")
SRC_RESOURCES = os.path.join(PROJECT_DIR, "src", "main", "resources")
BUILD_DIR = os.path.join(PROJECT_DIR, "build")
CLASSES_DIR = os.path.join(BUILD_DIR, "classes")
OUTPUT_JAR = os.path.join(BUILD_DIR, "skyblock-toolkit-1.0.0.jar")

MC_JAR = "/home/jeremy/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/libraries/com/mojang/minecraft/26.2/minecraft-26.2-client.jar"
FAPI_JAR = "/home/jeremy/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/Complex-Skyblock-2/.minecraft/mods/fabric-api-0.160.0+26.2.jar"
LOADER_JAR = "/home/jeremy/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/libraries/net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar"
JAVAC = "/home/jeremy/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/java/java-runtime-epsilon/bin/javac"
MODS_DIR = "/home/jeremy/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/instances/Complex-Skyblock-2/.minecraft/mods"

def main():
    print("Building Skyblock Toolkit mod...")
    
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
        
        prism_libs = glob.glob("/home/jeremy/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher/libraries/**/*.jar", recursive=True)
        
        classpath = ":".join([MC_JAR, LOADER_JAR] + fapi_sub_jars + prism_libs)
        
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
        cmd = [JAVAC, "-proc:none", "-cp", classpath, "-d", CLASSES_DIR] + java_files
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
        
    finally:
        shutil.rmtree(temp_dir)

if __name__ == "__main__":
    main()
