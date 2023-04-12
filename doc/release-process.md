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

# Consider branching

For v1.1 we needed a branch because non-prod code was committed to `main`.
This will probably continue moving forward, so that changes aren't stuck in 
feature branches during the deployment testing pase.

Around about the time it starts making sense to update the changelog with a 
concrete next version number and tentative release date, is probably a good 
time to start considering a new prod targeted branch like v`1.2`.


# Tag 

When the deployment is imminent, tag the new version.

To tag a new version, invoke:
`git tag --annotate raido-v-<version> <COMMITHASH> -m "manual tag"`
eg: `git tag --annotate raido-v-1.999 HEAD -m "manual tag"`

Dont' forget to make sure you push the tag to Github, something like:
`git push --follow-tags`  (I've never done this, I use IDEA).


To list tags: `git tag`

To fetch tags from origin: `git fetch --tags`

Delete a tag: `git tag -d raido-v-1.999`