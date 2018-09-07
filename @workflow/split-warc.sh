#!/bin/bash
# Split source WARC file into n record chunks

# Pass the variables in as -c $COLLECTION_NAME etc
while getopts ":c:n:s:v:w:" opt; do
  case $opt in
    c) COLLECTION_NAME="$OPTARG"
    ;;
    n) NUM_DOCS="$OPTARG"
    ;;
    s) SLICE_SIZE="$OPTARG"
    ;;
    v) CURRENT_VIEW="$OPTARG"
    ;;
    w) WARC_FILE_STEM=$"$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG; USAGE: $0 -c COLLECTION_NAME -g GROOVY_HOME" >&2
    ;;
  esac
done

for ((i=1;i<=${NUM_DOCS};i+=${SLICE_SIZE}))
do
    echo "Writing records ${i}-$((i+SLICE_SIZE-1)) to ${WARC_FILE_STEM}-$((i / SLICE_SIZE + 1)).warc"
    echo "${SEARCH_HOME}/linbin/java/bin/java -classpath "${SEARCH_HOME}/lib/java/all/*:target/funnelback-warc-library.jar" com.funnelback.warc.util.WarcCat -stem ${SEARCH_HOME}/data/${COLLECTION_NAME}/${CURRENT_VIEW}/data/${WARC_FILE_STEM} -matcher Bounded -MF first=${i} -MF last=$((i+SLICE_SIZE-1)) -printer AllCompressed > ${SEARCH_HOME}/data/${COLLECTION_NAME}/${CURRENT_VIEW}/data/${WARC_FILE_STEM}-$((i / SLICE_SIZE + 1)).warc"

    ${SEARCH_HOME}/linbin/java/bin/java -classpath "${SEARCH_HOME}/lib/java/all/*:target/funnelback-warc-library.jar" com.funnelback.warc.util.WarcCat -stem ${SEARCH_HOME}/data/${COLLECTION_NAME}/${CURRENT_VIEW}/data/${WARC_FILE_STEM} -matcher Bounded -MF first=${i} -MF last=$((i+SLICE_SIZE-1)) -printer AllCompressed > ${SEARCH_HOME}/data/${COLLECTION_NAME}/${CURRENT_VIEW}/data/${WARC_FILE_STEM}-$((i / SLICE_SIZE + 1)).warc

done
