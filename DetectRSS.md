## RSS detection filter

Filter that detects RSS feeds by analysing the content. Writes feed type and version to the `X-Funnelback-Feed-Type` and `X-Funnelback-Feed-Version` metadata fields.

Note: this filter has currently only been tested for RSS 2.0 and ATOM feeds.

### Usage

Install the files into the collection's conf folder:

```
$SEARCH_HOME/conf/COLLECTION/@groovy/com/funnelback/CAFilters/DetectRSS.groovy
```

Requires:

* `filter.jsoup.classes=com.funnelback.CAFilters.DetectRSS`
