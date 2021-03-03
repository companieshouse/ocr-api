artifact_name       := ocr-api
version := "unversioned"
OS := $(shell uname)

.PHONY: all
all: build

.PHONY: clean
clean:
	mvn clean
	rm -f ./$(artifact_name).jar
	rm -f ./$(artifact_name)-*.zip
	rm -rf ./build-*
	rm -f ./build.log

.PHONY: build
build:
	mvn versions:set -DnewVersion=$(version) -DgenerateBackupPoms=false
	mvn package -DskipTests=true
	cp ./target/$(artifact_name)-$(version).jar ./$(artifact_name).jar

.PHONY: test
test: clean
	mvn verify

.PHONY: test-unit
test-unit: clean
	mvn test -Dincluded.tests="unit-test"

.PHONY: dev
dev: clean
	mvn package
	cp target/$(artifact_name)-unversioned.jar $(artifact_name).jar

.PHONY: package
package:
ifndef version
	$(error No version given. Aborting)
endif
	$(info Packaging version: $(version) on $(OS))
ifneq ($(OS),Darwin)
		mvn versions:set -DnewVersion=$(version) -DgenerateBackupPoms=false
endif
	mvn package -DskipTests=true
	$(eval tmpdir:=$(shell mktemp -d build-XXXXXXXXXX))
	cp ./target/$(artifact_name)-$(version).jar $(tmpdir)/$(artifact_name).jar
ifeq ($(OS),Darwin)
	cp ./target/$(artifact_name)-$(version).jar ./$(artifact_name).jar
endif
	cd $(tmpdir); zip -r ../$(artifact_name)-$(version).zip *
	rm -rf $(tmpdir)

.PHONY: dist
dist: clean build package


.PHONY: sonar
sonar:
	mvn sonar:sonar

.PHONY: sonar-pr-analysis
sonar-pr-analysis:
	mvn sonar:sonar	-P sonar-pr-analysis
