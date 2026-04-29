This page is expected to kept up to date when terminology is finalised or if 
there are any significant changes in how Raido handles characters.

RSS users: you can watch for changes to a specific file by appending `.atom`
to the URL of the history page (`History` link in the top right of the page)
for that file.
For example to watch this page, subscribe to this 
[RSS feed url](https://github.com/au-research/raid-au/commits/main/doc/character-sets-and-encodings.md.atom).

Non-RSS users: you can `Watch` the GitHub repository, but that will 
notify you for every change across the entire repository.

Major changes and announcements regarding handle generationa and character sets
/encodings will also likely be added as an "Announcement" in the 
[Discussions](https://github.com/au-research/raid-au/discussions) forum.

Please submit issues or feedback using GitHub Issues, Discussions or email to 
`contact@raid.org`.


# Terminology

Exact terminology for the RAID things is still being defined in the handbook.

See the [readme](../readme.md#raid-vs-raido) for a discussion of the difference
between "RAiD" and "Raido".

Example: `https://raid.org/11111.1/99999999`

Note that `11111.1` is not a registered prefix, it's just used here as an 
example.

This topic uses these definitions:

| Term          | Meaning                             |
|---------------|-------------------------------------|
| Domain        | `https://raid.org`                  |
| Handle        | `https://raid.org/11111.1/99999999` |
| Number        | `11111.1/99999999`                  |
| Number prefix | `11111.1`                           |
| Number suffix | `99999999`                          |


# Current functionality 

RAiD handles are now minted under the `raid.org` domain, using DOI-based 
identifiers via DataCite. Legacy handles minted using the 
[ARDC APIDS service](https://github.com/au-research/ANDS-Registry-Core) 
under the shared handle prefixes `102.100.100` and `10378.1` are still 
supported for reading and resolution.

The RAiD service is deployed to `raid.org.au` environments (e.g. 
`app.prod.raid.org.au`, `app.demo.raid.org.au`).


# Character set

## Supported character set for Handles

### Current state

At the moment, RAID Numbers are generated using the CNRI handle system:
* the prefix consists of digits and `.` seperator characters
* the suffix consists of digits
  * it is currently generated from a numeric database sequence


### Future state

The above is the current situation, but the implementation may change in the
future.

Examples of possible future changes:
* allow the suffix to be provided by the user - ASCII only
* allow the suffix to be provided by the user - limited Unicode blocks allowed
  * the allowed Unicode blocks that will be allowed in the suffix may be 
    limited due to security concerns, such as those around 
    [confusable characters](https://unicode.org/faq/idn.html#15aa)


## Supported character set for content

Most of the time, Raido supports all Unicode characters in content.

Currently, metadata field names are restricted to US-ASCII for simplicity.

New RAID agencies may desire to allow non-ASCII in metadata field names for
their own purposes.  We will do further analysis when the need arises.


# Character encoding

Content for API data is encoded by UTF-8 by default.

Both internally (see 
[UTF-8 for content](./code/standard.md#utf-8-encoding-for-all-non-ascii-content))
and at the API level - 
[almost all](./code/standard.md#ascii-for-non-content--urls-filenames-) 
data is UTF-8 encoded and allows any Unicode 
character to saved.

Client applications are expected to be careful with output encoding in their 
usage to avoid
[XSS scripting attacks](https://owasp.org/www-community/attacks/xss/).

Raido may perform some HTML input sanitization on some/all fields, but that 
is not to be expected or relied upon.

In general, the API will return the data to clients exactly as provided to it.


# Character encoding for URLs

The encoding of internet URLs is not under the control of the RAID standard.
In order for URLs to be compatible, they must be encoded as UTF-8 and then 
additionally "percent encoded": https://stackoverflow.com/a/2744184/924597


---  

# References 

* https://raid.org
* https://github.com/au-research/raid-au
* Raido coding standards
  * [UTF-8 for content](./code/standard.md#utf-8-encoding-for-all-non-ascii-content)
  * [ASCII for non-content](./code/standard.md#ascii-for-non-content--urls-filenames-)
* https://en.wikipedia.org/wiki/Unicode_block
* https://stackoverflow.com/a/2744184/924597
* https://owasp.org/www-community/attacks/xss/
* https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html
* https://unicode.org/faq/idn.html

