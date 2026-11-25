package au.org.raid.orcidupdater.raidapi;

import java.util.List;

/** What the updater needs from a RAiD to invite its contributors. */
public record RaidSummary(String handle, String title, String servicePointName, List<String> contributorIds) {
}
