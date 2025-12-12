import raidData from "../raw-data/raids.json";
import type {RaidDto} from "@/generated/raid";

export const raids = raidData as RaidDto[];
