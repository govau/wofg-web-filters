## Social tag extraction filter

Filter that analyses the document content and extracts Twitter user mentions and hash tags. Detected user mentions/tags and counts of occurrences are written to 

* `X-Funnelback-Twitter-User-Tags`
* `X-Funnelback-Twitter-User-Tags-count`
* `X-Funnelback-Twitter-Hash-Tags`
* `X-Funnelback-Twitter-Hash-Tags-count`

### Usage

Install the files into the collection's conf folder:

```
$SEARCH_HOME/conf/COLLECTION/@groovy/com/funnelback/CAFilters/SocialTags.groovy
```

Requires:

* `filter.jsoup.classes=com.funnelback.CAFilters.SocialTags`

