
## Use bigint identity for surrogate keys

You can use a natural key as your primary key if it makes sense to you.

When using a surrogate key though, this is the default approach:
```sql
create table xxx (
  id bigint generated always as identity
    (start with 1)
    primary key not null
)
```

The `start with 1` clause is about avoiding having different tables
using the same identities, so that accidental transposition errors 
(because all the table surrogate keys have the same type) will fail quickly and
are likely to be detected early.

`bigint` is 64bits, plenty of space to spare. 
Don't have do it if you don't want to.


## Specify max length for text columns

It's easy to bump it up when you need to.
Hard to reduced it down once people abuse it and put giant things
in there that you weren't expecting.

If in doubt, use `VARCHAR(256)`.  There's nothing special about `256` and 
there's no logical reason to use powers of 2 for character lengths.  It's 
just a silly little forcing function, like using fibonacci numbers for story
points.  

If you know you need more, or know your exactly limit, then do it.

For large values, before worrying about factoring out to a separate table, learn
about postgrest TOAST.  Then think about separating it out anyway :)

Using `varchar` at the moment, because I couldn't see how to limit it with
a `text` column.

There is this:
https://dba.stackexchange.com/questions/189869/is-varcharx-as-fast-as-text-check-char-lengthx

But that means we lose the ability to find the column length from Java.
Note really that big a deal.


## Unique text constraints

This was used on `service_point.name`: https://stackoverflow.com/a/65675825/924597
I like this because:
* retains accessible metadata about max column length 
  * no need for extension like `citext`, which I'm not sure is usable on RDS, 
    and then have to manage it across local dev and RDS
  * using extensions is not disallowed, we just have to make sure it works 
    everywhere and be sure we really want to do that.


## DB index names

Table and Index share the same namespace.

I don't really mind what naming standard, I try to follow:
* https://stackoverflow.com/a/4108266/924597
* see also: https://www.postgresql.org/message-id/29939.999752832%40sss.pgh.pa.us

```
{tablename}_{columnname(s)}_{suffix}

where the suffix is one of the following:

pkey for a Primary Key constraint
key for a Unique constraint
excl for an Exclusion constraint
idx for any other kind of index
fkey for a Foreign key
check for a Check constraint
Standard suffix for sequences is

seq for all sequences
```

Propose something if you have a strong opinion.


## DB constraint names

I believe they're unique per table.

No naming standard, try to use a name that will make sense if the constraint
gets violated.  
Don't be afraid to add a comment.