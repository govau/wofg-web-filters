## Language detection filter

Filter that analyses the document content to determine the language. Writes detected language to the `X-Funnelback-Detected-Language` metadata field.

### Usage

Install the files into the collection's conf folder:

```
$SEARCH_HOME/conf/COLLECTION/@groovy/com/funnelback/CAFilters/DetectLang.groovy
$SEARCH_HOME/conf/COLLECTION/@share/langdetect/*
```

Requires:

* `filter.jsoup.classes=com.funnelback.CAFilters.DetectLang`
* `collection.cfg` entry - set the path of the language files:
    * `filter.langdetect.profilepath=$SEARCH_HOME/conf/$COLLECTION_NAME/@share/langdetect/profiles/`
