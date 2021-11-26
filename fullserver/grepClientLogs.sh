#!/bin/bash

SCRIPTDIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

helpme()
{
  cat <<HELPMEHELPME
Syntax:
  $0 logfile
  $0 [-h|--help]

Creates client<n>.log for all the client logs in the current directory.

HELPMEHELPME
}

if [[ "$1" == "-h" || "$1" == "--help" || "$1" == "" ]] ; then
  helpme
  exit 1
fi


egrep "ClientLog\([^,]+,1\)" $1 >client1.log
egrep "ClientLog\([^,]+,2\)" $1 >client2.log
egrep "ClientLog\([^,]+,3\)" $1 >client3.log
egrep "ClientLog\([^,]+,4\)" $1 >client4.log
