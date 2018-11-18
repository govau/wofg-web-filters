## Mixed content detection filter

Filter that analyses https html documents for mixed content.

The filter as implemented checks HTTPS pages for:

* img src and srcset attributes
* iframe src attributes
* object data attributes
* form action attributes
* embed src attributes
* video src attributes
* audio src attributes
* source src and srcset attributes
* link rel=stylesheet href attributes
* style content for @import '' and @import url() directives
* params value attributes

If any of these conditions are detected the page is flagged with a boolean indicating if mixed mode content was found within the following metadata field: `X-Funnelback-Contains-Mixed-Content`

Limitations: 

* Note: this filter is currently in beta and is not fully tested. 
* Detection of @import directives is currently untested.
* The filter does not inspect the contents of script fields and will not detect mixed content XMLHttpRequest calls, or other URLs contained within the Javascript code.
* The filter does not inspect linked files so mixed content errors as a result of links contained within linked CSS or Javascript files will not be detected.

### Usage

Install the files into the collection's conf folder:

```
$SEARCH_HOME/conf/COLLECTION/@groovy/com/funnelback/CAFilters/DetectMixedContent.groovy
```

Requires:

* `filter.jsoup.classes=com.funnelback.CAFilters.DetectMixedContent`
