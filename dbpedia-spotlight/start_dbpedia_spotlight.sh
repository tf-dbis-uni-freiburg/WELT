#!/bin/bash
java -jar ../executables/dbpedia-spotlight-0.7.1.jar ../models/dbpedia_spotlight_en http://localhost:2222/rest > logs/dbpedia_spotlight.out &
