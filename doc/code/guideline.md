
# Guidelines

> At least just think about it for a second.

## Imperative commit messages

Use the imperative mood for commit messages: 
https://git.kernel.org/pub/scm/git/git.git/tree/Documentation/SubmittingPatches?h=v2.36.1#n181

Try to stick to 50/72 format for lines, as per 
https://stackoverflow.com/q/2290016/924597


## Target "TODO" comments

TODO comments you see from me will be of the form: // TODO:STO - blah

This is me saying that the thing must be changed (before commit, before push, 
before release) and that I am responsible for doing that.

Don't make TODOs that someone is not responsible for.  Don't try to make me 
responsible for TODOs that I have not agreed to - won't work.

Don't make TODOs that aren't gonna get done - downgrade them to 
IMPROVE: or NOTE:

When a dev leaves the project, someone must take over their TODOs, or downgrade
them.

#### `SECURITY:XXX`
* security comments are used for serious security-related stuff.
* They must be investigated and either fixed or downgraded before any public 
release (if if demo or test is public, then must fix)
* used for security holes, or potential security holes - they're not "fix 
  soon", they're "do not release without fixing"
* examples
  * an unsecured endpoint
  * usage of a known insecure algo
  * code with a suspected injection vulnerability
  * something security related that you're not sure about
    * raise the issue, make a decision, then document and downgrade to an 
    IMPROVE: or NOTE:

#### `TODO:XXX`
* important stuff that really should be addressed ASAP
* Ideally, TODOs should rarely be seen in code that gets pushed to repo
* examples
  * using an algo that won't scale
  * hardcoded value/assumption that won't work long term
  * something not cleaned up (i.e. a temp file)
  * needs public formal documentation

#### `IMPROVE:`

* something that can be improved, but you don't have time to do
* examples:
  * use a better algorithm
  * better implementation to reduce garbage
  * needs better documentation

#### Other
For other notes, just use a normal comment (i.e 1 leading asterisk instead 
of 2).

* acknowledging an alternative implementation exists 
  * explain why the current implementation was chosen
* something could be better, but it's not a big deal
  * explain why it's not a big deal 


## Favour fail-fast config

Fail at start instead of later.

Means there's less smoke testing to do on a release, because if 
config/credentials are missing or malformed - the node won't start.   
i.e. less need to run around trying to exercise functionality to verify that 
the node is functional (which people always shortcut or skip entirely).

This also helps with container-orchestration tool deployments (i.e. AWS ECS or 
Kubernetes) - if the node fails on startup, then the orchestrator knows not to 
consider the deployment a success and can roll-back.

Also, the code isn't doing pointless validation on every invocation that only 
needs to be done once at startup.


## Configure for development by default

Configuration defaults are set in 
[application.yaml](/api-svc/raid-api/src/main/resources/application.yaml) 
with profile-specific overrides for deployed environments.


## MTTS > MTBF

mean-time-to-start over mean-time-between-failure.

Really more of an architectural guideline than coding guideline.


## Not-null by default

### SQL

Favour not null columns, use empty string or 0 where it makes sense 
instead of null.

Make null mean something, document what it means.
Add Postgres comments to nullable columns in your flyway scripts explaining 
what's the what - the comments will be propagated into the Jooq types and thus 
will be easily available to devs from within their IDE.

### Java
For Raido code - if you've got a string with no Optional wrapper and no doco 
that it might be null - then you are allowed to assume it won't be null 
(you can still check it if you want, but you don't need to).

For any parameter or return value that may be null, wrap it in an optional or 
document that it might be null.  You don't need to document anything 
`@NotNullable`, because that's the default - anything not explicitly called out 
as being nullable is assumed not-nullable.  Configure your IDE with this 
expectation.

This default not-null approach can be assumed all the way down to the database.

Remember that external APIs don't necessarily obey this guideline though.

### Typescript
Generally same as java; but the customs are different because of truthiness 
concept and better handling of falsey values ( ?.  and ??  etc.)


## Code review

If you're getting into long back and forth conversations in a PR - you're 
doing it wrong.  Go talk to the person; if that's not working for you, 
go talk to someone else about it.  Passive aggressive PR arguments are a 
net-negative - even if you "win", we all lose.

Break large PRs into smaller reviewable chunks where possible.
For complicated PRs that will be hard to review, talk to the person you want
to review the code first - walk them through it.

### Reviewer
Code review is not a quality gate.

Your review comments are not "you must fix this".  
More on the order of "consider this".

Dev is free to merge if they want - though it's polite to respond to comments.

### Reviewee
Feedback issues should be considered as a valuable external viewpoint on 
your code.  If someone says "what if X is null", consider their viewpoint and 
if it's valid but also consider if it's worth documenting in the code.  

Note that "what if it's null" for a normal internal field is invalid feedback 
given our guidelines (not-null by default) - respond that way to a comment, 
don't just add a null check you know is not necessary just to respond to an 
offhand comment in a PR. If its an external value, but you know it can't be 
null, consider adding a comment to that effect (_why_ you're sure it can't be 
null, not _that_ you're sure).  If someone is asking the question in a review, 
it's likely anyone without your context (or you yourself in a few months) will 
be asking that same question when reading the code.


## Use dev scratch area for non-prod code
Use this if you want to commit code that isn't prod code and isn't tests. 
Just because you use a unit test to scaffold some code, that doesn't make it 
a test. Don't put it in the standard test areas (unit, integration, etc.) 
with an ignore tag - put it in the scratch area.

Most quality standards don't apply to the scratch area (except - you still 
can't commit credentials in here!).  You can use libraries that would 
otherwise not be suitable for prod, etc.

Be prepared for dev scratch code to get deleted if it's causing 
build/deployment issues (because it's in the git history anyway, you can
restore it and re-commit when it's not getting in other people's way).


## Test guidelines
Probably needs its own page.

Add stuff about self-checking, re-runnable, etc.
Difference between unit, integration, functional, load, etc.


## 2 spaces
2 spaces, no tabs - this is the way.

Also: Coke > Pepsi, Picard > Kirk.  True Facts.

