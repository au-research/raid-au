import fs from "node:fs";
import path from "node:path";
import type { RaidDto } from "@/generated/raid";
import { sanitizeRaidForDownload } from "@/utils/sanitize-raid";

export function GET() {
  // Path to your existing JSON file in the src directory
  const jsonPath = path.resolve("./src/raw-data/raids.json");
  const rawData = fs.readFileSync(jsonPath, "utf-8");
  const raids = JSON.parse(rawData) as Partial<RaidDto>[];
  const sanitizedRaids = raids.map(sanitizeRaidForDownload);

  return new Response(JSON.stringify(sanitizedRaids), {
    headers: {
      "Content-Type": "application/json",
    },
  });
}
