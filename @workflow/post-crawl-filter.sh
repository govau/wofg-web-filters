#!/bin/bash
# Post crawl filter

# Run filters across a collection's warc file post-crawl and write the output to another file.
# Pass the variables in as -c $COLLECTION_NAME -f $FILTER_CHAIN -v $CURRENT_VIEW
while getopts ":c:f:j:v:w:" opt; do
  case $opt in
    c) COLLECTION_NAME="$OPTARG"
    ;;
    f) FILTER_CHAIN="$OPTARG"
    ;;
    v) CURRENT_VIEW="$OPTARG"
    ;;
    j) JAVA_OPTS="$OPTARG"
    ;;
    w) WARC_FILE_STEM="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG; USAGE: $0 -c COLLECTION_NAME -f FILTER_CHAIN -v CURRENT_VIEW -w WARC_FILE_STEM" >&2
    ;;
  esac
done

# Delete previously filtered warc file

if [ -e ${SEARCH_HOME}/data/${COLLECTION_NAME}/${CURRENT_VIEW}/data/${WARC_FILE_STEM}_filtered.warc ]
then
  echo "Removing previous filtered warc file ${SEARCH_HOME}/data/${COLLECTION_NAME}/${CURRENT_VIEW}/data/${WARC_FILE_STEM}_filtered.warc"
  rm -f ${SEARCH_HOME}/data/${COLLECTION_NAME}/${CURRENT_VIEW}/data/${WARC_FILE_STEM}_filtered.warc
fi

echo "Filtering ${SEARCH_HOME}/data/${COLLECTION_NAME}/${CURRENT_VIEW}/data/${WARC_FILE_STEM}.warc with $FILTER_CHAIN"

${SEARCH_HOME}/linbin/java/bin/java ${JAVA_OPTS} -classpath "${SEARCH_HOME}/lib/java/all/*:${SEARCH_HOME}/conf/${COLLECTION_NAME}/@groovy/*" com.funnelback.common.filter.util.FilterWarcFile -collection ${COLLECTION_NAME} -filter ${FILTER_CHAIN} -in ${SEARCH_HOME}/data/${COLLECTION_NAME}/${CURRENT_VIEW}/data/${WARC_FILE_STEM} -out ${SEARCH_HOME}/data/${COLLECTION_NAME}/${CURRENT_VIEW}/data/${WARC_FILE_STEM}_filtered -v
