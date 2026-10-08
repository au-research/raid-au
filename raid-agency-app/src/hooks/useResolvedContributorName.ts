import { fetchFromOrcidPublicApi } from "@/containers/orcid-lookup/ORCID";
import { fetchIsniName } from "@/services/isni";
import { getIsniBody, getOrcidBody } from "@/utils/contributor-utils/contributor-identifier";
import { useQuery } from "@tanstack/react-query";

/**
 * Resolves a Contributor's display name live - from ORCID's own public API
 * for ORCID (called directly from the browser), or from our own backend's
 * ISNI name endpoint for ISNI (ISNI's real resolver has no public,
 * CORS-enabled API the browser can call directly, unlike ORCID's).
 *
 * `enabled` gates both queries further (e.g. ContributorDetailsForm only
 * wants to resolve while its read-only display is showing, not while the
 * editable widget is).
 */
export function useResolvedContributorName(
  id: string | undefined,
  isIsni: boolean,
  enabled = true
): string {
  const orcidBody = !isIsni ? getOrcidBody(id ?? "") : null;
  const orcidNameQuery = useQuery({
    queryKey: ["orcid-name", orcidBody],
    queryFn: async () => {
      const person = await fetchFromOrcidPublicApi(orcidBody!);
      return (
        person.creditName ||
        [person.givenName, person.lastName].filter(Boolean).join(" ")
      );
    },
    enabled: !!orcidBody && enabled,
  });

  const isniBody = isIsni ? getIsniBody(id ?? "") : null;
  const isniNameQuery = useQuery({
    queryKey: ["isni-name", isniBody],
    queryFn: () => fetchIsniName({ isni: isniBody! }),
    enabled: !!isniBody && enabled,
  });

  const body = isIsni ? isniBody : orcidBody;
  const nameQuery = isIsni ? isniNameQuery : orcidNameQuery;

  // Bug fix: a disabled TanStack Query (no recognised id to resolve) stays
  // isPending forever - it never reaches isError - so checking "no body at
  // all" explicitly first avoids getting stuck on "Resolving…" indefinitely.
  if (!body) return "Not available";
  if (nameQuery.isPending) return "Resolving…";
  if (nameQuery.isError || !nameQuery.data) return "Not available";
  return nameQuery.data;
}
