IMPROVE: add doco of AWS stuff too?  People can't see the private repo, but no
need for the doco to be private.

# Update changelog

Through development, we add to changelog if/when we're sure that the change 
is likely to go to prod with next release.

When ready to release:
* update the changelog for the release (e.g. change heading from "Not 
  released" to `1.999`)"
* tag the repo with the next tag (e.g. in this case: `raido-v-1.999`)
* add the date we expect to release

For any change that was added to changelog but unexpectedly was not released 
(moved changes onto a feature branch because not ready, or just wholly reverted)
just move the changelog entry from the release version to new "Not released" 
section at the top.