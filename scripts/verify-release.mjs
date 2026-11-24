/** Verify CurseForge release metadata and uploaded bytes without changing anything (NightBeam publishes to CurseForge only). */
import fs from "node:fs";
import path from "node:path";
import { createHash } from "node:crypto";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const version = process.argv[2];
if (!version) throw new Error("Usage: node scripts/verify-release.mjs VERSION");
const env = fs.readFileSync(path.join(process.env.USERPROFILE, "NightBeam-Knowledge-Base/secrets/local.env"), "utf8");
for (const line of env.split(/\r?\n/)) {
  const match = line.match(/^([^#=]+)=(.*)$/);
  if (match && !process.env[match[1].trim()]) process.env[match[1].trim()] = match[2].trim();
}
async function read(url, headers = {}) {
  const response = await fetch(url, { headers });
  if (!response.ok) throw new Error(`Verification HTTP ${response.status} for ${url}`);
  return response.json();
}
const boots = JSON.parse(fs.readFileSync(path.join(root, `releases/client-verification-${version}.json`)));
if (boots.length !== 8 || boots.some(row => !row.passed)) throw new Error("Eight passing client tests required");
const state = JSON.parse(fs.readFileSync(path.join(root, ".release-upload-state.json")));
const results = [];
for (const boot of boots) {
  const jar = fs.readFileSync(path.join(root, "releases", boot.jar));
  const split = boot.case.lastIndexOf("-");
  const game = boot.case.slice(0, split), loader = boot.case.slice(split + 1);
  const upload = state.uploads.find(row => row.platform === "curseforge" && row.version === version && row.fileName === boot.jar);
  if (!upload?.remoteId) throw new Error(`CurseForge upload ID missing: ${boot.jar}`);
  const cf = (await read(`https://api.curseforge.com/v1/mods/1305455/files/${upload.remoteId}`,
    { "x-api-key": process.env.CURSEFORGE_API_KEY })).data;
  const sha1 = createHash("sha1").update(jar).digest("hex");
  const remoteSha1 = cf.hashes.find(row => row.algo === 1)?.value;
  const loaderLabel = { fabric: "Fabric", forge: "Forge", neoforge: "NeoForge" }[loader];
  const processing = [1, 3, 9, 11, 13, 14, 16, 17, 18, 19, 20, 21, 22, 23].includes(cf.fileStatus);
  if (cf.fileName !== boot.jar || (remoteSha1 ? sha1 !== remoteSha1.toLowerCase() : !processing) || !cf.gameVersions.includes(game)
      || !cf.gameVersions.includes(loaderLabel)) throw new Error(`CurseForge verification failed: ${boot.jar}`);
  if ([2, 5, 6, 7, 8, 12, 15].includes(cf.fileStatus)) throw new Error(`CurseForge rejected or failed: ${boot.jar}; status ${cf.fileStatus}`);
  const fabricDependency = loader !== "fabric" || cf.dependencies.some(row => row.modId === 306612 && row.relationType === 3);
  if (!fabricDependency && !processing) {
    throw new Error(`CurseForge Fabric API relation missing: ${boot.jar}`);
  }
  const row = { case: boot.case, jar: boot.jar, sha512: boot.sha512,
    curseforgeId: cf.id,
    curseforgeStatus: cf.fileStatus, curseforgeStatusName: { 1: "Processing", 3: "UnderReview", 4: "Approved",
      10: "Released", 18: "UnderManualReview", 19: "ScanningForMalware", 20: "ProcessingFile" }[cf.fileStatus] || "Pending",
    curseforgeAvailable: cf.isAvailable,
    curseforgeHashVerified: Boolean(remoteSha1), curseforgeDependencyVerified: fabricDependency };
  results.push(row);
  console.log(JSON.stringify(row));
}
fs.writeFileSync(path.join(root, `releases/platform-verification-${version}.json`), JSON.stringify(results, null, 2));
console.log(`Verified metadata for ${results.length} jars on CurseForge; CurseForge available: ${results.filter(r => r.curseforgeAvailable).length}/9; hashes: ${results.filter(r => r.curseforgeHashVerified).length}/9`);
