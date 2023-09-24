#!/bin/bash

cat build.gradle.kts | sed 's/^version="[^"]*"/version=\"'$1'\"/' > build.gradle.kts.tmp && mv build.gradle.kts.tmp build.gradle.kts

