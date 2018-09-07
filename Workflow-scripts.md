## Workflow scripts

Various workflow scripts for use with Funnelback.

* `post-crawl-filter.sh`: Wrapper script to apply Funnelback's series of filters to an unprocessed warc file (requires Funnelback).
* `post-gather.sh`: Funnelback post-gather script, run on completion of the web crawl.
* `pre-gather.sh`: Funnelback pre-gather script, run before commencement of the web crawl.
* `process-agor.groovy`: Script to process the AGOR CSV file and produce domain to portfolio mappings as well as a seed list and site profiles configuration used by the Funnelback crawler.
* `process-errors.groovy`: Script to analyse the url errors encountered during Funnelback's web crawl and produce some base level summary reporting. 
* `split-warc.sh`: Script to split the warc file produced by the Funnelback crawl into manageable chunks (requires Funnelback's built in warc tools).

