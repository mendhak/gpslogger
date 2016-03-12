#!/bin/bash

git reset --hard HEAD
git clean -fdx
rm -rf owncloudstorage/additional_apps
rm -rf owncloudstorage/data
rm -rf ftpstorage
