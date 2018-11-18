## Check Content filter

Rule-based content checking filter for use with Funnelback.

Install the files into the collection's conf folder:

```
$SEARCH_HOME/conf/COLLECTION/@groovy/com/funnelback/CAFilters/CheckContent.groovy
$SEARCH_HOME/conf/COLLECTION/check-content.cfg
$SEARCH_HOME/conf/COLLECTION/check-content.validation-patterns.cfg
$SEARCH_HOME/conf/COLLECTION/check-content.word-list.plain-english.cfg
$SEARCH_HOME/conf/COLLECTION/check-content.word-list.weasel-words.cfg
```

Requires:

* `filter.jsoup.classes=com.funnelback.CAFilters.CheckContent`
* `check-content.cfg` containing the rules for content checking.  Included file contains some sample rules.
* `collection.cfg` entries (depending on features used):
	* `filter.check-content.word-list.WORD_LIST_NAME=/path-to/file-containing-word-list.cfg`

### Check for element existence

Test for the existence of an element
         
Parameters in configuration:

* `name`: A name to identify the check
* `description`: A short description of the check
* `selector`: the jsoup selector to check the existence of
* `metaField`: Adds the metadata field with a value of true if the check was found, and `<metaField>-count` with a count of how many times the match was found
* `extractField`: either `CONTENT`, or `ATTRIBUTE:<attribute name>` where `<attribute name>` is the attribute to extract.
* `extractValue`: extract the value of the `extractField` and write to `<metaField>-value` if set to true
* `extractMode`: extract the contents as TEXT (will strip out all tags) or HTML.  Acceptable values are TEXT or HTML.
* `document`: Jsoup object representing the document

#### Example configuration

Check for the presence of link tags with a property of rel=canonical.

If found sets the metadata field:

```
X-FUNNELBACK-CANONICAL=true
X-FUNNELBACK-CANONICAL-COUNT=<N> where <N> is the number of times the element was detected within the page
X-FUNNELBACK-CANONICAL-VALUE=<V> where <V> is the extracted value if extractValue is set to true.
```

Example JSON entry:

```json
{
    "name":"Canonical URL defined",
    "check":"ELEMENT_EXISTENCE",
    "metaField":"X-FUNNELBACK-CANONICAL",
    "selector":"link[rel=canonical]",
    "extractField":"CONTENT",
    "extractValue":true,
    "description":"Detects the presence of a canonical URL"
}
```

### Check for element content length

Test the length in characters or words of an element's content. Example - identify pages with any H1 elements longer than 200 characters.

Parameters in configuration:

* `name`: A name to identify the check
* `description`: A short description of the check
* `selector`: the jsoup selector to check the existence of
* `metaField`: Adds the metadata field with a value of true if the check was found, and `<metaField>-count` with a count of how many times the match was found
* `compareField`: either `CONTENT`, or `ATTRIBUTE:<attribute name>` where `<attribute name>` is the attribute to compare against and extract.
* `comparator`: compare function to use. Acceptable values are (where `N` is set using the length parameter):
	* `LENGTH_EQ_CHARS`: length of the selected value is equal to `N` characters
	* `LENGTH_GT_CHARS`: length of the selected value is greater than `N` characters
	* `LENGTH_LT_CHARS`: length of the selected value is less than `N` characters
	* `LENGTH_EQ_WORDS`: length of the selected value is equal to `N` words
	* `LENGTH_GT_WORDS`: length of the selected value is greated than `N` words
	* `LENGTH_LT_WORDS`: length of the selected value is less than `N` words)
* `extractValue`: extract the value of the `compareField` and write to `<metaField>-value`, and write the size (in words or chars based on the comparator) to `<metaField>-size` if set to true
* `length`: length value used for the comparison

#### Example configuration

Check for the presence of any titles with more than 200 characters

If found sets the metadata field:

```
X-FUNNELBACK-H1-GT-200=true
X-FUNNELBACK-H1-GT-200-COUNT=<N> where <N> is the number of times the element was detected within the page
X-FUNNELBACK-H1-GT-200-SIZE=<N> where <N> is the number of words or chars of the matched field if extractValue is set to true
X-FUNNELBACK-H1-GT-200-VALUE=<V> where <V> is the extracted value if extractValue is set to true.
```

Example JSON entry:

