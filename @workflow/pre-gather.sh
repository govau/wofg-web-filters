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



# Download and process AGOR
${GROOVY_HOME} -cp "${SEARCH_HOME}/lib/java/all/*" ${SEARCH_HOME}/conf/${COLLECTION_NAME}/@workflow/process-agor.groovy ${SEARCH_HOME} ${COLLECTION_NAME}
# /opt/funnelback/tools/groovy/bin/groovy -cp '/opt/funnelback/lib/java/all/*' /opt/funnelback/conf/dta-filtertest/@workflow/process-agor.groovy $SEARCH_HOME dta-filtertest > AGOR_Processed.txt

# Download and process SSL data
#${GROOVY_HOME} -cp "${SEARCH_HOME}/lib/java/all/*" ${SEARCH_HOME}/conf/${COLLECTION_NAME}/@workflow/process-certificate-transparency.groovy ${SEARCH_HOME} ${COLLECTION_NAME}

# Back up configuration files
if [ -f ${SEARCH_HOME}/conf/${COLLECTION_NAME}/site_profiles.cfg ]; then
  mv ${SEARCH_HOME}/conf/${COLLECTION_NAME}/site_profiles.cfg ${SEARCH_HOME}/conf/${COLLECTION_NAME}/site_profiles.cfg.$(date +%s)
fi
mv ${SEARCH_HOME}/conf/${COLLECTION_NAME}/site_profiles.cfg.generated ${SEARCH_HOME}/conf/${COLLECTION_NAME}/site_profiles.cfg

mv ${SEARCH_HOME}/conf/${COLLECTION_NAME}/collection.cfg ${SEARCH_HOME}/conf/${COLLECTION_NAME}/collection.cfg.$(date +%s)
mv ${SEARCH_HOME}/conf/${COLLECTION_NAME}/collection.cfg.generated ${SEARCH_HOME}/conf/${COLLECTION_NAME}/collection.cfg

mv ${SEARCH_HOME}/conf/${COLLECTION_NAME}/collection.cfg.start.urls ${SEARCH_HOME}/conf/${COLLECTION_NAME}/collection.cfg.start.urls.$(date +%s)
mv ${SEARCH_HOME}/conf/${COLLECTION_NAME}/collection.cfg.start.urls.generated ${SEARCH_HOME}/conf/${COLLECTION_NAME}/collection.cfg.start.urls
