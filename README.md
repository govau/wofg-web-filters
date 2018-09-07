# wofg-web-filters
Filters for processing Web ARChive (WARC) files as part of the WofG Web Reporting Service

## Overview
These filters were originally developed with [Funnelback](https://www.funnelback.com) for use in both in-crawl and post-crawl filtering of data gathered during a [Whole-of-Australian Government web crawl](https://data.gov.au/dataset/whole-of-australian-government-web-crawl).

Pre-gather [workflow](Workflow-scripts.md) tasks are run in order to generate mappings for domains to portfolios (drawn from the Australian Government Organisation Register) and augment with other external data sources.

Post-gather, several [content checks](check-content.md) are run. These are written in Groovy, and are run with Funnelback's filter framework. Tools for splitting WARC files are also included at this stage.

Post-filtering, metadata is [written to JSON](WriteJSON.md) for injecting into ElasticSearch.