```json
{
    "name":"Heading 1 length",
    "check":"ELEMENT_LENGTH",
    "metaField":"X-FUNNELBACK-H1-GT-200",
    "selector":"title",
    "description":"Identifies if the document contains any H1 values that exceed 200 characters in length.",
    "comparator":"LENGTH_GT_CHARS",
    "length":"200",
    "extractValue":false
}
```

### Check for element content value

Tests an element's content
         
Parameters in configuration:

* `name`: A name to identify the check
* `description`: A short description of the check
* `selector`: the jsoup selector to check the existence of
* `metaField`: Adds the metadata field with a value of true if the check was found, and `<metaField>-count` with a count of how many times the match was found
* `comparator`: compare function to use. Acceptable values are (where `<comparison text>` is set using the compareText parameter):
            - `ENDS_WITH`: compareField value ends with `<comparison text>`
            - `STARTS_WITH`: compareField value starts with `<comparison text>`
            - `NOT_ENDS_WITH`: compareField value does not end with `<comparison text>`
            - `NOT_STARTS_WITH`: compareField value does not start with `<comparison text>`
            - `EQUALS`: compareField value equals `<comparison text>`
            - `NOT_EQUALS`: compareField value does not equal `<comparison text>`
            - `CONTAINS`: compareField value contains `<comparison text>`
            - `NOT_CONTAINS`: compareField value does not contain `<comparison text>`
            - `MATCHES`: compareField value includes text that matches the regular expression defined in `<comparison text>`
            - `NOT_MATCHES`: compareField value does not include text that matches the regular expression defined in `<comparison text>`
            - `FULLY_MATCHES`: compareField value fully matches the regular expression defined in `<comparison text>`
            - `NOT_FULLY_MATCHES`: compareField value does not fully match the regular expression defined in `<comparison text>`
* `compareField`: either `CONTENT`, or `ATTRIBUTE:<attribute name>` where `<attribute name>` is the attribute to compare against against and extract.
* `compareText`: value used for the comparison
* `extractValue`: extract the value of the selector and write to `<metaField>-value` if set to true

#### Example configuration

Check for the presence of any titles with more than 200 characters

If found sets the metadata field:

```
X-FUNNELBACK-LINK-CLICK-HERE=true
X-FUNNELBACK-LINK-CLICK-HERE-COUNT=<N> where <N> is the number of times the element was detected within the page
X-FUNNELBACK-LINK-CLICK-HERE-VALUE=<V> where <V> is the extracted value if extractValue is set to true.
```

Example JSON entry:

```json
{
    "name":"Links containing click here",
    "check":"ELEMENT_CONTENT",
    "metaField":"X-FUNNELBACK-LINK-CLICK-HERE",
    "selector":"a",
    "description":"Identifies if the document contains any links containing the phrase click here.",
    "comparator":"CONTAINS",
    "compareText":"click here",
    "extractValue":false
}
```

### Check element for words in a list

Check an element's content to see if it includes any words from a specified words list

Parameters in configuration:

Params:

* `name`: A name to identify the check
* `description`: A short description of the check
* `metaField`: Adds the metadata field with a value of true if the check was found, and `<metaField>-count` with a count of how many times the match was found
* `selector`: the jsoup selector to analyse. 
* `wordList`: The word list to check against (word list must be loaded via the `filter.check-content.word-list-X` `collection.cfg` parameter)

Code is based on the undesirable text core filter code, extended to support multiple word lists.

#### Example configuration

Check for the presence of any titles with more than 200 characters

If found sets the metadata field:

```
X-FUNNELBACK-WEASEL-WORDS=<weasel words found>.  This field will be set for each word that is found.
X-FUNNELBACK-WEASEL-WORDS_COUNT=<N> where <N> is the number of times a word was detected within the page
```

Requires a configuration entry:

```ini
filter.check-content.word-list.weasel-words=$SEARCH_HOME/conf/$COLLECTION_NAME/weasel-words.cfg
```

Example JSON entry:

```json
{
    "name":"Weasel words",
    "check":"WORD_LIST_COMPARE",
    "metaField":"X-FUNNELBACK-WEASEL-WORDS",
    "selector":"body",
    "description":"Identifies weasel words present in the document as defined in the weasel words list.",
    "wordList":"weasel-words"
}
```

