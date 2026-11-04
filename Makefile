# DayTrader 3 legacy baseline and standalone Spring Boot migration scaffold.
# Usage: make up && make build && make run && make parity TARGET=http://localhost:9083/daytrader

LIBERTY_IMAGE ?= icr.io/appcafe/websphere-liberty:full-java8-ibmjava-ubi
CONTAINER     ?= daytrader3-liberty
PORT          ?= 9083
BASE_URL      ?= http://localhost:$(PORT)/daytrader
TARGET        ?= $(BASE_URL)
SEED          ?= 42
VENV          ?= .venv
PY            := $(VENV)/bin/python

# The POMs target Java 7 (net.wasdev.maven.parent:java7-parent). Build with JDK 8 when it is installed locally.
JDK8_HOME     ?= $(firstword $(wildcard /usr/lib/jvm/java-8-openjdk-amd64 /usr/lib/jvm/temurin-8-jdk-amd64))
MVN           := $(if $(JDK8_HOME),JAVA_HOME=$(JDK8_HOME) )mvn -B -DskipFVT=true
BOOT_JDK      ?= $(firstword $(wildcard /usr/lib/jvm/java-21-openjdk-amd64 /usr/lib/jvm/temurin-21-jdk-amd64))
BOOT_MVN      ?= $(if $(wildcard /home/ubuntu/tools/apache-maven-3.9.16/bin/mvn),/home/ubuntu/tools/apache-maven-3.9.16/bin/mvn,mvn)
MVN_SETTINGS  ?=
BOOT_MVN_SETTINGS = $(if $(strip $(MVN_SETTINGS)),-s "$(MVN_SETTINGS)")
MAVEN_MIRROR_URL ?=
BOOT_DOCKER_MIRROR_ARG = $(if $(strip $(MAVEN_MIRROR_URL)),--build-arg MAVEN_MIRROR_URL=$(MAVEN_MIRROR_URL))
BOOT_BASE_URL ?= http://localhost:8080/daytrader

EAR           := $(CURDIR)/daytrader3-ee6/target/daytrader3-ee6-1.0-SNAPSHOT.ear
SERVER_XML    := $(CURDIR)/daytrader3-ee6-wlpcfg/servers/daytrader3_Sample/server.xml
DERBY_DIR     := $(CURDIR)/daytrader3-ee6-wlpcfg/shared/resources/Daytrader3_SampleDerbyLibs

.PHONY: up build test run seed record parity inventory down logs boot-build boot-run boot-stop boot-image boot-container

up: ## Pull the Liberty image and install the parity harness's Python deps
	docker pull $(LIBERTY_IMAGE)
	test -x $(PY) || python3 -m venv $(VENV)
	$(PY) -m pip install -q -r parity/requirements.txt

build: ## Build the EAR (runs the JUnit 5 unit tests too)
	$(MVN) clean package

test: ## Unit tests only
	$(MVN) test -pl daytrader3-ee6-ejb

run: ## Start Liberty with the repo's server.xml, then create and populate the Derby DB
	-docker rm -f $(CONTAINER) >/dev/null 2>&1
	docker run -d --name $(CONTAINER) -p $(PORT):9083 \
	  -v $(SERVER_XML):/config/server.xml:ro \
	  -v $(EAR):/config/apps/daytrader3-ee6.ear:ro \
	  -v $(DERBY_DIR):/opt/ibm/wlp/usr/shared/resources/Daytrader3_SampleDerbyLibs:ro \
	  $(LIBERTY_IMAGE)
	$(PY) parity/seed.py --target $(BASE_URL)

seed: ## Re-create and re-populate the DB on a running server
	$(PY) parity/seed.py --target $(TARGET) --skip-wait

record: ## Re-record the reference transcripts from the legacy app (only from the Liberty baseline)
	$(PY) parity/record.py --target $(BASE_URL) --seed $(SEED)

parity: ## Replay all scenarios against TARGET and diff against parity/transcripts (non-zero exit on any diff)
	$(PY) parity/compare.py --target $(TARGET)

inventory: ## Regenerate docs/WEBSPHERE_INVENTORY.md
	python3 tools/ws_inventory.py

logs:
	docker logs --tail 200 $(CONTAINER)

down:
	-docker rm -f $(CONTAINER)

boot-build: ## Verify the standalone Spring Boot application with JDK 21
	JAVA_HOME=$(BOOT_JDK) PATH="$(BOOT_JDK)/bin:$$PATH" $(BOOT_MVN) $(BOOT_MVN_SETTINGS) -B -f daytrader-boot/pom.xml verify

boot-run: boot-build ## Start the Spring Boot app on :8080 and seed its H2 database
	mkdir -p work
	$(BOOT_JDK)/bin/java -Duser.timezone=UTC -jar daytrader-boot/target/daytrader-boot.jar > work/boot.log 2>&1 & echo $$! > work/boot.pid
	$(PY) parity/seed.py --target $(BOOT_BASE_URL)

boot-stop: ## Stop the background Spring Boot app started by boot-run
	if test -f work/boot.pid; then kill "$$(cat work/boot.pid)" && rm -f work/boot.pid; fi

boot-image: ## Build the standalone Spring Boot Docker image
	docker build $(BOOT_DOCKER_MIRROR_ARG) -t daytrader-boot:local -f daytrader-boot/Dockerfile daytrader-boot

boot-container: boot-image ## Replace the jar app with the container and seed H2
	$(MAKE) boot-stop
	-docker rm -f daytrader-boot
	docker run -d --name daytrader-boot -p 8080:8080 daytrader-boot:local
	$(PY) parity/seed.py --target http://localhost:8080/daytrader
