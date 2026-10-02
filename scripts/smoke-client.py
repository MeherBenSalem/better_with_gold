"""Boot actual release jars in isolated clients and record hashes and startup results.

Requires Python's minecraft-launcher-lib and the JDKs used by the workspaces.
The test observer is a separate Java agent and never enters a released mod jar.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import urllib.request
import zipfile

from minecraft_launcher_lib import command, install, mod_loader

ROOT = Path(__file__).resolve().parent.parent
CACHE = Path.home() / ".cache" / "better-with-gold-tests"
JAVA = {
    "17": Path(os.environ.get("JAVA_HOME_17", str(Path.home() / ".gradle/jdks/eclipse_adoptium-17-amd64-windows.2"))),
    "21": Path(os.environ.get("JAVA_HOME_21", "C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot")),
    "25": Path(os.environ.get("JAVA_HOME_25", "C:/Program Files/Eclipse Adoptium/jdk-25.0.4.101-hotspot")),
}
MATRIX = [(mc, loader) for mc, loaders in (("1.20.1", ("fabric", "forge")),
          ("1.21.1", ("fabric", "forge", "neoforge")), ("26.2", ("fabric", "neoforge")),
          ("26.3", ("fabric", "neoforge"))) for loader in loaders]


def write_report(version):
    rows = []
    for mc, loader in MATRIX:
        file = CACHE / "runs" / version / f"{mc}-{loader}" / "verification.json"
        if file.exists():
            row = json.loads(file.read_text())
            jar = ROOT / "all-jars" / row["jar"]
            if jar.exists() and hashlib.sha512(jar.read_bytes()).hexdigest() == row["sha512"]:
                rows.append(row)
    output = ROOT / "all-jars" / f"client-verification-{version}.json"
    output.write_text(json.dumps(rows, indent=2))


def api(url):
    request = urllib.request.Request(url, headers={"User-Agent": "NightBeam-BWG-release-verification/1"})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def download(file, directory):
    target = directory / file["filename"]
    if not target.exists() or hashlib.sha512(target.read_bytes()).hexdigest() != file["hashes"]["sha512"]:
        with urllib.request.urlopen(file["url"], timeout=120) as response:
            target.write_bytes(response.read())
    if hashlib.sha512(target.read_bytes()).hexdigest() != file["hashes"]["sha512"]:
        raise RuntimeError(f"Download hash mismatch: {target.name}")
    return target


def properties(workspace):
    return dict(line.split("=", 1) for line in (ROOT / workspace / "gradle.properties").read_text().splitlines()
                if "=" in line and not line.startswith("#"))


def build_agent():
    destination = CACHE / "observer"
    destination.mkdir(parents=True, exist_ok=True)
    source = ROOT / "scripts/smoke/ClientBootAgent.java"
    subprocess.run([str(JAVA["17"] / "bin/javac.exe"), "--release", "17", "-d", str(destination), str(source)], check=True)
    manifest = destination / "MANIFEST.MF"
    manifest.write_text("Manifest-Version: 1.0\nPremain-Class: ClientBootAgent\n\n")
    agent = destination / "client-boot-agent.jar"
    subprocess.run([str(JAVA["17"] / "bin/jar.exe"), "cfm", str(agent), str(manifest), "-C", str(destination), "ClientBootAgent.class"], check=True)
    return agent


def run_case(workspace, loader, args, agent, releases, dependencies):
    case = f"{workspace}-{loader}"
    props = properties(workspace)
    java = JAVA[props["java_version"]] / "bin/java.exe"
    game = CACHE / "runs" / args.version / case
    mods = game / "mods"
    mods.mkdir(parents=True, exist_ok=True)
    # Only our isolated test directory is cleared; users' instances are untouched.
    for old in mods.glob("*.jar"):
        old.unlink()
    name = f"better_with_gold-{loader}-{workspace}-{args.version}.jar"
    if args.published:
        release = next(v for v in releases if v["version_number"] == f"{args.version}+{loader}-{workspace}")
        jar = download(next(f for f in release["files"] if f["primary"]), mods)
    else:
        jar = mods / name
        shutil.copy2(ROOT / "all-jars" / name, jar)
    with zipfile.ZipFile(jar) as archive:
        metadata = "\n".join(archive.read(n).decode() for n in archive.namelist()
                             if n in ("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml"))
    for project, versions in dependencies.items():
        if project == "fabric-api" and loader != "fabric":
            continue
        if project == "jauml" and "jauml" not in metadata:
            continue
        matching = next((v for v in versions if workspace in v["game_versions"] and loader in v["loaders"]
                         and (project != "fabric-api" or v["version_number"] == props["fabric_version"])), None)
        if matching is None:
            raise RuntimeError(f"No {project} dependency for {case}")
        download(next(f for f in matching["files"] if f["primary"]), mods)
    installation = CACHE / "minecraft"
    loader_version = props[f"{loader}_loader_version"] if loader == "fabric" else props[f"{loader}_version"]
    mod = mod_loader.get_mod_loader(loader)
    installed = mod.get_installed_version(workspace, loader_version)
    marker = installation / "versions" / installed / ".bwg-installed"
    if not marker.exists():
        print(f"INSTALL {case} loader={loader_version}", flush=True)
        if loader == "neoforge" and workspace.startswith("26."):
            # This launcher's version guard still prefixes NeoForge's new 26.x names with "1.".
            # Use its official installer path directly; keep vanilla/dependency installation intact.
            install.install_minecraft_version(workspace, installation)
            mod._base.install(workspace, str(installation), {}, str(java), loader_version)
            install.install_minecraft_version(installed, installation)
        else:
            mod.install(workspace, installation, loader_version=loader_version, java=java)
        marker.write_text("installed\n")
    result_file = game / "boot-result.txt"
    result_file.unlink(missing_ok=True)
    options_file = game / "options.txt"
    if not options_file.exists():
        options_file.write_text("onboardAccessibility:false\n")
    options = {
        "username": "BWGSmokeTest", "uuid": "8fe5327ed9344f2db9d056463e50d123", "token": "0",
        "executablePath": str(java), "gameDirectory": str(game),
        "launcherName": "BWG-release-test", "launcherVersion": "1",
        "jvmArguments": ["-Xmx2G"] + ([] if args.inspect else [f"-javaagent:{agent}", f"-Dbwg.smoke.result={result_file}"]),
    }
    launch = command.get_minecraft_command(installed, installation, options)
    # Windows command lines can exceed 32K. Java's argument file preserves the exact launcher arguments.
    argument_file = game / ("java-inspection.args" if args.inspect else "java.args")
    argument_file.write_text("\n".join('"' + part.replace('\\', '\\\\').replace('"', '\\"') + '"' for part in launch[1:]), encoding="utf-8")
    print(f"BOOT {case} jar={jar.name}", flush=True)
    start = time.monotonic()
    with (game / ("inspection.log" if args.inspect else "launcher.log")).open("w", encoding="utf-8") as log:
        process = subprocess.Popen([str(java), "@" + str(argument_file)], cwd=game, stdout=log, stderr=subprocess.STDOUT,
                                   creationflags=subprocess.CREATE_NO_WINDOW)
        if args.inspect:
            print(f"Inspection client open: PID {process.pid}; game directory {game}", flush=True)
            return None
        try:
            process.wait(timeout=args.timeout)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
    passed = process.returncode == 0 and result_file.exists() and result_file.read_text().startswith("PASS")
    result = {"case": case, "version": args.version, "loaderVersion": loader_version, "java": str(java),
              "jar": jar.name, "sha512": hashlib.sha512(jar.read_bytes()).hexdigest(),
              "passed": passed, "exitCode": process.returncode, "seconds": round(time.monotonic() - start, 1),
              "bootEvidence": result_file.read_text().strip() if result_file.exists() else None,
              "log": str(game / "launcher.log")}
    (game / "verification.json").write_text(json.dumps(result, indent=2))
    print(("PASS " if passed else "FAIL ") + case + " " + result["log"], flush=True)
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--version", required=True)
    parser.add_argument("--case", action="append", help="e.g. 26.3-fabric; default is all nine jars")
    parser.add_argument("--published", action="store_true", help="test public baseline instead of local artifacts")
    parser.add_argument("--inspect", action="store_true", help="leave one --case client open for manual world/armor inspection")
    parser.add_argument("--timeout", type=int, default=180)
    args = parser.parse_args()
    CACHE.mkdir(parents=True, exist_ok=True)
    if args.inspect and (not args.case or len(args.case) != 1):
        parser.error("--inspect requires exactly one --case")
    agent = None if args.inspect else build_agent()
    releases = api("https://api.modrinth.com/v2/project/B3cmf35A/version") if args.published else []
    dependencies = {name: api(f"https://api.modrinth.com/v2/project/{name}/version") for name in ("jauml", "fabric-api")}
    matrix = [(mc, loader) for mc, loader in MATRIX if not args.case or f"{mc}-{loader}" in args.case]
    results = []
    for workspace, loader in matrix:
        results.append(run_case(workspace, loader, args, agent, releases, dependencies))
        if not args.inspect:
            write_report(args.version)
    if args.inspect:
        return
    write_report(args.version)
    raise SystemExit(0 if results and all(r["passed"] for r in results) else 1)


if __name__ == "__main__":
    main()