### Validate content against a pattern

Validate an element's content using a library pattern

Parameters in configuration:

* `name`: A name to identify the check
* `description`: A short description of the check
* `selector`: the jsoup selector to validate
* `metaField`: Adds the metadata field with a value of true if the check was found, and `<metaField>-count` with a count of how many times the match was found
* `comparator`: compare function to use. Acceptable values are (where `<comparison text>` is set using the compareText parameter):
            - `MATCHES`: compareField value includes text that matches the regular expression defined in `<comparison text>`
            - `NOT_MATCHES`: compareField value does not include text that matches the regular expression defined in `<comparison text>`
            - `FULLY_MATCHES`: compareField value fully matches the regular expression defined in `<comparison text>`
            - `NOT_FULLY_MATCHES`: compareField value does not fully match the regular expression defined in `<comparison text>`
* `matchPattern`: Regex matcher as defined in the validation patterns. Metadata field is set to true if the selector matches the pattern. Acceptable values are:
	* `NUMBER_AS_TEXT`: Identifies numbers from 1-10 written out text
	* `PHONE_NUMBER_AU`: Identifies Australian telephone numbers
	* `URL`: Identifies valid URLs
	* `EMAIL_ADDRESS`: Identifies valid email addresses

#### Example configuration

Check the `og:url` field for an invalid url.

If found sets the metadata field:

```
X-FUNNELBACK-VALIDOGURL=true
X-FUNNELBACK-VALIDOGURL-count=<N> where <N> is the number of times a word was detected within the page
```

Example JSON entry:

Check for valid URLs in og:url metadata fields.

```json
{
    "name":"Validate OG URL",
    "check":"ELEMENT_VALIDATE",
    "metaField":"X-FUNNELBACK-VALIDOGURL",
    "selector":"meta[property=og:url]",
    "description":"Identifies OG URL fields that do not contain a valid URL",
    "comparator":"NOT_FULLY_MATCHES",
    "compareField":"ATTRIBUTE:content",
    "matchPattern":"URL",
    "extractValue":false
}
```

### check-content.cfg example

Note the order of items within each JSON record are not important.

```json
[
  {
    "name":"Canonical URL defined",
    "check":"ELEMENT_EXISTENCE",
    "metaField":"X-DTA-CANONICAL",
    "selector":"link[rel=canonical]",
    "description":"Detects the presence of a canonical URL"
  },
  {
    "name":"Links containing click here",
    "check":"ELEMENT_CONTENT",
    "metaField":"X-FUNNELBACK-LINK-CLICK-HERE",
    "selector":"a",
    "description":"Identifies if the document contains any links containing the phrase click here.",
    "comparator":"CONTAINS",
    "compareText":"click here",
    "extractValue":false
  },
  {
    "name":"Weasel words",
    "check":"WORD_LIST_COMPARE",
    "metaField":"X-FUNNELBACK-WEASEL-WORDS",
    "selector":"body",
    "description":"Identifies weasel words present in the document as defined in the weasel words list.",
    "wordList":"weasel-words"
  },
  {
    "name":"Validate OG URL",
    "check":"ELEMENT_VALIDATE",
    "metaField":"X-FUNNELBACK-VALIDOGURL",
    "selector":"meta[property=og:url]",
    "description":"Identifies OG URL fields that do not contain a valid URL",
    "comparator":"NOT_FULLY_MATCHES",
    "compareField":"ATTRIBUTE:content",
    "matchPattern":"URL",
    "extractValue":false
  }
]
```

### check-content.validation-patterns.cfg example

`check-content.validation-patterns.cfg` contains the pre-defined regular expressions that can be used in conjunction with the `ELEMENT_VALIDATE` check.

The format of the file is a key identifying the pattern, and the regular expression to use for the match.

Regular expressions must follow java escaping rules (so backslashes must be escaped).

```json
{
  "NUMBER_AS_TEXT":"\\b(one|two|three|four|five|six|seven|eight|nine|ten)\\b",
  "PHONE_NUMBER_AU":"(\\b1[38]00\\s+(?:\\D|\\d\\D|\\d\\d\\D))|(\\b1[38]00\\d)",
  "URL":"(https?|ftp|file):\/\/[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
  "EMAIL_ADDRESS":"[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}",
}
```


