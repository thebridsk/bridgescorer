#!/bin/bash

SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

helpme()
{
  cat <<HELPMEHELPME
Syntax: $0 cmd
Syntax: $0 [-h|--help]

Runs cmd writing a message every 5 minutes to stdout.

HELPMEHELPME
}

if [[ "$1" == "-h" || "$1" == "--help" ]] ; then
  helpme
  exit 1
fi

while sleep 5m; do echo "====== $SECONDS seconds, building ======"; done &

$*
rc=$?

kill %1

exit $rc
