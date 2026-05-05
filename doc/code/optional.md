# Opinions

> Doesn't really matter. This is one way to do it - whatever floats your boat.

## 80 character lines
Code reads down, not across.

Also, even if you set the line length longer - you can't make me use it 😛

Also also, my main monitor is Dual-Quad-HD profile.620+ characters at 11 
points.  Be careful what you wish for.

Why 80?  Because 
[1887 US paper currency containers](https://softwareengineering.stackexchange.com/questions/148677/why-is-80-characters-the-standard-limit-for-code-width/148678#148678) 
were about 20cm wide.


## Prefer descriptive names for type variables
These days most people agree naming their variable x is bad (unless they 
it's an X coordinate or something).

The same applies to type variables.

`T` and `R` are fine when they're used in the canonical way, just like 
`i` and `j` for loop counters.  But the rest of the time - use proper names.

Consider pre-prepending `T` to type-names and using init-caps: 
like `TInput`  and `TResult`.


## Favour early failure/return over wrapping in conditionals

Move validation as far up in the method as possible.  
Throw errors rather than quietly not doing things.  If the conditional 
behaviour is expected and required, document it.

```
if( !x ){
  throw "x was null"
}

if( !x.y ){
  throw "x.y was null"
}

x.y.doSomething()
```
or
```
if( !x || !x.y ){
  return
}

z.y.doSomething()
```
over
```
if( x ){
  if( y ){
    x.y.doSomething()
  }
}
```
Better yet, use guard clause functions for stuff that should fail:

```
guardHasValue(x)
guardHasValue(y)
x.y.doSomething()
```

And of course, optional chaining is fine if it's available:
```
x?.y?.doSomething()
```

The best solution is to not need the conditionals in the first place - 
remember: not null by default.  
The guard conditionals are nice, they definitely aid diagnosability of the 
system, but if you're talking about internal code, they're optional.


## Favour singular over plural at the "module level"
* by "module" I'm talking here about higher level stuff like
  * git repositories, orgs, teams, projects
  * module, library names
  * enums, classes
  * table names
  * directories, files
* examples
  * /resource over /resources
    * note: for maven/gradle projects, you will see /src/main/resources
    * because that's the default for those kind of projects
    * it's not worth customising just to fit in with a naming guideline
    * minor inconsistency inside the codebase is not worth departing from 
    how most other projects are structured
  * /util over /utils
  * StringUtil vs StringUtils
* plural is ok for fields or members though
  * e.g. util.file.Resource.privateResources
  * but the type (array list, or whatever) gives you enough context, if you 
  wanna stick with singular, that's fine
  
### Reasoning
* Consistency, I always flip-flop in the moment and the codebase ends up being 
an inconsistent mess.
  * This way I have a guideline and when I've done it the "wrong" way when busy 
    in the moment, I have an excuse to fix it 🙃
* singular is usually shorter
  * especially, modules are often referred to frequently as prefixes
  * so it's a multiplier, shorter names give shorter references, paths, etc.
* english pluralisation and grammar rules is dumb


## Consider avoiding negation in conditionals

In the pursuit of increased readability of the overall logic.

Instead of `if( !isBlank(field) ){...}`, consider if you can refactor to 
something like `if( hasValue(field) ){...}`.

Some people go to the length of writing methods lie `isNotBlank()`, but it's 
arguable if that actually achieves the goal.

Please don't write `if( !isNotBlank() ){...}`.
