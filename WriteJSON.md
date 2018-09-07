## WriteJSON

Filter for use with Funnelback.

This filter writes out metadata associated with a document to a JSON log that can be loaded into Elastic Search.

The following metadata is captured:

* Embedded HTML `<meta>` tag based metadata
* HTTP headers that were captured when the document was retrieved
* Some additional metrics captured by the web crawler (including host/domain values, response times)
* Additional metadata produced by analysing the document content.  Additional metadata includes:
    * WCAG 2.0 accessibility information
    * Flesch-Kincaid reading level, declared and detected language
    * Other rule-based content analysis checks
    * Portfolio information, derived by cross-referencing the domain with information in the Australian Government Online Register.
    * GA360 identification, based on a list of know GA360 IDs.  Note: the ga360 mappings file included here has been emptied.

## Usage

Install the files into the collection's conf folder:

```
$SEARCH_HOME/conf/COLLECTION/@groovy/com/funnelback/dta/WriteJSON.groovy
$SEARCH_HOME/conf/COLLECTION/portfolio.mappings.generated
$SEARCH_HOME/conf/COLLECTION/dta-ga360.csv
```

Requires:

* `filter.classes=com.funnelback.dta.WriteJSON`
* Domain to portfolio mappings
* List of GA360 Ids (for the GA360 identification).  

## Output

Produces a `metadata_report-<TIMESTAMP>.json` file to the collection's log folder.

Each line contains a JSON packet consisting of metadata keys extracted by the filter.  The metadata keys produced are prefixed lower-cased versions of the field name, with spaces and punctuation replaced with dashes.

The metadata prefixes are:

| Elastic search variable | Source variable name | ES variable type | Category | Description |
| ======================= | ==================== | ================ | ======== | =========== |
| `m_*` | `<meta name="*">` | (varies) | Metadata (name type) | Fields prefixed with `m_` are standard metadata tags |
| `mh_*` | `<meta http-equiv="*">` | (varies) |  Metadata (http-equiv type) | Fields prefixed with `mh_` are http-equiv style metadata tags |
| `mp_*` | `<meta property="*">` | (varies) | Metadata (property type) | Fields prefixed with `mp_` are property style metadata tags |
| `aa_*` | (Added via filters) | (varies) | Accessibility check | Fields prefixed with `aa_` contain Funnelback accessibility auditor check information |
| `ca_*` | (Added via filters) | (varies) | Content auditor check | Fields prefixed with `ca_` contain Funnelback content auditor check information |
| `h_*` | `*:` (varies) | HTTP header | Fields prefixed with `h_` are HTTP headers |
| `fb_*` | (Added via filters) | (varies) | Funnelback-generated Header metadata | Fields prefixed with `fb_` are Funnelback inserted HTTP headers |