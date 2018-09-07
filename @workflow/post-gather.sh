#!/bin/bash
# Pre gather script

# Pass the variables in as -c $COLLECTION_NAME -g $GROOVY_HOME
while getopts ":c:g:" opt; do
  case $opt in
    c) COLLECTION_NAME="$OPTARG"
    ;;
    g) GROOVY_HOME="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG; USAGE: $0 -c COLLECTION_NAME -g GROOVY_HOME" >&2
    ;;
  esac
done


# Process the URL errors log and write out the error JSON and summary
${GROOVY_HOME} -cp "${SEARCH_HOME}/lib/java/all/*" ${SEARCH_HOME}/conf/${COLLECTION_NAME}/@workflow/process-errors.groovy ${SEARCH_HOME} ${COLLECTION_NAME}

