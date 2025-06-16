artifact_name       := ocr-api
version             := "unversioned"

OS := $(shell uname)

dependency_check_runner := 416670754337.dkr.ecr.eu-west-2.amazonaws.com/dependency-check-runner

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

.PHONY: test-integration
test-integration: clean
	mvn test -Dincluded.tests="integration-test"

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

.PHONY: dependency-check
dependency-check:
	docker run --rm -e DEPENDENCY_CHECK_SUPPRESSIONS_HOME=/opt -v "$$(pwd)":/app -w /app ${dependency_check_runner} --repo-name="$(basename "$$(pwd)")"

.PHONY: security-check
security-check: dependency-check

